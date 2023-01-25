package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses;
import space.alphaserpentis.squeethdiscordbot.data.ethereum.CommonFunctions;
import space.alphaserpentis.squeethdiscordbot.data.ethereum.struct.AssetConfig;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.PositionsDataHandler;

import java.awt.*;
import java.io.IOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Euler.marketsProxy;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Euler.simpleLens;

public class ZenBull extends BotCommand<MessageEmbed> {
    private static ZenBullData lastData;
    private static Crab.CrabVault crabv2;
    private long lastRun = 0;

    public record ZenBullData(
            // Denominated in a per-token basis
            BigInteger crabValue,
            // Denominated in a per-token basis
            BigInteger eulerDebt,
            // Denominated in a per-token basis
            BigInteger eulerCollateral,
            // Total supply of Zen Bull tokens
            BigInteger totalSupply
    ) {}

    public record ZenBullGreeks(
            double delta,
            double gamma,
            double theta,
            double vega
    ) {}

    public ZenBull() {
        super(new BotCommandOptions(
           "zenbull",
           "Get current statistics on the Zen Bull strategy!",
           0,
           0,
           true,
           false,
           TypeOfEphemeral.DYNAMIC,
           true,
           true,
           false,
           false
        ));
    }

    @Override
    public CommandResponse<MessageEmbed> beforeRunCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        return new CommandResponse<>(null, false);
    }

    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        // maybe swap this out for a switch in the future
        if(event.getSubcommandName().equalsIgnoreCase("stats")) {
            statsPage(eb);
        }

        return new CommandResponse<>(eb.build(), false);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        SubcommandData stats = new SubcommandData("stats", "Regular statistics on Zen Bull");

        Command cmd = jda.upsertCommand(name, description).addSubcommands(stats).complete();

        commandId = cmd.getIdLong();
    }


    // PAGES
    private void statsPage(@NonNull EmbedBuilder eb) {
        PriceData priceData;
        ZenBullGreeks greeks;
        NumberFormat instance = NumberFormat.getInstance();

        try {
            priceData = PositionsDataHandler.getPriceData(
                    new PriceData.Prices[]{PriceData.Prices.ZENBULL}
            );
            greeks = calculateGreeks();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }

        eb.setTitle("Zen Bull Statistics");
        eb.setThumbnail("https://media.tenor.com/P03vVwVx-_MAAAAd/bull-grazing-gordon-ramsay-makes-masa.gif");
        eb.setDescription("Get all of your Zen Bull stats here!\n\nhttps://squeeth.com/strategies/bull");
        eb.addField("**Price per Zen Bull Token**: " + instance.format(priceData.zenbull.doubleValue() / Math.pow(10,18)) + " Ξ",
                    "\n├ Euler Debt: " +  instance.format(-lastData.eulerDebt.doubleValue() / Math.pow(10,18)) + " Ξ" +
                        "\n├ Euler Collateral: " + instance.format(lastData.eulerCollateral.doubleValue() / Math.pow(10,18)) + " Ξ" +
                        "\n└ Crab: " + instance.format(lastData.crabValue.doubleValue() / Math.pow(10,18)) + " Ξ",
                false
        );
        eb.addField("Crab Collat. Ratio", instance.format(crabv2.calculateCollateralRatio()) + "%", false);
        eb.addField("Euler Health Factor", instance.format(calculateEulerHealthFactor()), false);
        eb.addField("Δ Delta", "$" + instance.format(greeks.delta), true);
        eb.addField("Θ Theta", "$" + instance.format(greeks.theta), true);
        eb.addBlankField(true);
        eb.addField("Γ Gamma", "$" + instance.format(greeks.gamma), true);
        eb.addField("ν Vega", "$" + instance.format(greeks.vega), true);
        eb.addBlankField(true);

//        eb.addField("Euler Debt", instance.format(lastData.eulerDebt.doubleValue() / Math.pow(10,18)) + " Ξ", false);
//        eb.addField("Euler Collateral", instance.format(lastData.eulerCollateral.doubleValue() / Math.pow(10,18)) + " Ξ", false);
//        eb.addField("Crab", instance.format(lastData.crabValue.doubleValue() / Math.pow(10,18)) + " Ξ", false);
//        eb.addField("Price per Zen Bull Token", instance.format(priceData.zenbull.doubleValue() / Math.pow(10,18)) + " Ξ", false);
        eb.setColor(Color.ORANGE);
    }

    // MISC
    public static void updateZenBullVault(@NonNull ZenBullData data) {
        lastData = data;

        if(crabv2 == null) {
            crabv2 = Crab.crabV2;
            try {
                Crab.update(crabv2);
            } catch (IOException | ExecutionException | InterruptedException e) {
                crabv2 = null;
                throw new RuntimeException(e);
            }
        }
    }

    private static Function underlyingToAssetConfig(@NonNull String underlying) {
        return new Function(
                "underlyingToAssetConfig",
                List.of(
                        new Address(underlying)
                ),
                List.of(
                        new TypeReference<AssetConfig>() {
                        }
                )
        );
    }

    private static Function interestRates(@NonNull String underlying) {
        return new Function(
                "interestRates",
                List.of(
                        new Address(underlying)
                ),
                List.of(
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {}
                )
        );
    }

    private double calculateEulerHealthFactor() {
        double debtValue = lastData.eulerDebt.doubleValue() / Math.pow(10,18);
        double collateralValue = lastData.eulerCollateral.doubleValue() / Math.pow(10,18);
        double usdcBorrowFactor, wethCollateralFactor;

        // Get collateral and borrowing factors
        try {
            usdcBorrowFactor = (double) ((Uint32) ((List<?>) EthereumRPCHandler.ethCallAtLatestBlock(
                    marketsProxy,
                    underlyingToAssetConfig(Addresses.usdc)
            ).get(0).getValue()).get(3)).getValue().longValue() / (4 * Math.pow(10,9));
            wethCollateralFactor = (double) ((Uint32) ((List<?>) EthereumRPCHandler.ethCallAtLatestBlock(
                    marketsProxy,
                    underlyingToAssetConfig(Addresses.weth)
            ).get(0).getValue()).get(2)).getValue().longValue() / (4 * Math.pow(10,9));
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return (collateralValue * wethCollateralFactor)/(debtValue / usdcBorrowFactor);
    }

    @NonNull
    public static ZenBullGreeks calculateGreeks() {
        double delta, gamma, theta, vega, crabTotalSupply, crabBalance;
        delta = gamma = theta = vega = 0;

        try {
            crabTotalSupply = ((BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(
                    Addresses.Squeeth.crabv2,
                    CommonFunctions.callTotalSupply
            ).get(0).getValue()).doubleValue() / Math.pow(10,18);
            crabBalance = ((BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(
                    Addresses.Squeeth.crabv2,
                    CommonFunctions.balanceOf(Addresses.Squeeth.zenbull)
            ).get(0).getValue()).doubleValue() / Math.pow(10,18);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }

        // Add Crab Greeks
        Vault.VaultGreeks crabGreeks = crabv2.lastRunVaultGreeks;
        delta += crabGreeks.delta * (crabBalance/crabTotalSupply);
        gamma += crabGreeks.gamma * (crabBalance/crabTotalSupply);
        theta += crabGreeks.theta * (crabBalance/crabTotalSupply);
        vega += crabGreeks.vega * (crabBalance/crabTotalSupply);

        // Add Euler Greeks
        delta += lastData.eulerCollateral.multiply(lastData.totalSupply).divide(BigInteger.TEN.pow(18)).doubleValue() / Math.pow(10,18);
        theta += getTotalEulerInterest();

        return new ZenBullGreeks(
                delta,
                gamma,
                theta,
                vega
        );
    }

    private static double getTotalEulerInterest() {
        BigInteger ethSupplyApy, usdcBorrowApy, ethUsdcPrice, ethCollateral, usdcDebt;
        double ethInterest, usdcInterest;

        try {
            ethUsdcPrice = PositionsDataHandler.getPriceData(
                    new PriceData.Prices[]{PriceData.Prices.ETHUSD}
            ).ethUsdc;
            usdcBorrowApy = (BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(
                    simpleLens,
                    interestRates(Addresses.usdc)
            ).get(1).getValue();
            ethSupplyApy = (BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(
                    simpleLens,
                    interestRates(Addresses.weth)
            ).get(2).getValue();
            ethCollateral = lastData.eulerCollateral.multiply(lastData.totalSupply).divide(BigInteger.TEN.pow(18));
            usdcDebt = lastData.eulerDebt.multiply(lastData.totalSupply).multiply(ethUsdcPrice).divide(BigInteger.TEN.pow(36));
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

//        ethInterest = (ethCollateral.doubleValue() / Math.pow(10,18) * Math.pow(1 + (ethSupplyApy.doubleValue() / Math.pow(10,27)/(31556952)), 31556952) - ethCollateral.doubleValue() / Math.pow(10,18)) * (ethUsdcPrice.doubleValue()/Math.pow(10,18));
        ethInterest = (ethCollateral.doubleValue() / Math.pow(10,18) * (ethSupplyApy.doubleValue() / Math.pow(10,27)) * (ethUsdcPrice.doubleValue()/Math.pow(10,18)));
//        usdcInterest = (usdcDebt.doubleValue() / Math.pow(10,18) * Math.pow(1 + (usdcBorrowApy.doubleValue() / Math.pow(10,27)/(31556952)), 31556952) - usdcDebt.doubleValue() / Math.pow(10,18));
        usdcInterest = usdcDebt.doubleValue() / Math.pow(10,18) * (usdcBorrowApy.doubleValue() / Math.pow(10,27));

//        System.out.println(ethInterest);
//        System.out.println(usdcInterest);

        return (ethInterest - usdcInterest)/365.25;
    }
}
