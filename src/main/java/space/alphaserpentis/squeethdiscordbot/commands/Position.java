package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import space.alphaserpentis.squeethdiscordbot.data.PriceData;
import space.alphaserpentis.squeethdiscordbot.data.SimpleTokenTransferResponse;
import space.alphaserpentis.squeethdiscordbot.handler.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.PositionsDataHandler;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Position extends BotCommand {

    private static HashMap<Long, Long> ratelimitMap = new HashMap<>();
    private static final String ethUSDPool = "0x8ad599c3a0ff1de082011efddc58f1908eb6e6d8";
    private static final String ethOSQTHPool = "0x82c427adfdf2d245ec51d8046b41c4ee87f0d29c";
    private static final String usdc = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48";
    private static final String osqth = "0xf1b99e3e573a1a9c5e6b2ce818b617f0e664e86b";
    private static final String weth = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2";
    private static final String oracle = "0x65d66c76447ccb45daf1e8044e918fa786a483a1";
    private static final Function getTwap_ethUSD = new Function(
                    "getTwap",
                    Arrays.asList(
                            new org.web3j.abi.datatypes.Address(ethUSDPool),
                            new org.web3j.abi.datatypes.Address(weth),
                            new org.web3j.abi.datatypes.Address(usdc),
                            new Uint32(420),
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
                    new Uint32(420),
                    new org.web3j.abi.datatypes.Bool(true)
            ),
            List.of(
                    new TypeReference<Uint256>() {
                    }
            )
    );

    public Position() {
        name = "position";
        description = "Checks your wallet's Squeeth position";
        onlyEmbed = true;
        onlyEphemeral = true;
        deferReplies = true;
//        isActive = false;
    }

    @Override
    public MessageEmbed runCommand(long userId, @NotNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        Long rateLimit = ratelimitMap.get(event.getUser().getIdLong());

        if(rateLimit != null) {
            if(rateLimit > Instant.now().getEpochSecond()) {
                eb.setDescription("You are still rate limited");
                return eb.build();
            } else {
                ratelimitMap.remove(event.getUser().getIdLong());
            }
        }

        String userAddress = event.getOptions().get(0).getAsString();
        ArrayList<SimpleTokenTransferResponse> transfers;
        BigInteger costBasis = BigInteger.ZERO, currentPriceUsd, currentPriceEth;
        HashMap<Integer, BigInteger> oSQTHAtBlock = new HashMap<>();
        ArrayList<BigInteger> oSQTHPrices_USD = new ArrayList<>();
        ArrayList<BigInteger> oSQTHPrices = new ArrayList<>();

        // Validate Ethereum address
        if(userAddress.length() != 42 && userAddress.startsWith("0x")) {
            eb.setDescription("Invalid Ethereum address (must be a proper Ethereum address with 0x prefix)");
            return eb.build();
        }

        // Check caches to see if we have the data
        if(PositionsDataHandler.cachedTransfers.containsKey(userAddress)) {
            int highestBlock = PositionsDataHandler.cachedTransfers.get(userAddress).get(PositionsDataHandler.cachedTransfers.get(userAddress).size() - 1).blockNum;
            transfers = PositionsDataHandler.cachedTransfers.get(userAddress);

            ArrayList<SimpleTokenTransferResponse> newTransfers = EthereumRPCHandler.getAssetTransfersOfUser(userAddress, osqth, highestBlock + 1);
            transfers.addAll(newTransfers);
            PositionsDataHandler.addNewData(userAddress, newTransfers);
            System.out.println("Went for the cached data");
        } else {
            transfers = EthereumRPCHandler.getAssetTransfersOfUser(userAddress, osqth);
            PositionsDataHandler.addNewData(userAddress, transfers);
            System.out.println("Went for the new data");
        }

        // TODO: FIX ROUNDING ERROR IN SIMPLETOKENTRANSFERRESPONSE
        for(SimpleTokenTransferResponse transfer : transfers) {
            if(transfer.from.equalsIgnoreCase(userAddress)) { // leaves the account
                if(oSQTHAtBlock.size() == 0) continue;

                int lowestBlock = oSQTHAtBlock.keySet().stream().min(Integer::compareTo).get();
                BigInteger val = oSQTHAtBlock.get(lowestBlock).subtract(transfer.getBigIntegerValue());

                while(val.compareTo(BigInteger.ZERO) < 0) { // if the value is negative, we need to remove the block and subtract the remainder to the next lowest block
                    oSQTHAtBlock.remove(lowestBlock);
                    if(oSQTHAtBlock.size() == 0) break;

                    lowestBlock = oSQTHAtBlock.keySet().stream().min(Integer::compareTo).get();
                    val = oSQTHAtBlock.get(lowestBlock).subtract(val.abs());

                    if(val.compareTo(BigInteger.ZERO) > 0) {
                        oSQTHAtBlock.put(lowestBlock, val);
                    }
                }

                if(val.equals(BigInteger.ZERO)) {
                    oSQTHAtBlock.remove(lowestBlock);
                } else if(val.compareTo(BigInteger.ZERO) > 0) {
                    oSQTHAtBlock.put(lowestBlock, val);
                }
            } else { // enters the account
                oSQTHAtBlock.put(transfer.getBlockNum(), transfer.getBigIntegerValue());
            }
        }

        if(oSQTHAtBlock.size() == 0) {
            eb.setDescription("You have no position");
            return eb.build();
        }

        // Grab current price data
        try {
            currentPriceEth = (BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(oracle, getTwap_osqth).get(0).getValue();
            currentPriceUsd = currentPriceEth.multiply((BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(oracle, getTwap_ethUSD).get(0).getValue());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        DecimalFormat df = new DecimalFormat("#");

        // Loop through all the blocks and calculate the PNL
        for(int block : oSQTHAtBlock.keySet().stream().sorted().collect(Collectors.toList())) {
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
            oSQTHPrices.add(priceOsqth);
            oSQTHPrices_USD.add(priceOsqth.multiply(priceEth));
            costBasis = costBasis.add(priceOsqth.multiply(priceEth).multiply(oSQTHAtBlock.get(block)).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,36))))));
        }

        BigInteger oSQTHPosition = oSQTHAtBlock.values().stream().reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger currentValue = oSQTHPosition.multiply(currentPriceUsd).divide(new BigInteger(String.valueOf(df.format(Math.pow(10,36)))));

        eb.setTitle("Position Viewer for " + userAddress);
        eb.setDescription("**Disclaimer**: This command is only for LONGS! It does not take into account your Crab tokens, shorts, or LP positions. PNL does not take into account for Uniswap slippage or gas fees. Use https://squeeth.com/positions to see your positions.");
        eb.addField("Price of oSQTH", "$" + NumberFormat.getInstance().format(currentPriceUsd.divide(new BigInteger(String.valueOf(df.format(Math.pow(10,18))))).doubleValue() / Math.pow(10,18)), false);
        eb.addField("Position Value", "$" + NumberFormat.getInstance().format(currentValue.doubleValue() / Math.pow(10,18)) + " (" + NumberFormat.getInstance().format(oSQTHPosition.doubleValue() / Math.pow(10,18)) + " oSQTH)", false);
        eb.addField("Unrealized PNL", "$" + NumberFormat.getInstance().format((currentValue.doubleValue() / Math.pow(10,18)) - (costBasis.doubleValue() / Math.pow(10,18))), false);

        // ratelimitMap.put(userId, Instant.now().getEpochSecond() + 600);

        return eb.build();
    }

    @Override
    public void addCommand(@NotNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.STRING, "address", "Your Ethereum address", true)
                .complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@NotNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.STRING, "address", "Your Ethereum address", true)
                .complete();

        commandId = cmd.getIdLong();
    }
}
