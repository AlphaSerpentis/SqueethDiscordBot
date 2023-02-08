package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.*;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.squeeth.LaevitasHandler;

import java.awt.*;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Squeeth.controller;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Uniswap.oracle;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Uniswap.osqthEthPool;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.CommonFunctions.*;

public class Vault extends BotCommand<MessageEmbed> {

    public static class VaultGreeks {
        private final double ethUsd;
        private final double osqthUsd;
        private final double normFactor;
        private final double impliedVol;
        private final double osqthHoldings;
        private final double ethVaultCollateral;
        public double delta; // every $1 move in eth
        public double gamma;
        public double vega; // every 100%
        public double theta; // daily theta

        public VaultGreeks(
                double ethUsd,
                double osqthUsd,
                double normFactor,
                double impliedVol,
                double osqthHoldings,
                double ethVaultCollateral
        ) {
            this.ethUsd = ethUsd;
            this.osqthUsd = osqthUsd;
            this.normFactor = normFactor;
            this.impliedVol = impliedVol;
            this.osqthHoldings = osqthHoldings;
            this.ethVaultCollateral = ethVaultCollateral;

            setTheGreeks();
        }

        public void setTheGreeks() {
            double deltaPerOsqth, gammaPerOsqth, vegaPerOsqth, thetaPerOsqth;

            double exp = Math.exp(Math.pow(impliedVol, 2) * FUNDING_PERIOD);
            deltaPerOsqth = 2*normFactor*ethUsd*exp/SCALING_FACTOR;
            gammaPerOsqth = 2*normFactor*exp/SCALING_FACTOR;
            vegaPerOsqth = 2*impliedVol* FUNDING_PERIOD *osqthUsd;
            thetaPerOsqth = Math.pow(impliedVol, 2)*osqthUsd;

            delta = deltaPerOsqth*osqthHoldings+ethVaultCollateral;
            gamma = gammaPerOsqth*osqthHoldings;
            vega = vegaPerOsqth*osqthHoldings;
            theta = -thetaPerOsqth*osqthHoldings/365;
        }
    }

    // No hate to the Uniswap team, but holy shit how and why
    public static class Uniswapv3FuckYouMath {

        private static final String addressTickMathExternal = "0x4d9d7F7aE80d51628Aa56eF37720718C99E6FDfC", addressSqrtPriceMathPartial = "0x9cf8dcbCf115B06d8f577E73Cb9EdFdb27828460";

        public static class Amount0Amount1 {
            BigInteger amount0 = BigInteger.ZERO;
            BigInteger amount1 = BigInteger.ZERO;
        }

        @SuppressWarnings("rawtypes")
        public BigInteger getAmount0Delta(
                @NonNull BigInteger sqrtRatioAX96, // uint160
                @NonNull BigInteger sqrtRatioBX96, // uint160
                @NonNull BigInteger liquidity, // uint128
                boolean roundUp
        ) throws ExecutionException, InterruptedException {
            Function callGetAmount0Delta = new Function("getAmount0Delta",
                    Arrays.asList(
                            new Uint160(sqrtRatioAX96),
                            new Uint160(sqrtRatioBX96),
                            new Uint128(liquidity),
                            new Bool(roundUp)
                    ),
                    List.of(
                            new TypeReference<Uint256>() {
                            }
                    )
            );

            List<Type> response = EthereumRPCHandler.ethCallAtLatestBlock(addressSqrtPriceMathPartial, callGetAmount0Delta);

            return (BigInteger) response.get(0).getValue();
        }

        @SuppressWarnings("rawtypes")
        public BigInteger getAmount1Delta(
                @NonNull BigInteger sqrtRatioAX96, // uint160
                @NonNull BigInteger sqrtRatioBX96, // uint160
                @NonNull BigInteger liquidity, // uint128
                boolean roundUp
        ) throws ExecutionException, InterruptedException {
            Function callGetAmount1Delta = new Function("getAmount1Delta",
                    Arrays.asList(
                            new Uint160(sqrtRatioAX96),
                            new Uint160(sqrtRatioBX96),
                            new Uint128(liquidity),
                            new Bool(roundUp)
                    ),
                    List.of(
                            new TypeReference<Uint256>() {
                            }
                    )
            );

            List<Type> response = EthereumRPCHandler.ethCallAtLatestBlock(addressSqrtPriceMathPartial, callGetAmount1Delta);

            return (BigInteger) response.get(0).getValue();
        }

