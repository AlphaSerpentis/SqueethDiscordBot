package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses;
import space.alphaserpentis.squeethdiscordbot.data.ethereum.CommonFunctions;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.PositionsDataHandler;

import java.io.IOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.concurrent.ExecutionException;

public class Simulate extends BotCommand<MessageEmbed> {

    public record GreekPnlComponents(
            double deltaPnl,
            double gammaPnl,
            double vegaPnl,
            double thetaPnl
    ) {
        public double totalPnl() {
            return deltaPnl + gammaPnl + vegaPnl + thetaPnl;
        }
    }

    public Simulate() {
        super(new BotCommandOptions(
                "simulate",
                "Simulate various positions within the Squeethcosystem",
                15,
                0,
                true,
                false,
                TypeOfEphemeral.DEFAULT,
                true,
                true,
                true,
                false
        ));
    }

    @Override
    @SuppressWarnings("unchecked")
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb;
        CommandResponse<?> response = checkAndHandleRateLimitedUser(event.getUser().getIdLong());

        if(response != null)
            return (CommandResponse<MessageEmbed>) response;

        double days = 7d;
        double priceMove = 0.1;
        double volBump = 0.1;

        // Pull the options if any
        if(event.getOptions().size() != 0) {
            if(event.getOption("days") != null) {
                days = event.getOption("days").getAsDouble();
            }
            if(event.getOption("price_move") != null) {
                priceMove = event.getOption("price_move").getAsDouble()/100;
            }
            if(event.getOption("vol_bump") != null) {
                volBump = event.getOption("vol_bump").getAsDouble()/100;
            }
        }

        eb = calculateScenarios(new double[]{days, priceMove, volBump});

        return new CommandResponse<>(eb.build(), false);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        OptionData days = new OptionData(
                OptionType.NUMBER,
                "days",
                "Days the position is open",
                false
        );
        OptionData priceMove = new OptionData(
                OptionType.NUMBER,
                "price_move",
                "Percent move since the position is opened",
                false
        );
        OptionData volBump = new OptionData(
                OptionType.NUMBER,
                "vol_bump",
                "Percent move in volatility since the position is opened",
                false
        );

        Command cmd = jda.upsertCommand(name, description).addOptions(days, priceMove, volBump).complete();

