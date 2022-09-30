// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

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
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint128;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint96;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.data.api.alchemy.SimpleTokenTransferResponse;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.handler.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.PositionsDataHandler;

import javax.annotation.Nonnull;
import java.awt.*;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.*;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Squeeth.*;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Uniswap.*;

public class Position extends ButtonCommand<MessageEmbed> {

    private static final HashMap<Long, AbstractPositions[]> cachedPositions = new HashMap<>();
    private static final HashMap<String, String> cachedENSDomains = new HashMap<>();
    private static final Function getTwap_ethUsd = new Function(
                    "getTwap",
                    Arrays.asList(
                            new org.web3j.abi.datatypes.Address(ethUsdcPool),
                            new org.web3j.abi.datatypes.Address(weth),
                            new org.web3j.abi.datatypes.Address(usdc),
                            new Uint32(1),
                            new org.web3j.abi.datatypes.Bool(true)
                    ),
                    List.of(
                            new TypeReference<Uint256>() {
                            }
                    )
            );

    private static final Function getTwap_osqth = new Function(
            "getTwap",
            Arrays.asList(
                    new org.web3j.abi.datatypes.Address(osqthEthPool),
                    new org.web3j.abi.datatypes.Address(osqth),
                    new org.web3j.abi.datatypes.Address(weth),
                    new Uint32(1),
                    new org.web3j.abi.datatypes.Bool(true)
            ),
            List.of(
                    new TypeReference<Uint256>() {
                    }
            )
    );

    public abstract static class AbstractPositions {

        public ArrayList<SimpleTokenTransferResponse> transfers = new ArrayList<>();
        public final HashMap<Integer, BigInteger> tokensAtBlock = new HashMap<>();
        public final String userAddress;
        public BigInteger costBasis = BigInteger.ZERO;
        public BigInteger costBasisInEth = BigInteger.ZERO;
        public BigInteger currentAmtHeld;
        public BigInteger currentPriceInEth;
        public BigInteger currentPriceInUsd;
        public BigInteger currentValueInEth;
        public BigInteger currentValueInUsd;

        public AbstractPositions(@Nonnull String userAddress) {
            this.userAddress = userAddress;
        }

        /**
         * Obtains the transfers of the given token for userAddress
         */
        public void getAndSetTransfers(@Nonnull String tokenAddress) {
            // Check caches to see if we have the data
            if(PositionsDataHandler.cachedTransfers.containsKey(userAddress)) { // cache does contain address
                if(PositionsDataHandler.cachedTransfers.get(userAddress).stream().noneMatch(t -> t.token.equalsIgnoreCase(tokenAddress))) { // cache doesn't have the specific token we need
                    transfers = EthereumRPCHandler.getAssetTransfersOfUser(userAddress, tokenAddress);
                } else {
                    transfers = (ArrayList<SimpleTokenTransferResponse>) PositionsDataHandler.cachedTransfers.get(userAddress).stream().filter(t -> t.token.equalsIgnoreCase(tokenAddress)).collect(Collectors.toList());
                    int highestBlock = transfers.get(transfers.size() - 1).blockNum;
                    ArrayList<SimpleTokenTransferResponse> newTransfers = EthereumRPCHandler.getAssetTransfersOfUser(userAddress, tokenAddress, highestBlock + 1, -1);
                    transfers.addAll(newTransfers);
                }
            } else {
                transfers = EthereumRPCHandler.getAssetTransfersOfUser(userAddress, tokenAddress);
            }

            for(SimpleTokenTransferResponse transfer : transfers) {
                if(transfer.from.equalsIgnoreCase(userAddress)) { // leaves the account
                    if(tokensAtBlock.size() == 0) continue;

                    int lowestBlock = tokensAtBlock.keySet().stream().min(Integer::compareTo).get();
                    BigInteger val = tokensAtBlock.get(lowestBlock).subtract(transfer.getBigIntegerValue());

                    while(val.compareTo(BigInteger.ZERO) < 0) { // if the value is negative, we need to remove the block and subtract the next lowest block's value and repeat until the lowest block is no longer negative
                        tokensAtBlock.remove(lowestBlock);
                        if(tokensAtBlock.size() == 0) break;

                        lowestBlock = tokensAtBlock.keySet().stream().min(Integer::compareTo).get();
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
                    tokensAtBlock.put(transfer.getBlockNum(), transfer.getBigIntegerValue());
                }
            }

            if(tokensAtBlock.isEmpty()) { // if empty, indicates no transactions or ended up being empty as of current
                PositionsDataHandler.removeData(userAddress, tokenAddress);
            } else {
                PositionsDataHandler.addNewData(userAddress, transfers);
            }
        }
        public void calculatePnl() {
            calculateCostBasis();
            currentAmtHeld = tokensAtBlock.values().stream().reduce(BigInteger.ZERO, BigInteger::add);
            calculateCurrentValue();
        }
        public abstract void getAndSetPrices();
        public abstract void calculateCostBasis();
        public abstract void calculateCurrentValue();
        public boolean isValueDust(@Nonnull BigInteger value) {
            return value.compareTo(BigInteger.TEN.pow(12)) < 0;
        }
    }

