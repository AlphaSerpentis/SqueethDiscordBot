package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.*;
import space.alphaserpentis.squeethdiscordbot.handler.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.LaevitasHandler;

import java.awt.*;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Vault extends BotCommand {

    private static HashMap<Long, Long> ratelimitMap = new HashMap<>();

    private static final String controller = "0x64187ae08781b09368e6253f9e94951243a493d5";

    // No hate to the Uniswap team, but holy shit how and why
    public class Uniswapv3FuckYouMath {

        private static final String addressTickMathExternal = "0x4d9d7F7aE80d51628Aa56eF37720718C99E6FDfC", addressSqrtPriceMathPartial = "0x9cf8dcbCf115B06d8f577E73Cb9EdFdb27828460";

        public class Amount0Amount1 {
            BigInteger amount0;
            BigInteger amount1;
        }

        public BigInteger getAmount0Delta(
                BigInteger sqrtRatioAX96, // uint160
                BigInteger sqrtRatioBX96, // uint160
                BigInteger liquidity, // uint128
                boolean roundUp
        ) throws ExecutionException, InterruptedException {
            Function callGetAmount0Delta = new Function("getAmount0Delta",
                    Arrays.asList(
                            new Uint160(sqrtRatioAX96),
                            new Uint160(sqrtRatioBX96),
                            new Uint128(liquidity),
                            new Bool(roundUp)
                    ),
                    Arrays.asList(
                            new TypeReference<Uint256>() { }
                    )
            );

            List<Type> response = EthereumRPCHandler.ethCallAtLatestBlock(addressSqrtPriceMathPartial, callGetAmount0Delta);

            return (BigInteger) response.get(0).getValue();
        }

        public BigInteger getAmount1Delta(
                BigInteger sqrtRatioAX96, // uint160
                BigInteger sqrtRatioBX96, // uint160
                BigInteger liquidity, // uint128
                boolean roundUp
        ) throws ExecutionException, InterruptedException {
            Function callGetAmount1Delta = new Function("getAmount1Delta",
                    Arrays.asList(
                            new Uint160(sqrtRatioAX96),
                            new Uint160(sqrtRatioBX96),
                            new Uint128(liquidity),
                            new Bool(roundUp)
                    ),
                    Arrays.asList(
                            new TypeReference<Uint256>() { }
                    )
            );

            List<Type> response = EthereumRPCHandler.ethCallAtLatestBlock(addressSqrtPriceMathPartial, callGetAmount1Delta);

            return (BigInteger) response.get(0).getValue();
        }

        public Amount0Amount1 getToken0Token1Balances(
                BigInteger tickLower,
                BigInteger tickUpper,
                BigInteger tick,
                BigInteger liquidity
        ) throws ExecutionException, InterruptedException {
            // Call the library because I am not transposing that voodoo magic to Java
            BigInteger sqrtPriceX96 = call_getSqrtRatioAtTick(tick); // uint160
            Amount0Amount1 amounts = new Amount0Amount1();

            if(tick.compareTo(tickLower) < 0) {
                 amounts.amount0 = getAmount0Delta(
                        call_getSqrtRatioAtTick(tickLower),
                        call_getSqrtRatioAtTick(tickUpper),
                        liquidity,
                        true
                );
            } else if(tick.compareTo(tickUpper) < 0) {
                amounts.amount0 = getAmount0Delta(
                        sqrtPriceX96,
                        call_getSqrtRatioAtTick(tickUpper),
                        liquidity,
                        true
                );
                amounts.amount1 = getAmount1Delta(
                        call_getSqrtRatioAtTick(tickLower),
                        sqrtPriceX96,
                        liquidity,
                        true
                );
            } else {
                amounts.amount1 = getAmount1Delta(
                        call_getSqrtRatioAtTick(tickLower),
                        call_getSqrtRatioAtTick(tickUpper),
                        liquidity,
                        true
                );
            }

            return amounts;
        }

        private BigInteger call_getSqrtRatioAtTick(BigInteger tick) throws ExecutionException, InterruptedException {
            Function getSqrtRatioAtTick = new Function("getSqrtRatioAtTick",
                    Arrays.asList(
                            new Int24(tick)
                    ),
                    Arrays.asList(
                            new TypeReference<Uint160>() {}
                    )
            );
            List<Type> getSqrtRatioAtTickResponse = EthereumRPCHandler.ethCallAtLatestBlock(addressTickMathExternal, getSqrtRatioAtTick);
            return (BigInteger) getSqrtRatioAtTickResponse.get(0).getValue();
        }
    }

    public Vault() {
        name = "vault";
        description = "Display info on a certain short vault";
        onlyEmbed = true;
        onlyEphemeral = true;
        deferReplies = true;
    }

    @NotNull
    @Override
    public MessageEmbed runCommand(long userId, @NotNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        // check for ratelimit
        Long rateLimit = ratelimitMap.get(event.getUser().getIdLong());

        if(rateLimit != null) {
            if(rateLimit > Instant.now().getEpochSecond()) {
                eb.setDescription("You are still rate limited. Expires in " + (rateLimit - Instant.now().getEpochSecond()) + " seconds.");
                return eb.build();
            } else {
                ratelimitMap.remove(event.getUser().getIdLong());
            }
        } else {
            ratelimitMap.put(userId, Instant.now().getEpochSecond() + 60);
        }

        // web3j stuff
        List<Type> vaultsResponse;
        List<Type> priceOfEthResponse;
        List<Type> tickOfoSQTHPoolResponse;
        List<Type> uniswapv3NftResponse;
        Function callVaults = new Function("vaults",
                Arrays.asList(
                        new Uint256(event.getOptions().get(0).getAsLong())
                ),
                Arrays.asList(
                        new TypeReference<Address>() { }, // operator
                        new TypeReference<Uint32>() { }, // nft collateral id
                        new TypeReference<Uint96>() { }, // collateral
                        new TypeReference<Uint128>() { } // shortAmount
                )
        );
        Function callUniswapv3PriceCheck = new Function("getTwap",
                Arrays.asList(
                        new org.web3j.abi.datatypes.Address("0x8ad599c3a0ff1de082011efddc58f1908eb6e6d8"),
                        new org.web3j.abi.datatypes.Address("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2"),
                        new org.web3j.abi.datatypes.Address("0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"),
                        new Uint32(1),
                        new org.web3j.abi.datatypes.Bool(true)
                ),
                Arrays.asList(
                        new TypeReference<Uint256>() { }
                )
        );
        Function callUniswapv3Tick = new Function("getTimeWeightedAverageTickSafe",
                Arrays.asList(
                        new org.web3j.abi.datatypes.Address("0x82c427AdFDf2d245Ec51D8046b41c4ee87F0d29C"),
                        new Uint32(1)
                ),
                Arrays.asList(
                        new TypeReference<Int24>() { }
                )
        );

        String address;
        BigInteger nftCollateralId;
        BigInteger collateral;
        BigInteger nftEthCollateral = BigInteger.ZERO;
        BigInteger shortAmount;
        BigInteger priceOfEth;

        BigInteger normFactor = new BigInteger(String.valueOf(new DecimalFormat("#").format(LaevitasHandler.latestSqueethData.getNormalizationFactor() * (long) Math.pow(10,18))));

        try {
            vaultsResponse = EthereumRPCHandler.ethCallAtLatestBlock(controller, callVaults);
            priceOfEthResponse = EthereumRPCHandler.ethCallAtLatestBlock("0x65d66c76447ccb45daf1e8044e918fa786a483a1", callUniswapv3PriceCheck);
            tickOfoSQTHPoolResponse = EthereumRPCHandler.ethCallAtLatestBlock("0x65d66c76447ccb45daf1e8044e918fa786a483a1", callUniswapv3Tick);
            Uniswapv3FuckYouMath univ3 = new Uniswapv3FuckYouMath();

            address = (String) vaultsResponse.get(0).getValue();
            nftCollateralId = (BigInteger) vaultsResponse.get(1).getValue();
            collateral = (BigInteger) vaultsResponse.get(2).getValue();
            shortAmount = (BigInteger) vaultsResponse.get(3).getValue();
            priceOfEth = (BigInteger) priceOfEthResponse.get(0).getValue();

            if(!nftCollateralId.equals(BigInteger.ZERO)) {
                Function callUniswapv3NftPositions = new Function("positions",
                        Arrays.asList(
                                new Uint256(nftCollateralId)
                        ),
                        Arrays.asList(
                                new TypeReference<Uint96>() { },
                                new TypeReference<Address>() { },
                                new TypeReference<Address>() { },
                                new TypeReference<Address>() { },
                                new TypeReference<Uint24>() { },
                                new TypeReference<Int24>() { }, // 5 - tickLower
                                new TypeReference<Int24>() { }, // 6 - tickUpper
                                new TypeReference<Uint128>() { }, // 7 - liquidity
                                new TypeReference<Uint256>() { },
                                new TypeReference<Uint256>() { },
                                new TypeReference<Uint128>() { },
                                new TypeReference<Uint128>() { }
                        )
                );
                uniswapv3NftResponse = EthereumRPCHandler.ethCallAtLatestBlock(
                        "0xC36442b4a4522E871399CD717aBDD847Ab11FE88",
                        callUniswapv3NftPositions
                );

                Uniswapv3FuckYouMath.Amount0Amount1 amounts = univ3.getToken0Token1Balances(
                        (BigInteger) uniswapv3NftResponse.get(5).getValue(),
                        (BigInteger) uniswapv3NftResponse.get(6).getValue(),
                        (BigInteger) tickOfoSQTHPoolResponse.get(0).getValue(),
                        (BigInteger) uniswapv3NftResponse.get(7).getValue()
                );

                nftEthCollateral = amounts.amount1.multiply(normFactor).multiply(priceOfEth).divide(new BigInteger(String.valueOf(new DecimalFormat("#").format(Math.pow(10,40))))).add(amounts.amount0);
            }
        } catch (ExecutionException | InterruptedException e) {
            eb.setDescription("An error has occurred. Please try again later. If this continues to persist, reach out to the bot owner.");
            e.printStackTrace();
            return eb.build();
        }

        // EmbedBuilder stuff
        NumberFormat instance = NumberFormat.getInstance();
        double cr = calculateCollateralRatio(shortAmount, collateral.add(nftEthCollateral), priceOfEth, normFactor);

        eb.setTitle("Short Vault Data - Vault " + event.getOptions().get(0).getAsLong());
        eb.addField("Operator", "[" + address + "](https://etherscan.io/address/" + address + ")", false);
        eb.addField("NFT Collateral ID", "[" + nftCollateralId + "](https://etherscan.io/token/0xC36442b4a4522E871399CD717aBDD847Ab11FE88?a=" + nftCollateralId + ")", false);
        eb.addField("Collateral", instance.format(collateral.doubleValue() / Math.pow(10,18)) + " Ξ " + (nftEthCollateral.compareTo(BigInteger.ZERO) == 0 ? "" : "(+" + instance.format(nftEthCollateral.doubleValue() / Math.pow(10,18)) + " Ξ from NFT collateral)"), false);
        eb.addField("Short Amount", instance.format(shortAmount.doubleValue() / Math.pow(10,18)) + " oSQTH", false);
        eb.addField("Collateral Ratio", instance.format(cr) + "%", false);

        if(cr < 200) {
            eb.setColor(Color.RED);
        } else if(cr >= 200 && cr < 225) {
            eb.setColor(Color.YELLOW);
        } else {
            eb.setColor(Color.GREEN);
        }

        return eb.build();
    }

    @Override
    public void addCommand(@NotNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.INTEGER, "id", "ID of the short vault", true).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@NotNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.INTEGER, "id", "ID of the short vault", true).complete();

        commandId = cmd.getIdLong();
    }

    public double calculateCollateralRatio(BigInteger shortoSQTH, BigInteger ethCollateral, BigInteger priceOfETHinUSD, BigInteger normFactor) {
        BigInteger debt = shortoSQTH.multiply(priceOfETHinUSD).multiply(normFactor).divide(BigInteger.valueOf(10000));
        // Divide by 10^36 of debt to get the correctly scaled debt
        return ethCollateral.doubleValue() / (debt.doubleValue() / Math.pow(10,36)) * 100;
    }
}
