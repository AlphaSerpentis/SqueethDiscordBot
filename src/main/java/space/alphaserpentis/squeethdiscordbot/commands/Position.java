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

public class Position extends ButtonCommand<MessageEmbed> {

    private static HashMap<Long, AbstractPositions[]> cachedPositions = new HashMap<>();
    private static HashMap<String, String> cachedENSDomains = new HashMap<>();
    private static final String ethUSDPool = "0x8ad599c3a0ff1de082011efddc58f1908eb6e6d8";
    private static final String ethOSQTHPool = "0x82c427adfdf2d245ec51d8046b41c4ee87f0d29c";
    private static final String usdc = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48";
    private static final String osqth = "0xf1b99e3e573a1a9c5e6b2ce818b617f0e664e86b";
    private static final String crab = "0xf205ad80bb86ac92247638914265887a8baa437d";
    private static final String weth = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2";
    private static final String oracle = "0x65d66c76447ccb45daf1e8044e918fa786a483a1";
    private static final Function getTwap_ethUSD = new Function(
                    "getTwap",
                    Arrays.asList(
                            new org.web3j.abi.datatypes.Address(ethUSDPool),
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
                    new org.web3j.abi.datatypes.Address(ethOSQTHPool),
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
        public HashMap<Integer, BigInteger> tokensAtBlock = new HashMap<>();
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
            if(PositionsDataHandler.cachedTransfers.containsKey(userAddress)) {
                if(PositionsDataHandler.cachedTransfers.get(userAddress).stream().noneMatch(t -> t.token.equalsIgnoreCase(userAddress))) {
                    transfers = EthereumRPCHandler.getAssetTransfersOfUser(userAddress, tokenAddress);
                    PositionsDataHandler.addNewData(userAddress, transfers);
                } else {
                    transfers = (ArrayList<SimpleTokenTransferResponse>) PositionsDataHandler.cachedTransfers.get(userAddress).stream().filter(t -> t.token.equalsIgnoreCase(userAddress)).collect(Collectors.toList());
                    int highestBlock = transfers.get(transfers.size() - 1).blockNum;

                    for(SimpleTokenTransferResponse transfer: transfers) {
                        if(!transfer.token.equalsIgnoreCase(tokenAddress)) {
                            transfers = EthereumRPCHandler.getAssetTransfersOfUser(userAddress, tokenAddress, -1, highestBlock);
                            break;
                        }
                    }

                    ArrayList<SimpleTokenTransferResponse> newTransfers = EthereumRPCHandler.getAssetTransfersOfUser(userAddress, tokenAddress, highestBlock + 1, -1);
                    transfers.addAll(newTransfers);
                    PositionsDataHandler.addNewData(userAddress, transfers);
                }
            } else {
                transfers = EthereumRPCHandler.getAssetTransfersOfUser(userAddress, tokenAddress);
                PositionsDataHandler.addNewData(userAddress, transfers);
            }

            for(SimpleTokenTransferResponse transfer : transfers) {
                if(transfer.from.equalsIgnoreCase(userAddress)) { // leaves the account
                    if(tokensAtBlock.size() == 0) continue;

                    int lowestBlock = tokensAtBlock.keySet().stream().min(Integer::compareTo).get();
                    BigInteger val = tokensAtBlock.get(lowestBlock).subtract(transfer.getBigIntegerValue());

                    while(val.compareTo(BigInteger.ZERO) < 0) { // if the value is negative, we need to remove the block and subtract the remainder to the next lowest block
                        tokensAtBlock.remove(lowestBlock);
                        if(tokensAtBlock.size() == 0) break;

                        lowestBlock = tokensAtBlock.keySet().stream().min(Integer::compareTo).get();
                        val = tokensAtBlock.get(lowestBlock).subtract(val.abs());

                        if(val.compareTo(BigInteger.ZERO) > 0) {
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

        public LongPositions(@Nonnull String userAddress) {
            super(userAddress);
        }

        @Override
        public void getAndSetPrices() {
            try {
                currentPriceInEth = (BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(oracle, getTwap_osqth).get(0).getValue();
                currentPriceInUsd = currentPriceInEth.multiply((BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(oracle, getTwap_ethUSD).get(0).getValue());
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void calculateCostBasis() {
            DecimalFormat df = new DecimalFormat("#");
            // Loop through all the blocks
            for(int block : tokensAtBlock.keySet().stream().sorted().collect(Collectors.toList())) {
                BigInteger priceOsqth, priceEth;
                try {
                    PriceData priceData = new PriceData();

                    if(PositionsDataHandler.cachedPrices.containsKey((long) block)) {
                        priceData = PositionsDataHandler.cachedPrices.get((long) block);
                        priceOsqth = priceData.osqthEth;
                        priceEth = priceData.ethUsdc;
                    } else {
                        priceOsqth = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(oracle, getTwap_osqth, Long.parseLong(String.valueOf(block))).get(0).getValue();
                        priceEth = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(oracle, getTwap_ethUSD, Long.parseLong(String.valueOf(block))).get(0).getValue();

                        priceData.ethUsdc = priceEth;
                        priceData.osqthEth = priceOsqth;

                        PositionsDataHandler.addNewData((long) block, priceData);
                    }

                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                costBasisInEth = costBasisInEth.add(priceOsqth.multiply(tokensAtBlock.get(block)).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,18))))));
                costBasis = costBasis.add(priceOsqth.multiply(priceEth).multiply(tokensAtBlock.get(block)).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,36))))));
            }
        }

        @Override
        public void calculateCurrentValue() {
            DecimalFormat df = new DecimalFormat("#");
            currentValueInEth = currentAmtHeld.multiply(currentPriceInEth).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,18)))));
            currentValueInUsd = currentAmtHeld.multiply(currentPriceInUsd).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,36)))));
        }
    }

    public static class CrabPositions extends AbstractPositions {
        private static final String crab = "0xf205ad80bb86ac92247638914265887a8baa437d", oSQTH = "0xf1b99e3e573a1a9c5e6b2ce818b617f0e664e86b", pool = "0x82c427adfdf2d245ec51d8046b41c4ee87f0d29c", ethusdcPool = "0x8ad599c3a0ff1de082011efddc58f1908eb6e6d8", oracle = "0x65d66c76447ccb45daf1e8044e918fa786a483a1", usdc = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", weth = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2";

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
                        new org.web3j.abi.datatypes.Address(pool),
                        new org.web3j.abi.datatypes.Address(oSQTH),
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
                        new org.web3j.abi.datatypes.Address(ethusdcPool),
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

        public CrabPositions(@Nonnull String userAddress) {
            super(userAddress);
        }

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

        @Override
        public void calculateCostBasis() {
            DecimalFormat df = new DecimalFormat("#");
            // Loop through all the blocks
            for(int block : tokensAtBlock.keySet().stream().sorted().collect(Collectors.toList())) {
                BigInteger priceCrabEth;
                try {
                    PriceData priceData = new PriceData();
                    boolean fetchPriceAtThisBlock = false;

                    if(PositionsDataHandler.cachedPrices.containsKey((long) block)) {
                        priceData = PositionsDataHandler.cachedPrices.get((long) block);
                        if(priceData.crabEth.equals(BigInteger.ZERO) || priceData.ethUsdc.equals(BigInteger.ZERO)) {
                            fetchPriceAtThisBlock = true;
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

                        priceCrabEth = netEth.multiply(BigInteger.valueOf((long) Math.pow(10,18))).divide(crabTotalSupply);
                        priceData.osqthEth = priceOfoSQTH;
                        priceData.crabEth = priceCrabEth;
                        priceData.ethUsdc = priceOfETHinUSD;

                        PositionsDataHandler.addNewData((long) block, priceData);
                    }

                    costBasisInEth = costBasisInEth.add(priceData.crabEth.multiply(tokensAtBlock.get(block)).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,18))))));
                    costBasis = costBasis.add(priceData.crabEth.multiply(priceData.ethUsdc).multiply(tokensAtBlock.get(block)).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,36))))));
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
        name = "position";
        description = "Checks your wallet's Squeeth position";
        onlyEmbed = true;
        onlyEphemeral = true;
        deferReplies = true;
        useRatelimits = true;
        ratelimitLength = 60;

        buttonHashMap.put("Previous", Button.primary("position_previous", "Previous").asDisabled());
        buttonHashMap.put("Page", Button.secondary("position_page", "1/2").asDisabled());
        buttonHashMap.put("Next", Button.primary("position_next", "Next"));
    }

    @Nonnull
    @Override
    public MessageEmbed runCommand(long userId, @Nonnull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        if(isUserRatelimited(event.getUser().getIdLong())) {
            eb.setDescription("You are still rate limited. Expires in " + (ratelimitMap.get(event.getUser().getIdLong()) - Instant.now().getEpochSecond()) + " seconds.");
            return eb.build();
        }

        String userAddress = event.getOptions().get(0).getAsString();

        // Validate Ethereum address
        if(userAddress.length() != 42 && userAddress.startsWith("0x") && !userAddress.endsWith(".eth")) {
            eb.setDescription("Invalid Ethereum address (must be a proper Ethereum address with 0x prefix OR valid ENS name)");
            return eb.build();
        } else {
            try {
                userAddress = EthereumRPCHandler.getResolvedAddress(userAddress).toLowerCase();
            } catch (Exception e) {
                eb.setDescription("Invalid Ethereum address (must be a proper Ethereum address with 0x prefix OR valid ENS name)");
                return eb.build();
            }
        }

        AbstractPositions[] posArray = new AbstractPositions[]{
                new LongPositions(userAddress),
                new CrabPositions(userAddress)
        };

        posArray[0].getAndSetTransfers(osqth);
        posArray[1].getAndSetTransfers(crab);

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
                !(posArray[1].transfers.size() != 0 || !posArray[1].isValueDust(posArray[1].currentAmtHeld))
        ) {
            cachedPositions.remove(event.getUser().getIdLong());
        }

        return eb.build();
    }

    @Override
    public void addCommand(@Nonnull JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.STRING, "address", "Your Ethereum address", true)
                .complete();

        commandId = cmd.getIdLong();
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
                buttons.add(Button.secondary("position_page", currentPage++ + 1 + "/2").asDisabled());
            }
            case "position_previous" -> {
                displayPositionPage(eb, currentPage - 2, posArray);
                pending = event.editMessageEmbeds(eb.build());
                buttons.add(Button.secondary("position_page", currentPage-- - 1 + "/2").asDisabled());
            }
        }

        if(currentPage == 2) {
            buttons.add(0, Button.primary("position_previous", "Previous").asEnabled());
            buttons.add(Button.primary("position_next", "Next").asDisabled());
        } else if(currentPage == 1) {
            buttons.add(0, Button.primary("position_previous", "Previous").asDisabled());
            buttons.add(Button.primary("position_next", "Next").asEnabled());
        } else {
            buttons.add(0, Button.primary("position_previous", "Previous").asEnabled());
            buttons.add(Button.primary("position_next", "Next").asEnabled());
        }

        pending.setActionRow(buttons);

        pending.complete();
    }

    @Override
    public Collection<ItemComponent> addButtons(@Nonnull GenericCommandInteractionEvent event) {
        if(cachedPositions.get(event.getUser().getIdLong()) == null || isUserRatelimited(event.getUser().getIdLong())) {
            return Collections.emptyList();
        }

        return Arrays.asList(new ItemComponent[]{getButton("Previous"), getButton("Page"), getButton("Next")});
    }

    private void displayPositionPage(@Nonnull EmbedBuilder eb, int page, @Nonnull AbstractPositions[] posArray) {
        DecimalFormat df = new DecimalFormat("#");

        String priceInUsd = NumberFormat.getInstance().format(posArray[page].currentPriceInUsd.divide(new BigInteger(String.valueOf(df.format(Math.pow(10,18))))).doubleValue() / Math.pow(10,18));
        String positionHeld = NumberFormat.getInstance().format(posArray[page].currentAmtHeld.doubleValue() / Math.pow(10,18));
        String costBasisInUsd = NumberFormat.getInstance().format(posArray[page].costBasis.doubleValue() / Math.pow(10,18));
        String costBasisInEth = NumberFormat.getInstance().format(posArray[page].costBasisInEth.doubleValue() / Math.pow(10,18));
        String positionValueInUsd = NumberFormat.getInstance().format(posArray[page].currentValueInUsd.doubleValue() / Math.pow(10,18));
        String positionValueInEth = NumberFormat.getInstance().format(posArray[page].currentValueInEth.doubleValue() / Math.pow(10,18));
        String unrealizedPnlInUsd = NumberFormat.getInstance().format((posArray[page].currentValueInUsd.doubleValue() / Math.pow(10,18)) - (posArray[page].costBasis.doubleValue() / Math.pow(10,18)));
        String unrealizedPnlInEth = NumberFormat.getInstance().format((posArray[page].currentValueInEth.doubleValue() / Math.pow(10,18)) - (posArray[page].costBasisInEth.doubleValue() / Math.pow(10,18)));
        String unrealizedPnlInUsdPercentage = NumberFormat.getInstance().format(((posArray[page].currentValueInUsd.doubleValue() / Math.pow(10,18)) - (posArray[page].costBasis.doubleValue() / Math.pow(10,18))) / (posArray[page].costBasis.doubleValue() / Math.pow(10,18)) * 100);
        String unrealizedPnlInEthPercentage = NumberFormat.getInstance().format(((posArray[page].currentValueInEth.doubleValue() / Math.pow(10,18)) - (posArray[page].costBasisInEth.doubleValue() / Math.pow(10,18))) / (posArray[page].costBasisInEth.doubleValue() / Math.pow(10,18)) * 100);

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
                }
            }
            case 1 -> { // crab
                if(posArray[1].transfers.size() == 0 || posArray[1].isValueDust(posArray[1].currentAmtHeld)) {
                    eb.setDescription("No crab position active (empty or dust)");
                } else {
                    eb.setThumbnail("https://c.tenor.com/3CIbJomibvYAAAAi/crab-rave.gif");
                    eb.setColor(Color.RED);
//                    eb.addField("Crab Position", "Holding " + NumberFormat.getInstance().format(posArray[1].currentAmtHeld.doubleValue()/Math.pow(10,18)) + " Crab", false);
                    eb.addField("Price of Crab", "$" + priceInUsd, false);
                    eb.addField("Cost Basis", "$" + costBasisInUsd + " (" + costBasisInEth + " Ξ)", false);
                    eb.addField("Position Value", "$" + positionValueInUsd + " (" + positionValueInEth + " Ξ)", false);
                    eb.addField("Unrealized PNL", "$" + unrealizedPnlInUsd + " (" + unrealizedPnlInUsdPercentage + "%)\n" + unrealizedPnlInEth + " Ξ (" + unrealizedPnlInEthPercentage + "%)", false);
                }
            }
        }

    }
}