    public static class LongPositions extends AbstractPositions {
        public static final Function getExpectedNormFactor = new Function("getExpectedNormalizationFactor",
                List.of(),
                List.of(
                        new TypeReference<Uint256>() {}
                )
        );

        public double estimatedFunding = 0;

        public LongPositions(@Nonnull String userAddress) {
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
            for(int block : tokensAtBlock.keySet().stream().sorted().toList()) {
                BigInteger priceOsqth, priceEth, transferCostBasis;
                double earliestNormFactor;
                try {
                    PriceData priceData = new PriceData();

                    if(PositionsDataHandler.cachedPrices.containsKey((long) block)) {
                        priceData = PositionsDataHandler.cachedPrices.get((long) block);
                        priceOsqth = priceData.osqthEth;
                        priceEth = priceData.ethUsdc;
                    } else {
                        priceOsqth = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(oracle, getTwap_osqth, Long.parseLong(String.valueOf(block))).get(0).getValue();
                        priceEth = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(oracle, getTwap_ethUsd, Long.parseLong(String.valueOf(block))).get(0).getValue();

                        priceData.ethUsdc = priceEth;
                        priceData.osqthEth = priceOsqth;
                    }

                    if(priceData.normFactor.equals(BigInteger.ZERO)) {
                        priceData.normFactor = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                                controller,
                                getExpectedNormFactor,
                                (long) block
                        ).get(0).getValue();
                    }

                    earliestNormFactor = priceData.normFactor.doubleValue() / Math.pow(10,18);

                    PositionsDataHandler.addNewData((long) block, priceData);

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
    }

    public static class CrabPositions extends AbstractPositions {
        private final String crab;
        private final boolean isV2;

        private final Function callVaultsFunc = new Function("getVaultDetails",
                Collections.emptyList(),
                Arrays.asList(
                        new TypeReference<Address>() { },
                        new TypeReference<Uint32>() { },
                        new TypeReference<Uint96>() { },
                        new TypeReference<Uint128>() { }
                )
        );
        private final Function callUniswapv3PriceCheck = new Function("getTwap",
                Arrays.asList(
                        new org.web3j.abi.datatypes.Address(osqthEthPool),
                        new org.web3j.abi.datatypes.Address(osqth),
                        new org.web3j.abi.datatypes.Address(weth),
                        new Uint32(1),
                        new org.web3j.abi.datatypes.Bool(true)
                ),
                List.of(
                        new TypeReference<Uint256>() {
                        }
                )
        );
        private final Function callUniswapv3PriceCheck_USDC = new Function("getTwap",
                Arrays.asList(
                        new org.web3j.abi.datatypes.Address(ethUsdcPool),
                        new org.web3j.abi.datatypes.Address(weth),
                        new org.web3j.abi.datatypes.Address(usdc),
                        new Uint32(1),
                        new org.web3j.abi.datatypes.Bool(true)
                ),
                List.of(
                        new TypeReference<Uint256>() {
                        }
                )
        );
        private final Function callTotalSupply = new Function("totalSupply",
                Collections.emptyList(),
                List.of(
                        new TypeReference<Uint256>() {
                        }
                )
        );

        public CrabPositions(@Nonnull String userAddress, @Nonnull String crabAddress, boolean isV2) {
            super(userAddress);
            crab = crabAddress;
            this.isV2 = isV2;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void getAndSetPrices() {
            try {
                BigInteger ethCollateral, shortoSQTH, priceOfoSQTH, priceOfETHinUSD, crabTotalSupply;

                List<Type> vaultDetails = EthereumRPCHandler.ethCallAtLatestBlock(crab, callVaultsFunc);
                List<Type> osqthEthPrice = EthereumRPCHandler.ethCallAtLatestBlock(oracle, callUniswapv3PriceCheck);
                List<Type> ethUsdcPrice = EthereumRPCHandler.ethCallAtLatestBlock(oracle, callUniswapv3PriceCheck_USDC);

                ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
                shortoSQTH = (BigInteger) vaultDetails.get(3).getValue();
                priceOfoSQTH = (BigInteger) osqthEthPrice.get(0).getValue();
                priceOfETHinUSD = (BigInteger) ethUsdcPrice.get(0).getValue();
                crabTotalSupply = (BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(crab, callTotalSupply).get(0).getValue();

                BigInteger netEth = ethCollateral.subtract(shortoSQTH.multiply(priceOfoSQTH).divide(BigInteger.valueOf((long) Math.pow(10,18))));

                currentPriceInEth = netEth.multiply(BigInteger.valueOf((long) Math.pow(10,18))).divide(crabTotalSupply);
                currentPriceInUsd = currentPriceInEth.multiply(priceOfETHinUSD);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void calculateCostBasis() {
            DecimalFormat df = new DecimalFormat("#");
            // Loop through all the blocks
            for(int block : tokensAtBlock.keySet().stream().sorted().toList()) {
                BigInteger priceCrabEth;
                try {
                    PriceData priceData = new PriceData();
                    boolean fetchPriceAtThisBlock = false;

                    if(PositionsDataHandler.cachedPrices.containsKey((long) block)) {
                        priceData = PositionsDataHandler.cachedPrices.get((long) block);
                        if(!isV2) {
                            if(priceData.crabEth.equals(BigInteger.ZERO) || priceData.ethUsdc.equals(BigInteger.ZERO)) {
                                fetchPriceAtThisBlock = true;
                            }
                        } else {
                            if(priceData.crabV2Eth.equals(BigInteger.ZERO) || priceData.ethUsdc.equals(BigInteger.ZERO)) {
                                fetchPriceAtThisBlock = true;
                            }
                        }
                    } else {
                        fetchPriceAtThisBlock = true;
                    }

                    if(fetchPriceAtThisBlock) {
                        BigInteger ethCollateral, shortoSQTH, priceOfoSQTH, priceOfETHinUSD, crabTotalSupply;

                        List<Type> vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(crab, callVaultsFunc, (long) block);
                        List<Type> osqthEthPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3PriceCheck, (long) block);
                        List<Type> ethUsdcPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3PriceCheck_USDC, (long) block);

                        ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
                        shortoSQTH = (BigInteger) vaultDetails.get(3).getValue();
                        priceOfoSQTH = (BigInteger) osqthEthPrice.get(0).getValue();
                        priceOfETHinUSD = (BigInteger) ethUsdcPrice.get(0).getValue();
                        crabTotalSupply = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(crab, callTotalSupply, (long) block).get(0).getValue();

                        BigInteger netEth = ethCollateral.subtract(shortoSQTH.multiply(priceOfoSQTH).divide(BigInteger.valueOf((long) Math.pow(10,18))));

                        priceCrabEth = netEth.multiply(BigInteger.valueOf((long) Math.pow(10,18))).divide(crabTotalSupply.subtract(tokensAtBlock.get(block)));
                        priceData.osqthEth = priceOfoSQTH;
                        if(!isV2) {
                            priceData.crabEth = priceCrabEth;
                        } else {
                            priceData.crabV2Eth = priceCrabEth;
                        }
                        priceData.ethUsdc = priceOfETHinUSD;

                        PositionsDataHandler.addNewData((long) block, priceData);
                    }

                    if(isV2) {
                        costBasisInEth = costBasisInEth.add(priceData.crabV2Eth.multiply(tokensAtBlock.get(block)).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,18))))));
                        costBasis = costBasis.add(priceData.crabV2Eth.multiply(priceData.ethUsdc).multiply(tokensAtBlock.get(block)).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,36))))));
                    } else {
                        costBasisInEth = costBasisInEth.add(priceData.crabEth.multiply(tokensAtBlock.get(block)).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,18))))));
                        costBasis = costBasis.add(priceData.crabEth.multiply(priceData.ethUsdc).multiply(tokensAtBlock.get(block)).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,36))))));
                    }
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void calculateCurrentValue() {
            DecimalFormat df = new DecimalFormat("#");
            currentValueInEth = currentAmtHeld.multiply(currentPriceInEth).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,18)))));
            currentValueInUsd = currentAmtHeld.multiply(currentPriceInUsd).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,36)))));
        }
    }

    public Position() {
        super(new BotCommandOptions(
            "position",
            "Checks your wallet's long Squeeth and Crab v1/v2 positions",
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
        buttonHashMap.put("Page", Button.secondary("position_page", "1/3").asDisabled());
        buttonHashMap.put("Next", Button.primary("position_next", "Next"));
    }

    @Nonnull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @Nonnull SlashCommandInteractionEvent event) {
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
                new CrabPositions(userAddress, "0xf205ad80bb86ac92247638914265887a8baa437d", false), // v1
                new CrabPositions(userAddress, "0x3b960e47784150f5a63777201ee2b15253d713e8", true) // v2
        };

        posArray[0].getAndSetTransfers(osqth);
        posArray[1].getAndSetTransfers(((CrabPositions) posArray[1]).crab);
        posArray[2].getAndSetTransfers(((CrabPositions) posArray[2]).crab);

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
                !(posArray[2].transfers.size() != 0 || !posArray[2].isValueDust(posArray[2].currentAmtHeld))
        ) {
            cachedPositions.remove(event.getUser().getIdLong());
        }

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@Nonnull JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.STRING, "address", "Your Ethereum address", true)
                .complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void runButtonInteraction(@Nonnull ButtonInteractionEvent event) {
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
                buttons.add(Button.secondary("position_page", currentPage++ + 1 + "/3").asDisabled());
            }
            case "position_previous" -> {
                displayPositionPage(eb, currentPage - 2, posArray);
                pending = event.editMessageEmbeds(eb.build());
                buttons.add(Button.secondary("position_page", currentPage-- - 1 + "/3").asDisabled());
            }
        }

        if(currentPage == 3) {
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
    @Nonnull
    public Collection<ItemComponent> addButtons(@Nonnull GenericCommandInteractionEvent event) {
        if(cachedPositions.get(event.getUser().getIdLong()) == null || isUserRatelimited(event.getUser().getIdLong())) {
            return Collections.emptyList();
        }

        return Arrays.asList(new ItemComponent[]{getButton("Previous"), getButton("Page"), getButton("Next")});
    }

    private void displayPositionPage(@Nonnull EmbedBuilder eb, int page, @Nonnull AbstractPositions[] posArray) {
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

        switch(page) {
            case 0 -> { // long squeeth
                if(posArray[0].transfers.size() == 0 || posArray[0].isValueDust(posArray[0].currentAmtHeld)) {
                    eb.setDescription("No long position active (empty or dust)");
                } else {
                    eb.setThumbnail("https://c.tenor.com/URrZkAPGQjAAAAAC/cat-squish-cat.gif");
                    eb.setColor(Color.GREEN);
                    eb.addField("Long Position", "Holding " + positionHeld, false);
                    eb.addField("Price of oSQTH", "$" + priceInUsd, false);
                    eb.addField("Cost Basis", "$" + costBasisInUsd + " (" + costBasisInEth + " Ξ)", false);
                    eb.addField("Position Value", "$" + positionValueInUsd + " (" + positionValueInEth + " Ξ)", false);
                    eb.addField("Unrealized PNL", "$" + unrealizedPnlInUsd + " (" + unrealizedPnlInUsdPercentage + "%)\n" + unrealizedPnlInEth + " Ξ (" + unrealizedPnlInEthPercentage + "%)", false);
                    eb.addField("Estimated Funding Paid", "$" + nf.format(((LongPositions) posArray[0]).estimatedFunding), false);
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
                }
            }
            case 2 -> {
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
                }
            }
        }

    }
}