        public Amount0Amount1 getToken0Token1Balances(
                @NonNull BigInteger tickLower,
                @NonNull BigInteger tickUpper,
                @NonNull BigInteger tick,
                @NonNull BigInteger liquidity
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

        @SuppressWarnings("rawtypes")
        private BigInteger call_getSqrtRatioAtTick(@NonNull BigInteger tick) throws ExecutionException, InterruptedException {
            Function getSqrtRatioAtTick = new Function("getSqrtRatioAtTick",
                    List.of(
                            new Int24(tick)
                    ),
                    List.of(
                            new TypeReference<Uint160>() {
                            }
                    )
            );
            List<Type> getSqrtRatioAtTickResponse = EthereumRPCHandler.ethCallAtLatestBlock(addressTickMathExternal, getSqrtRatioAtTick);
            return (BigInteger) getSqrtRatioAtTickResponse.get(0).getValue();
        }
    }

    public Vault() {
        super(new BotCommandOptions(
            "vault",
            "Display info on a certain short vault",
            60,
            0,
            true,
            true,
            TypeOfEphemeral.DEFAULT,
            true,
            true,
            true,
            false
        ));
    }

    @NonNull
    @SuppressWarnings("rawtypes")
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        if(isUserRatelimited(event.getUser().getIdLong())) {
            eb.setDescription("You are still rate limited. Expires in " + (ratelimitMap.get(event.getUser().getIdLong()) - Instant.now().getEpochSecond()) + " seconds.");
            return new CommandResponse<>(eb.build(), onlyEphemeral);
        }
        if(event.getOptions().get(0).getAsLong() < 0) {
            eb.setDescription("Invalid ID");
            return new CommandResponse<>(eb.build(), onlyEphemeral);
        }

        // web3j stuff
        List<Type> vaultsResponse;
        List<Type> priceOfEthResponse;
        List<Type> tickOfoSQTHPoolResponse;
        List<Type> uniswapv3NftResponse;
        Function callVaults = new Function("vaults",
                List.of(
                        new Uint256(event.getOptions().get(0).getAsLong())
                ),
                Arrays.asList(
                        new TypeReference<Address>() { }, // operator
                        new TypeReference<Uint32>() { }, // nft collateral id
                        new TypeReference<Uint96>() { }, // collateral
                        new TypeReference<Uint128>() { } // shortAmount
                )
        );
        Function callUniswapv3Tick = new Function("getTimeWeightedAverageTickSafe",
                Arrays.asList(
                        new org.web3j.abi.datatypes.Address(osqthEthPool),
                        new Uint32(1)
                ),
                List.of(
                        new TypeReference<Int24>() {
                        }
                )
        );

        String address;
        BigInteger nftCollateralId;
        BigInteger collateral;
        BigInteger nftTickLower = BigInteger.ZERO;
        BigInteger nftTickUpper = BigInteger.ZERO;
        BigInteger nftCurrentTick = BigInteger.ZERO;
        BigInteger nftEthValue = BigInteger.ZERO;
        BigInteger nftEthLocked = BigInteger.ZERO;
        BigInteger nftOsqthLocked = BigInteger.ZERO;
        BigInteger shortAmount;
        BigInteger priceOfEth;

        BigInteger normFactor = new BigInteger(String.valueOf(new DecimalFormat("#").format(LaevitasHandler.latestSqueethData.data.getNormalizationFactor() * (long) Math.pow(10,18))));

