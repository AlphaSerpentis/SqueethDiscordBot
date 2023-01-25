// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.data.api.alchemy.SimpleTokenTransferResponse;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.LaevitasHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.PositionsDataHandler;

import java.awt.*;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Squeeth.*;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Uniswap.oracle;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.CommonFunctions.*;

public class Position extends ButtonCommand<MessageEmbed> {

    private static final HashMap<Long, AbstractPositions[]> cachedPositions = new HashMap<>();
    private static final HashMap<String, String> cachedENSDomains = new HashMap<>();

    public abstract static class AbstractPositions {

        public ArrayList<SimpleTokenTransferResponse> transfers = new ArrayList<>();
        public final HashMap<Long, BigInteger> tokensAtBlock = new HashMap<>();
        public final String userAddress;
        public BigInteger costBasis = BigInteger.ZERO;
        public BigInteger costBasisInEth = BigInteger.ZERO;
        public BigInteger currentAmtHeld;
        public BigInteger currentPriceInEth;
        public BigInteger currentPriceInUsd;
        public BigInteger currentValueInEth;
        public BigInteger currentValueInUsd;

        public AbstractPositions(@NonNull String userAddress) {
            this.userAddress = userAddress;
        }

        /**
         * Obtains the transfers of the given token for userAddress
         */
        public void getAndSetTransfers(@NonNull String tokenAddress) {
            // Check caches to see if we have the data
            if(PositionsDataHandler.cachedTransfers.containsKey(userAddress)) { // cache does contain address
                if(PositionsDataHandler.cachedTransfers.get(userAddress).stream().noneMatch(t -> t.token.equalsIgnoreCase(tokenAddress))) { // cache doesn't have the specific token we need
                    transfers = EthereumRPCHandler.getAssetTransfersOfUser(userAddress, tokenAddress);
                } else {
                    transfers = (ArrayList<SimpleTokenTransferResponse>) PositionsDataHandler.cachedTransfers.get(userAddress).stream().filter(t -> t.token.equalsIgnoreCase(tokenAddress)).collect(Collectors.toList());
                    long highestBlock = transfers.get(transfers.size() - 1).blockNum;
                    ArrayList<SimpleTokenTransferResponse> newTransfers = EthereumRPCHandler.getAssetTransfersOfUser(userAddress, tokenAddress, highestBlock + 1, -1);
                    transfers.addAll(newTransfers);
                }
            } else {
                transfers = EthereumRPCHandler.getAssetTransfersOfUser(userAddress, tokenAddress);
            }

            for(SimpleTokenTransferResponse transfer : transfers) {
                if(transfer.from.equalsIgnoreCase(userAddress)) { // leaves the account
                    if(tokensAtBlock.size() == 0) continue;

                    long lowestBlock = tokensAtBlock.keySet().stream().min(Long::compareTo).get();
                    BigInteger val = tokensAtBlock.get(lowestBlock).subtract(transfer.getBigIntegerValue());

                    while(val.compareTo(BigInteger.ZERO) < 0) { // if the value is negative, we need to remove the block and subtract the next lowest block's value and repeat until the lowest block is no longer negative
                        tokensAtBlock.remove(lowestBlock);
                        if(tokensAtBlock.size() == 0) break;

                        lowestBlock = tokensAtBlock.keySet().stream().min(Long::compareTo).get();
                        val = tokensAtBlock.get(lowestBlock).subtract(val.abs());

                        if(val.compareTo(BigInteger.ZERO) > 0) { // value is no longer negative, therefore it is registered in the mapping
                            tokensAtBlock.put(lowestBlock, val);
                        }
                    }

                    if(val.equals(BigInteger.ZERO)) {
                        tokensAtBlock.remove(lowestBlock);
                    } else if(val.compareTo(BigInteger.ZERO) > 0) {
                        tokensAtBlock.put(lowestBlock, val);
                    }
                } else { // enters the account
                    tokensAtBlock.put((long) transfer.getBlockNum(), transfer.getBigIntegerValue());
                }
            }

            if(tokensAtBlock.isEmpty()) { // if empty, indicates no transactions or ended up being empty as of current
                PositionsDataHandler.removeData(userAddress, tokenAddress);
            } else {
                PositionsDataHandler.addNewData(userAddress, transfers);
            }
        }
        public void calculatePnl() {
            currentAmtHeld = tokensAtBlock.values().stream().reduce(BigInteger.ZERO, BigInteger::add);
            calculateCostBasis();
            calculateCurrentValue();
        }
        public abstract void getAndSetPrices();
        public abstract void calculateCostBasis();
        public abstract void calculateCurrentValue();
        public boolean isValueDust(@NonNull BigInteger value) {
            return value.compareTo(BigInteger.TEN.pow(12)) < 0;
        }
    }