        commandId = cmd.getIdLong();
    }

    private EmbedBuilder calculateScenarios(@NonNull double[] input) {
        EmbedBuilder eb = new EmbedBuilder();
        PriceData priceData = currentPriceData();
        GreekPnlComponents longPnl, crabPnl, zenBullPnl;
        double osqthPrice, crabPrice, zenBullPrice;
        NumberFormat nf = NumberFormat.getInstance();

        // Get the prices
        osqthPrice = priceData.osqthEth.multiply(priceData.ethUsdc).divide(BigInteger.TEN.pow(18)).doubleValue() / Math.pow(10,18);
        crabPrice = priceData.crabV2Eth.multiply(priceData.ethUsdc).divide(BigInteger.TEN.pow(18)).doubleValue() / Math.pow(10,18);
        zenBullPrice = priceData.zenbull.multiply(priceData.ethUsdc).divide(BigInteger.TEN.pow(18)).doubleValue() / Math.pow(10,18);

        longPnl = calculateLong(input, priceData);
        crabPnl = calculateCrab(input, priceData);
        zenBullPnl = calculateZenBull(input, priceData);

        // Set the embed builder

        eb.setTitle("Position Simulator");
        eb.setDescription("Simulate various strategies within the Squeethcosystem.\n\n**Disclaimer**: These are estimates based on current data and or values provided by you. Rebalances are not priced in.");
        eb.addField(
                "Parameters",
                "**Time**: " + input[0] + " Days\n**Price Move**: " + input[1] * 100 + "%\n**Vol. Bump**: " + input[2] * 100 + "%",
                false
        );
        eb.addField(
                ":chart_with_upwards_trend: Long",
                "Total PnL: " + nf.format(((osqthPrice + longPnl.totalPnl()) - osqthPrice)/(osqthPrice) * 100) + "%" +
                "\n├ Price Movement: " + nf.format(((osqthPrice + longPnl.deltaPnl) - osqthPrice)/(osqthPrice) * 100) + "%" +
                "\n├ Speed of Price Movement: " + nf.format(((osqthPrice + longPnl.gammaPnl) - osqthPrice)/(osqthPrice) * 100) + "%" +
                "\n├ Volatility: " + nf.format(((osqthPrice + longPnl.vegaPnl) - osqthPrice)/(osqthPrice) * 100) + "%" +
                "\n└ Time: " + nf.format(((osqthPrice + longPnl.thetaPnl) - osqthPrice)/(osqthPrice) * 100) + "%",
                false
        );
        eb.addField(
                ":crab: Crab",
                "Total PnL: " + nf.format(((crabPrice + crabPnl.totalPnl()) - crabPrice)/(crabPrice) * 100) + "%" +
                        "\n├ Price Movement: " + nf.format(((crabPrice + crabPnl.deltaPnl) - crabPrice)/(crabPrice) * 100) + "%" +
                        "\n├ Speed of Price Movement: " + nf.format(((crabPrice + crabPnl.gammaPnl) - crabPrice)/(crabPrice) * 100) + "%" +
                        "\n├ Volatility: " + nf.format(((crabPrice + crabPnl.vegaPnl) - crabPrice)/(crabPrice) * 100) + "%" +
                        "\n└ Time: " + nf.format(((crabPrice + crabPnl.thetaPnl) - crabPrice)/(crabPrice) * 100) + "%",
                false
        );
        eb.addField(
                ":person_in_lotus_position: :ox:  Zen Bull",
                "Total PnL: " + nf.format(((zenBullPrice + zenBullPnl.totalPnl()) - zenBullPrice)/(zenBullPrice) * 100) + "%" +
                        "\n├ Price Movement: " + nf.format(((zenBullPrice + zenBullPnl.deltaPnl) - zenBullPrice)/(zenBullPrice) * 100) + "%" +
                        "\n├ Speed of Price Movement: " + nf.format(((zenBullPrice + zenBullPnl.gammaPnl) - zenBullPrice)/(zenBullPrice) * 100) + "%" +
                        "\n├ Volatility: " + nf.format(((zenBullPrice + zenBullPnl.vegaPnl) - zenBullPrice)/(zenBullPrice) * 100) + "%" +
                        "\n└ Time: " + nf.format(((zenBullPrice + zenBullPnl.thetaPnl) - zenBullPrice)/(zenBullPrice) * 100) + "%",
                false
        );
        
        return eb;
    }

    private GreekPnlComponents calculateLong(@NonNull double[] input, @NonNull PriceData priceData) {
        double normFactor = priceData.normFactor.doubleValue() / Math.pow(10,18);
        double ethPrice = priceData.ethUsdc.doubleValue() / Math.pow(10,18);
        double osqthUsd = (priceData.osqthEth.doubleValue() / Math.pow(10,18) * ethPrice);

        Vault.VaultGreeks longOsqth = new Vault.VaultGreeks(
                ethPrice,
                osqthUsd,
                normFactor,
                priceData.squeethVol,
                1,
                0
        );

        return new GreekPnlComponents(
                longOsqth.delta * (ethPrice * input[1]), // delta pnl
                0.5 * longOsqth.gamma * Math.pow(ethPrice * input[1], 2), // gamma pnl
                longOsqth.vega * input[2], // vega pnl
                longOsqth.theta * input[0] // theta pnl
        );
    }

    private GreekPnlComponents calculateCrab(@NonNull double[] input, @NonNull PriceData priceData) {
        double normFactor = priceData.normFactor.doubleValue() / Math.pow(10,18);
        double ethPrice = priceData.ethUsdc.doubleValue() / Math.pow(10,18);
        double osqthUsd = (priceData.osqthEth.doubleValue() / Math.pow(10,18) * ethPrice);
        double totalSupply;

        try {
            Crab.update(Crab.crabV2);
            totalSupply = ((BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(
                    Addresses.Squeeth.crabv2,
                    CommonFunctions.callTotalSupply
            ).get(0).getValue()).doubleValue() / Math.pow(10,18);
        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        Vault.VaultGreeks crab = new Vault.VaultGreeks(
                ethPrice,
                osqthUsd,
                normFactor,
                priceData.squeethVol,
                -Crab.crabV2.shortOsqth.doubleValue() / Math.pow(10,18)/totalSupply,
                Crab.crabV2.ethCollateral.doubleValue() / Math.pow(10,18)/totalSupply
        );

        return new GreekPnlComponents(
                crab.delta * (ethPrice * input[1]),
                0.5 * crab.gamma * Math.pow(ethPrice * input[1], 2),
                crab.vega * input[2],
                crab.theta * input[0]
        );
    }

    private GreekPnlComponents calculateZenBull(@NonNull double[] input, @NonNull PriceData priceData) {
        double ethPrice = priceData.ethUsdc.doubleValue() / Math.pow(10,18);
        double totalSupply;

        try {
            totalSupply = ((BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(
                    Addresses.Squeeth.zenbull,
                    CommonFunctions.callTotalSupply
            ).get(0).getValue()).doubleValue() / Math.pow(10,18);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        ZenBull.ZenBullGreeks greeks = ZenBull.calculateGreeks();

        return new GreekPnlComponents(
                greeks.delta() * (ethPrice * input[1]) / totalSupply,
                0.5 * greeks.gamma() * Math.pow(ethPrice * input[1], 2) / totalSupply,
                greeks.vega() * input[2] / totalSupply,
                greeks.theta() * input[0] / totalSupply
        );
    }

    @NonNull
    private PriceData currentPriceData() {
        PriceData priceData;
        try {
            priceData = PositionsDataHandler.getPriceData(new PriceData.Prices[]{
                    PriceData.Prices.ETHUSD,
                    PriceData.Prices.OSQTHETH,
                    PriceData.Prices.CRABV2ETH,
                    PriceData.Prices.ZENBULL,
                    PriceData.Prices.SQUEETHVOL,
                    PriceData.Prices.NORMFACTOR
            });
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        return priceData;
    }
}