        try {
            vaultsResponse = EthereumRPCHandler.ethCallAtLatestBlock(controller, callVaults);
            priceOfEthResponse = EthereumRPCHandler.ethCallAtLatestBlock(oracle, getTwap_ethUsd);
            tickOfoSQTHPoolResponse = EthereumRPCHandler.ethCallAtLatestBlock(oracle, callUniswapv3Tick);
            Uniswapv3FuckYouMath univ3 = new Uniswapv3FuckYouMath();

            address = (String) vaultsResponse.get(0).getValue();
            nftCollateralId = (BigInteger) vaultsResponse.get(1).getValue();
            collateral = (BigInteger) vaultsResponse.get(2).getValue();
            shortAmount = (BigInteger) vaultsResponse.get(3).getValue();
            priceOfEth = (BigInteger) priceOfEthResponse.get(0).getValue();

            if(!nftCollateralId.equals(BigInteger.ZERO)) {
                Function callUniswapv3NftPositions = new Function("positions",
                        List.of(
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

                nftTickLower = (BigInteger) uniswapv3NftResponse.get(5).getValue();
                nftTickUpper = (BigInteger) uniswapv3NftResponse.get(6).getValue();
                nftCurrentTick = (BigInteger) tickOfoSQTHPoolResponse.get(0).getValue();
                nftEthLocked = amounts.amount0;
                nftOsqthLocked = amounts.amount1;

                nftEthValue = amounts.amount1.multiply(normFactor).multiply(priceOfEth).divide(new BigInteger(String.valueOf(new DecimalFormat("#").format(Math.pow(10,40))))).add(amounts.amount0);
            }
        } catch (ExecutionException | InterruptedException e) {
            eb.setDescription("An error has occurred. Please try again later. If this continues to persist, reach out to the bot owner.");
            e.printStackTrace();
            return new CommandResponse<>(eb.build(), onlyEphemeral);
        }

        VaultGreeks vaultGreeks = new VaultGreeks(
            priceOfEth.doubleValue() / Math.pow(10,18),
                LaevitasHandler.latestSqueethData.data.getoSQTHPrice(),
                normFactor.doubleValue() / Math.pow(10,18),
                LaevitasHandler.latestSqueethData.data.getCurrentImpliedVolatility()/100,
                -(shortAmount.subtract(nftOsqthLocked).doubleValue() / Math.pow(10,18)),
                collateral.add(nftEthLocked).doubleValue() / Math.pow(10,18)
        );

        // EmbedBuilder stuff
        NumberFormat instance = NumberFormat.getInstance();
        double cr = calculateCollateralRatio(shortAmount, collateral.add(nftEthValue), priceOfEth, normFactor);

        eb.setTitle("Short Vault Data - Vault " + event.getOptions().get(0).getAsLong());
        eb.addField("Operator", "[" + address + "](https://etherscan.io/address/" + address + ")", false);
        if(nftCollateralId.doubleValue() != 0) {
            double tickLower = nftTickLower.doubleValue() / Math.pow(10,18), tickUpper = nftTickUpper.doubleValue() / Math.pow(10,18), currentTick = nftCurrentTick.doubleValue() / Math.pow(10,18);
            double difference = tickUpper - tickLower;

            double ethPercentage, osqthPercentage;

            osqthPercentage = ((tickLower - currentTick) / -(difference)) * 100;
            ethPercentage = 100 - osqthPercentage;

            if(osqthPercentage > 100) {
                osqthPercentage = 100;
                ethPercentage = 0;
            } else if(ethPercentage > 100) {
                ethPercentage = 100;
                osqthPercentage = 0;
            }

            eb.addField("NFT Collateral ID", "[" + nftCollateralId + "](https://etherscan.io/token/0xC36442b4a4522E871399CD717aBDD847Ab11FE88?a=" + nftCollateralId + ")", false);
            eb.addField("NFT Collateral Mixture", instance.format(nftEthLocked.doubleValue() / Math.pow(10,18)) + " Ξ (" + instance.format(ethPercentage) + "%) + " + instance.format(nftOsqthLocked.doubleValue() / Math.pow(10,18)) + " oSQTH (" + instance.format(osqthPercentage) + "%)", false);
        }
        eb.addField("Collateral", instance.format(collateral.doubleValue() / Math.pow(10,18)) + " Ξ " + (nftEthValue.compareTo(BigInteger.ZERO) == 0 ? "" : "(+" + instance.format(nftEthValue.doubleValue() / Math.pow(10,18)) + " Ξ from NFT collateral)"), false);
        eb.addField("Short Amount", instance.format(shortAmount.doubleValue() / Math.pow(10,18)) + " oSQTH", false);
        eb.addField("Collateral Ratio", instance.format(cr) + "%", false);
        eb.addField("Δ Delta", "$" + instance.format(vaultGreeks.delta), true);
        eb.addField("Γ Gamma", "$" + instance.format(vaultGreeks.gamma), true);
        eb.addBlankField(true);
        eb.addField("ν Vega", "$" + instance.format(vaultGreeks.vega), true);
        eb.addField("Θ Theta", "$" + instance.format(vaultGreeks.theta), true);
        eb.addBlankField(true);
        eb.addField("Greeks Notice", "*Greeks use some Laevitas data which is polled every 5-minutes*", false);

        if(cr < 200) {
            eb.setColor(Color.RED);
        } else if(cr >= 200 && cr < 225) {
            eb.setColor(Color.YELLOW);
        } else {
            eb.setColor(Color.GREEN);
        }

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.INTEGER, "id", "ID of the short vault", true).complete();

        commandId = cmd.getIdLong();
    }

    public double calculateCollateralRatio(@NonNull BigInteger shortoSQTH, @NonNull BigInteger ethCollateral, @NonNull BigInteger priceOfETHinUSD, @NonNull BigInteger normFactor) {
        BigInteger debt = shortoSQTH.multiply(priceOfETHinUSD).multiply(normFactor).divide(BigInteger.valueOf(10000));
        // Divide by 10^36 of debt to get the correctly scaled debt
        return ethCollateral.doubleValue() / (debt.doubleValue() / Math.pow(10,36)) * 100;
    }
}