    public abstract static class ShortVol extends AbstractPositions {
        double currentVol;
        double averageVolEntry;
        double averageVega;

        public ShortVol(String userAddress) {
            super(userAddress);
        }
        @SuppressWarnings("unused")
        public abstract double calculateShortOsqthExposure(@NonNull BigInteger size, long block) throws ExecutionException, InterruptedException;
        static double calculateVega(double vol, double osqthUsd) {
            return 2 * vol * FUNDING_PERIOD * osqthUsd;
        }
    }

    public static class LongPositions extends AbstractPositions {
        public double estimatedFunding = 0;

        public LongPositions(@NonNull String userAddress) {
            super(userAddress);
        }

        @Override
        public void getAndSetPrices() {
            try {
                currentPriceInEth = (BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(oracle, getTwap_osqth).get(0).getValue();
                currentPriceInUsd = currentPriceInEth.multiply((BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(oracle, getTwap_ethUsd).get(0).getValue());
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void calculateCostBasis() {
            DecimalFormat df = new DecimalFormat("#");
            // Loop through all the blocks
            for(long block : tokensAtBlock.keySet().stream().sorted().toList()) {
                BigInteger priceOsqth, priceEth, transferCostBasis;
                double earliestNormFactor;
                try {
                    PriceData priceData = new PriceData();

                    if(PositionsDataHandler.cachedPrices.containsKey(block)) {
                        priceData = PositionsDataHandler.cachedPrices.get(block);
                        priceOsqth = priceData.osqthEth;
                        priceEth = priceData.ethUsdc;
                    } else {
                        PriceData tempPriceData = PositionsDataHandler.getPriceData(block, new PriceData.Prices[]{PriceData.Prices.OSQTHETH, PriceData.Prices.ETHUSD});
                        priceOsqth = tempPriceData.osqthEth;
                        priceEth = tempPriceData.ethUsdc;
                        priceData.ethUsdc = priceEth;
                        priceData.osqthEth = priceOsqth;
                    }

                    if(priceData.normFactor.equals(BigInteger.ZERO)) {
                        priceData.normFactor = PositionsDataHandler.getPriceData(block, new PriceData.Prices[]{PriceData.Prices.NORMFACTOR}).normFactor;
                    }

                    earliestNormFactor = priceData.normFactor.doubleValue() / Math.pow(10,18);

                    PositionsDataHandler.addNewData(block, priceData);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                costBasisInEth = costBasisInEth.add(priceOsqth.multiply(tokensAtBlock.get(block)).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,18))))));
                transferCostBasis = priceOsqth.multiply(priceEth).multiply(tokensAtBlock.get(block)).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,36)))));
                costBasis = costBasis.add(transferCostBasis);
                estimatedFunding = estimatedFunding + calculateEstimatedFunding(earliestNormFactor, transferCostBasis.doubleValue() / Math.pow(10,18));
            }
        }

        @Override
        public void calculateCurrentValue() {
            DecimalFormat df = new DecimalFormat("#");
            currentValueInEth = currentAmtHeld.multiply(currentPriceInEth).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,18)))));
            currentValueInUsd = currentAmtHeld.multiply(currentPriceInUsd).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,36)))));
        }

        /**
         * Calculates the estimated funding provided norm factors from start to end and USD size
         * @param normFactorStart norm factor at the start
         * @param size USD size of the position affected
         * @return estimated funding
         */
        public double calculateEstimatedFunding(double normFactorStart, double size) {
            double normFactorEnd;
            try {
                normFactorEnd = ((BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(
                    controller,
                    getExpectedNormFactor
                ).get(0).getValue()).doubleValue() / Math.pow(10,18);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            double normFactorDiff = normFactorEnd/normFactorStart - 1;
            return normFactorDiff * size * -1;
        }

        public double calculateEstimatedBreakeven() {
            double costBasisPerToken = costBasis.doubleValue() / currentAmtHeld.doubleValue();
            double osqthPrice = currentValueInUsd.doubleValue() / currentAmtHeld.doubleValue();
            double difference = costBasisPerToken - osqthPrice;
            double breakevenOsqthPrice = osqthPrice + difference;
            double impliedVol = LaevitasHandler.latestSqueethData.data.getCurrentImpliedVolatility() / 100;
            double normFactor = LaevitasHandler.latestSqueethData.data.getNormalizationFactor();
            final double fundingPeriod = 0.04794520548;
            final double scalingFactor = 10000;

            return Math.sqrt((breakevenOsqthPrice * scalingFactor)/(normFactor*Math.exp(Math.pow(impliedVol,2) * fundingPeriod)));
        }
    }

    public static class CrabPositions extends ShortVol {
        private final String crab;
        private final boolean isV2;

        public CrabPositions(@NonNull String userAddress, @NonNull String crabAddress, boolean isV2) {
            super(userAddress);
            crab = crabAddress;
            this.isV2 = isV2;
        }

        @Override
        public void getAndSetPrices() {
            try {
                PriceData tempPriceData;
                if(!isV2) {
                    tempPriceData = PositionsDataHandler.getPriceData(
                            EthereumRPCHandler.web3.ethBlockNumber().send().getBlockNumber().longValue(),
                            new PriceData.Prices[]{PriceData.Prices.CRABV1ETH, PriceData.Prices.ETHUSD, PriceData.Prices.SQUEETHVOL}
                    );
                    currentPriceInEth = tempPriceData.crabEth;
                } else {
                    tempPriceData = PositionsDataHandler.getPriceData(
                            EthereumRPCHandler.web3.ethBlockNumber().send().getBlockNumber().longValue(),
                            new PriceData.Prices[]{PriceData.Prices.CRABV2ETH, PriceData.Prices.ETHUSD, PriceData.Prices.SQUEETHVOL}
                    );
                    currentPriceInEth = tempPriceData.crabV2Eth;
                }

                currentPriceInUsd = currentPriceInEth.multiply(tempPriceData.ethUsdc);
                currentVol = tempPriceData.squeethVol;
            } catch (ExecutionException | InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void calculateCostBasis() {
            // Loop through all the blocks
            for(long block : tokensAtBlock.keySet().stream().sorted().toList()) {
                try {
                    PriceData priceData = new PriceData();
                    boolean fetchPriceAtThisBlock = false;

                    if(PositionsDataHandler.cachedPrices.containsKey(block)) {
                        priceData = PositionsDataHandler.cachedPrices.get(block);
                        if(!isV2) {
                            if(priceData.crabEth.equals(BigInteger.ZERO) || priceData.ethUsdc.equals(BigInteger.ZERO) || priceData.osqthEth.equals(BigInteger.ZERO) || priceData.squeethVol == 0) {
                                fetchPriceAtThisBlock = true;
                            }
                        } else {
                            if(priceData.crabV2Eth.equals(BigInteger.ZERO) || priceData.ethUsdc.equals(BigInteger.ZERO) || priceData.osqthEth.equals(BigInteger.ZERO) || priceData.squeethVol == 0) {
                                fetchPriceAtThisBlock = true;
                            }
                        }
                    } else {
                        fetchPriceAtThisBlock = true;
                    }

                    if(fetchPriceAtThisBlock) {
                        if(!isV2) {
                            priceData = PositionsDataHandler.getPriceData(
                                    block,
                                    new PriceData.Prices[]{PriceData.Prices.CRABV1ETH, PriceData.Prices.ETHUSD, PriceData.Prices.OSQTHETH, PriceData.Prices.SQUEETHVOL}
                            );
                        } else {
                            priceData = PositionsDataHandler.getPriceData(
                                    block,
                                    new PriceData.Prices[]{PriceData.Prices.CRABV2ETH, PriceData.Prices.ETHUSD, PriceData.Prices.OSQTHETH, PriceData.Prices.SQUEETHVOL}
                            );
                        }

                        PositionsDataHandler.addNewData(block, priceData);
                    }

                    if(isV2) {
                        costBasisInEth = costBasisInEth.add(priceData.crabV2Eth.multiply(tokensAtBlock.get(block)).divide(BigInteger.TEN.pow(18)));
                        costBasis = costBasis.add(priceData.crabV2Eth.multiply(priceData.ethUsdc).multiply(tokensAtBlock.get(block)).divide(BigInteger.TEN.pow(36)));
                    } else {
                        costBasisInEth = costBasisInEth.add(priceData.crabEth.multiply(tokensAtBlock.get(block)).divide(BigInteger.TEN.pow(18)));
                        costBasis = costBasis.add(priceData.crabEth.multiply(priceData.ethUsdc).multiply(tokensAtBlock.get(block)).divide(BigInteger.TEN.pow(36)));
                    }

                    averageVolEntry += priceData.squeethVol * (tokensAtBlock.get(block).doubleValue() / Math.pow(10,18) / (currentAmtHeld.doubleValue() / Math.pow(10,18)));
                    averageVega += calculateShortOsqthExposure(tokensAtBlock.get(block), block) * -calculateVega(priceData.squeethVol, (priceData.osqthEth.multiply(priceData.ethUsdc).divide(BigInteger.TEN.pow(18))).doubleValue() / Math.pow(10,18));
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void calculateCurrentValue() {
            currentValueInEth = currentAmtHeld.multiply(currentPriceInEth).divide(BigInteger.TEN.pow(18));
            currentValueInUsd = currentAmtHeld.multiply(currentPriceInUsd).divide(BigInteger.TEN.pow(36));
        }

        @Override
        public double calculateShortOsqthExposure(@NonNull BigInteger size, long block) throws ExecutionException, InterruptedException {
            BigInteger crabTotalSupply, crabShortOsqth;

            crabShortOsqth = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                    Addresses.Squeeth.crabv2,
                    getVaultDetails,
                    block
            ).get(3).getValue();
            crabTotalSupply = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                    Addresses.Squeeth.crabv2,
                    callTotalSupply,
                    block
            ).get(0).getValue();

            return (crabShortOsqth.doubleValue() / crabTotalSupply.doubleValue()) * (size.doubleValue() / Math.pow(10,18));
        }
    }

    public static class ZenBullPositions extends ShortVol {

        public ZenBullPositions(@NonNull String userAddress) {
            super(userAddress);
        }

        @Override
        public double calculateShortOsqthExposure(@NonNull BigInteger size, long block) throws ExecutionException, InterruptedException {
            BigInteger zenbullTotalSupply, crabBalance, crabTotalSupply, crabShortOsqth;

            crabShortOsqth = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                    Addresses.Squeeth.crabv2,
                    getVaultDetails,
                    block
            ).get(3).getValue();
            crabBalance = ((BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                    Addresses.Squeeth.crabv2,
                    balanceOf(
                            zenbull
                    ),
                    block
            ).get(0).getValue());
            zenbullTotalSupply = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                    zenbull,
                    callTotalSupply,
                    block
            ).get(0).getValue();
            crabTotalSupply = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                    Addresses.Squeeth.crabv2,
                    callTotalSupply,
                    block
            ).get(0).getValue();

            return (crabShortOsqth.doubleValue() / (crabTotalSupply.doubleValue() / crabBalance.doubleValue())) * (size.doubleValue() / zenbullTotalSupply.doubleValue()) / Math.pow(10,18);
        }

        @Override
        public void getAndSetPrices() {
            try {
                PriceData tempPriceData;
                tempPriceData = PositionsDataHandler.getPriceData(
                        EthereumRPCHandler.web3.ethBlockNumber().send().getBlockNumber().longValue(),
                        new PriceData.Prices[]{PriceData.Prices.ZENBULL, PriceData.Prices.ETHUSD, PriceData.Prices.SQUEETHVOL}
                );
                currentPriceInEth = tempPriceData.zenbull;
                currentPriceInUsd = currentPriceInEth.multiply(tempPriceData.ethUsdc);
                currentVol = tempPriceData.squeethVol;
            } catch (ExecutionException | InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void calculateCostBasis() {
            // Loop through all the blocks
            for(long block : tokensAtBlock.keySet().stream().sorted().toList()) {
                try {
                    PriceData priceData = new PriceData();
                    boolean fetchPriceAtThisBlock = false;

                    if(PositionsDataHandler.cachedPrices.containsKey(block)) {
                        priceData = PositionsDataHandler.cachedPrices.get(block);
                        if(priceData.zenbull.equals(BigInteger.ZERO) || priceData.ethUsdc.equals(BigInteger.ZERO) || priceData.squeethVol == 0) {
                            fetchPriceAtThisBlock = true;
                        }
                    } else {
                        fetchPriceAtThisBlock = true;
                    }

                    if(fetchPriceAtThisBlock) {
                        priceData = PositionsDataHandler.getPriceData(
                                block,
                                new PriceData.Prices[]{PriceData.Prices.ZENBULL, PriceData.Prices.ETHUSD, PriceData.Prices.OSQTHETH, PriceData.Prices.SQUEETHVOL}
                        );

                        PositionsDataHandler.addNewData(block, priceData);
                    }

                    costBasisInEth = costBasisInEth.add(priceData.zenbull.multiply(tokensAtBlock.get(block)).divide(BigInteger.TEN.pow(18)));
                    costBasis = costBasis.add(priceData.zenbull.multiply(priceData.ethUsdc).multiply(tokensAtBlock.get(block)).divide(BigInteger.TEN.pow(36)));
                    averageVolEntry += priceData.squeethVol * (tokensAtBlock.get(block).doubleValue() / Math.pow(10,18)) / (currentAmtHeld.doubleValue() / Math.pow(10,18));
                    averageVega += calculateShortOsqthExposure(tokensAtBlock.get(block), block) * -calculateVega(priceData.squeethVol, (priceData.osqthEth.multiply(priceData.ethUsdc).divide(BigInteger.TEN.pow(18))).doubleValue() / Math.pow(10,18));
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void calculateCurrentValue() {
            currentValueInEth = currentAmtHeld.multiply(currentPriceInEth).divide(BigInteger.TEN.pow(18));
            currentValueInUsd = currentAmtHeld.multiply(currentPriceInUsd).divide(BigInteger.TEN.pow(36));
        }
    }

    public Position() {
        super(new BotCommandOptions(
            "position",
            "Checks your wallet's long Squeeth, Crab v1/v2, and Zen Bull positions",
            30,
            0,
            true,
            true,
            TypeOfEphemeral.DEFAULT,
            true,
            true,
            true,
            false
        ));

        buttonHashMap.put("Previous", Button.primary("position_previous", "Previous").asDisabled());
        buttonHashMap.put("Page", Button.secondary("position_page", "1/4").asDisabled());
        buttonHashMap.put("Next", Button.primary("position_next", "Next"));
    }

    @NonNull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        if(isUserRatelimited(event.getUser().getIdLong())) {
            eb.setDescription("You are still rate limited. Expires in " + (ratelimitMap.get(event.getUser().getIdLong()) - Instant.now().getEpochSecond()) + " seconds.");
            return new CommandResponse<>(eb.build(), onlyEphemeral);
        }

        String userAddress = event.getOptions().get(0).getAsString();

        // Validate Ethereum address
        if(userAddress.length() != 42 && userAddress.startsWith("0x") && !userAddress.endsWith(".eth")) {
            eb.setDescription("Invalid Ethereum address (must be a proper Ethereum address with 0x prefix OR valid ENS name)");
            return new CommandResponse<>(eb.build(), onlyEphemeral);
        } else {
            try {
                userAddress = EthereumRPCHandler.getResolvedAddress(userAddress).toLowerCase();
            } catch (Exception e) {
                eb.setDescription("Invalid Ethereum address (must be a proper Ethereum address with 0x prefix OR valid ENS name)");
                return new CommandResponse<>(eb.build(), onlyEphemeral);
            }
        }

        AbstractPositions[] posArray = new AbstractPositions[]{
                new LongPositions(userAddress),
                new CrabPositions(userAddress, crabv1, false), // v1
                new CrabPositions(userAddress, crabv2, true), // v2
                new ZenBullPositions(userAddress)
        };

        posArray[0].getAndSetTransfers(osqth);
        posArray[1].getAndSetTransfers(((CrabPositions) posArray[1]).crab);
        posArray[2].getAndSetTransfers(((CrabPositions) posArray[2]).crab);
        posArray[3].getAndSetTransfers(zenbull);

        for(AbstractPositions pos: posArray) {
            pos.getAndSetPrices();
            pos.calculatePnl();
        }

        cachedPositions.put(
                event.getUser().getIdLong(),
                posArray
        );
        cachedENSDomains.putIfAbsent(userAddress, EthereumRPCHandler.getENSName(userAddress));

        eb.setTitle("Position Viewer for " + cachedENSDomains.get(userAddress));
        displayPositionPage(eb, 0, posArray);

        if(
                !(posArray[0].transfers.size() != 0 || !posArray[0].isValueDust(posArray[0].currentAmtHeld)) &&
                !(posArray[1].transfers.size() != 0 || !posArray[1].isValueDust(posArray[1].currentAmtHeld)) &&
                !(posArray[2].transfers.size() != 0 || !posArray[2].isValueDust(posArray[2].currentAmtHeld)) &&
                !(posArray[3].transfers.size() != 0 || !posArray[3].isValueDust(posArray[3].currentAmtHeld))
        ) {
            cachedPositions.remove(event.getUser().getIdLong());
        }

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.STRING, "address", "Your Ethereum address", true)
                .complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void runButtonInteraction(@NonNull ButtonInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        List<ItemComponent> buttons = new ArrayList<>();
        AbstractPositions[] posArray = cachedPositions.get(event.getUser().getIdLong());
        MessageEditCallbackAction pending = null;

        eb.setTitle("Position Viewer for " + cachedENSDomains.getOrDefault(posArray[0].userAddress, posArray[0].userAddress));
        int currentPage = Integer.parseInt(event.getMessage().getButtons().get(1).getLabel().substring(0,1));

        switch(event.getButton().getId()) {
            case "position_next" -> {
                displayPositionPage(eb, currentPage, posArray);
                pending = event.editMessageEmbeds(eb.build());
                buttons.add(Button.secondary("position_page", currentPage++ + 1 + "/4").asDisabled());
            }
            case "position_previous" -> {
                displayPositionPage(eb, currentPage - 2, posArray);
                pending = event.editMessageEmbeds(eb.build());
                buttons.add(Button.secondary("position_page", currentPage-- - 1 + "/4").asDisabled());
            }
        }

        if(currentPage == 4) {
            buttons.add(0, Button.primary("position_previous", "Previous").asEnabled());
            buttons.add(Button.primary("position_next", "Next").asDisabled());
        } else if(currentPage == 1) {
            buttons.add(0, Button.primary("position_previous", "Previous").asDisabled());
            buttons.add(Button.primary("position_next", "Next").asEnabled());
        } else {
            buttons.add(0, Button.primary("position_previous", "Previous").asEnabled());
            buttons.add(Button.primary("position_next", "Next").asEnabled());
        }

        pending.setActionRow(buttons).complete();
    }

    @Override
    @NonNull
    public Collection<ItemComponent> addButtons(@NonNull GenericCommandInteractionEvent event) {
        if(cachedPositions.get(event.getUser().getIdLong()) == null || isUserRatelimited(event.getUser().getIdLong())) {
            return Collections.emptyList();
        }

        return Arrays.asList(new ItemComponent[]{getButton("Previous"), getButton("Page"), getButton("Next")});
    }

    private void displayPositionPage(@NonNull EmbedBuilder eb, int page, @NonNull AbstractPositions[] posArray) {
        DecimalFormat df = new DecimalFormat("#");
        NumberFormat nf = NumberFormat.getInstance();

        String priceInUsd = nf.format(posArray[page].currentPriceInUsd.divide(new BigInteger(String.valueOf(df.format(Math.pow(10,18))))).doubleValue() / Math.pow(10,18));
        String positionHeld = nf.format(posArray[page].currentAmtHeld.doubleValue() / Math.pow(10,18));
        String costBasisInUsd = nf.format(posArray[page].costBasis.doubleValue() / Math.pow(10,18));
        String costBasisInEth = nf.format(posArray[page].costBasisInEth.doubleValue() / Math.pow(10,18));
        String positionValueInUsd = nf.format(posArray[page].currentValueInUsd.doubleValue() / Math.pow(10,18));
        String positionValueInEth = nf.format(posArray[page].currentValueInEth.doubleValue() / Math.pow(10,18));
        String unrealizedPnlInUsd = nf.format((posArray[page].currentValueInUsd.doubleValue() / Math.pow(10,18)) - (posArray[page].costBasis.doubleValue() / Math.pow(10,18)));
        String unrealizedPnlInEth = nf.format((posArray[page].currentValueInEth.doubleValue() / Math.pow(10,18)) - (posArray[page].costBasisInEth.doubleValue() / Math.pow(10,18)));
        String unrealizedPnlInUsdPercentage = nf.format(((posArray[page].currentValueInUsd.doubleValue() / Math.pow(10,18)) - (posArray[page].costBasis.doubleValue() / Math.pow(10,18))) / (posArray[page].costBasis.doubleValue() / Math.pow(10,18)) * 100);
        String unrealizedPnlInEthPercentage = nf.format(((posArray[page].currentValueInEth.doubleValue() / Math.pow(10,18)) - (posArray[page].costBasisInEth.doubleValue() / Math.pow(10,18))) / (posArray[page].costBasisInEth.doubleValue() / Math.pow(10,18)) * 100);
        String unrealizedPnlFromVega = null;

        MessageEmbed.Field volatilityField = null;

        if(page >= 1) {
            unrealizedPnlFromVega = nf.format(
                    ((ShortVol) posArray[page]).averageVega * (((ShortVol) posArray[page]).currentVol - ((ShortVol) posArray[page]).averageVolEntry)
            );
            volatilityField = new MessageEmbed.Field(
                    "Volatility (Yours) → (Current)",
                    nf.format(((ShortVol) posArray[page]).averageVolEntry * 100) + "% → " + nf.format(((ShortVol) posArray[page]).currentVol * 100) + "% " + (((ShortVol) posArray[page]).averageVolEntry >= ((ShortVol) posArray[page]).currentVol ? "\uD83D\uDE0A\n\nPremiums earned are lower over time, but exit conditions are ideal at the moment" : "\uD83D\uDE13\n\nPremiums earned are higher over time, but exit conditions may not be ideal at the moment"),
                    false
            );
        }

        switch(page) {
            case 0 -> { // long squeeth
                if(posArray[0].transfers.size() == 0 || posArray[0].isValueDust(posArray[0].currentAmtHeld)) {
                    eb.setDescription("No long position active (empty or dust)");
                } else {
                    eb.setThumbnail("https://c.tenor.com/URrZkAPGQjAAAAAC/cat-squish-cat.gif");
                    eb.setColor(Color.GREEN);
                    eb.addField("Long Position", "Holding " + positionHeld + " oSQTH", false);
                    eb.addField("Price of oSQTH", "$" + priceInUsd, false);
                    eb.addField("Cost Basis", "$" + costBasisInUsd + " (" + costBasisInEth + " Ξ)", false);
                    eb.addField("Position Value", "$" + positionValueInUsd + " (" + positionValueInEth + " Ξ)", false);
                    eb.addField("Unrealized PNL", "$" + unrealizedPnlInUsd + " (" + unrealizedPnlInUsdPercentage + "%)\n" + unrealizedPnlInEth + " Ξ (" + unrealizedPnlInEthPercentage + "%)", false);
                    eb.addField("Estimated Funding Paid", "$" + nf.format(((LongPositions) posArray[0]).estimatedFunding), false);
                    eb.addField("Estimated Breakeven", "$" + nf.format(((LongPositions) posArray[0]).calculateEstimatedBreakeven()), false);
                }
            }
            case 1 -> { // crab v1
                if(posArray[1].transfers.size() == 0 || posArray[1].isValueDust(posArray[1].currentAmtHeld)) {
                    eb.setDescription("No Crab v1 position active (empty or dust)");
                } else {
                    eb.setThumbnail("https://c.tenor.com/3CIbJomibvYAAAAi/crab-rave.gif");
                    eb.setColor(Color.RED);
//                    eb.addField("Crab Position", "Holding " + NumberFormat.getInstance().format(posArray[1].currentAmtHeld.doubleValue()/Math.pow(10,18)) + " Crab", false);
                    eb.addField("Price of Crab v1", "$" + priceInUsd, false);
                    eb.addField("Cost Basis", "$" + costBasisInUsd + " (" + costBasisInEth + " Ξ)", false);
                    eb.addField("Position Value", "$" + positionValueInUsd + " (" + positionValueInEth + " Ξ)", false);
                    eb.addField("Unrealized PNL", "$" + unrealizedPnlInUsd + " (" + unrealizedPnlInUsdPercentage + "%)\n" + unrealizedPnlInEth + " Ξ (" + unrealizedPnlInEthPercentage + "%)", false);
                    eb.addField("Unrealized Vol. PNL", "$" + unrealizedPnlFromVega, false);
                    eb.addField(volatilityField);
//                    eb.addField("Avg. Volatility Entry", nf.format(((ShortVol) posArray[page]).averageVolEntry * 100) + "%", true);
//                    eb.addField("Current Volatility", nf.format(((ShortVol) posArray[page]).currentVol * 100) + "%", true);
                }
            }
            case 2 -> { // crab v2
                if(posArray[2].transfers.size() == 0 || posArray[2].isValueDust(posArray[2].currentAmtHeld)) {
                    eb.setDescription("No Crab v2 position active (empty or dust)");
                } else {
                    eb.setThumbnail("https://i.imgur.com/SqABJgg.gif");
                    eb.setColor(Color.RED);
//                    eb.addField("Crab Position", "Holding " + NumberFormat.getInstance().format(posArray[1].currentAmtHeld.doubleValue()/Math.pow(10,18)) + " Crab", false);
                    eb.addField("Price of Crab v2", "$" + priceInUsd, false);
                    eb.addField("Cost Basis", "$" + costBasisInUsd + " (" + costBasisInEth + " Ξ)", false);
                    eb.addField("Position Value", "$" + positionValueInUsd + " (" + positionValueInEth + " Ξ)", false);
                    eb.addField("Unrealized PNL", "$" + unrealizedPnlInUsd + " (" + unrealizedPnlInUsdPercentage + "%)\n" + unrealizedPnlInEth + " Ξ (" + unrealizedPnlInEthPercentage + "%)", false);
                    eb.addField("Unrealized Vol. PNL", "$" + unrealizedPnlFromVega, false);
                    eb.addField(volatilityField);
//                    eb.addField("Avg. Volatility Entry", nf.format(((ShortVol) posArray[page]).averageVolEntry * 100) + "%", true);
//                    eb.addField("Current Volatility", nf.format(((ShortVol) posArray[page]).currentVol * 100) + "%", true);
                }
            }
            case 3 -> { // zen bull
                if(posArray[3].transfers.size() == 0 || posArray[3].isValueDust(posArray[3].currentAmtHeld)) {
                    eb.setDescription("No Zen Bull position active (empty or dust)");
                } else {
                    eb.setThumbnail("https://media.tenor.com/P03vVwVx-_MAAAAd/bull-grazing-gordon-ramsay-makes-masa.gif");
                    eb.setColor(Color.orange);
//                    eb.addField("Crab Position", "Holding " + NumberFormat.getInstance().format(posArray[1].currentAmtHeld.doubleValue()/Math.pow(10,18)) + " Crab", false);
                    eb.addField("Price of Zen Bull", "$" + priceInUsd, false);
                    eb.addField("Cost Basis", "$" + costBasisInUsd + " (" + costBasisInEth + " Ξ)", false);
                    eb.addField("Position Value", "$" + positionValueInUsd + " (" + positionValueInEth + " Ξ)", false);
                    eb.addField("Unrealized PNL", "$" + unrealizedPnlInUsd + " (" + unrealizedPnlInUsdPercentage + "%)\n" + unrealizedPnlInEth + " Ξ (" + unrealizedPnlInEthPercentage + "%)", false);
                    eb.addField("Unrealized Vol. PNL", "$" + unrealizedPnlFromVega, false);
                    eb.addField(volatilityField);
//                    eb.addField("Avg. Volatility Entry", nf.format(((ShortVol) posArray[page]).averageVolEntry * 100) + "%", true);
//                    eb.addField("Current Volatility", nf.format(((ShortVol) posArray[page]).currentVol * 100) + "%", true);
                }
            }
        }

    }
}
