package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.PositionsDataHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.squeeth.JumboHandler;

import java.awt.*;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class Jumbo extends BotCommand<MessageEmbed>  {

    public Jumbo() {
        super(
                new BotCommandOptions(
                        "jumbo",
                        "View statistics on Jumbo Crab & Zen Bull",
                        true,
                        false,
                        TypeOfEphemeral.DEFAULT
                )
        );
    }
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        String subcommand = event.getSubcommandName().toLowerCase();

        if(subcommand.equalsIgnoreCase("crab")) {
            try {
                PriceData priceData = PositionsDataHandler.getPriceData(new PriceData.Prices[]{PriceData.Prices.CRABV2ETH, PriceData.Prices.ETHUSD});
                JumboHandler.JumboCrabStatistics jumboCrabStats = JumboHandler.getCurrentJumboCrabStatistics();
                NumberFormat nf = NumberFormat.getInstance();
                NumberFormat cf = NumberFormat.getCurrencyInstance(Locale.US);
                MessageEmbed.Field nettingEstimates = null;
                double crabUsd = priceData.ethUsdc.multiply(priceData.crabV2Eth).doubleValue() / Math.pow(10,36);
                double crabQueuedUsdValue = jumboCrabStats.pendingCrabTokens() * crabUsd;

                // Check if netting is possible
                if(jumboCrabStats.pendingCrabTokens() > 0.01 && jumboCrabStats.pendingUsdc() > 1) {
                    double difference = Math.abs(jumboCrabStats.pendingUsdc() - crabQueuedUsdValue);
                    double crabTokensNetted, usdcNetted;

                    usdcNetted = crabQueuedUsdValue - difference;
                    crabTokensNetted = usdcNetted/crabUsd;

                    nettingEstimates = new MessageEmbed.Field(
                        "Netting Estimate",
                        "**Crab Tokens**: " + nf.format(crabTokensNetted) +
                            "\n**USDC**: " + cf.format(usdcNetted),
                        false
                    );
                }

                eb.setTitle("Jumbo Crab Statistics");
                eb.setColor(Color.RED);
                eb.setDescription("Live statistics of Jumbo Crab!");
                eb.addField("Queued USDC", cf.format(jumboCrabStats.pendingUsdc()), false);
                eb.addField("Queued Crab Tokens", nf.format(jumboCrabStats.pendingCrabTokens()), false);
                eb.addField("Netting Possible?", (nettingEstimates == null ? "No" : "Yes"), false);
                if(nettingEstimates != null)
                    eb.addField(nettingEstimates);
                eb.addField("Last Netting", "<t:" + jumboCrabStats.lastNet().time() + ">", false);
                eb.addField("Last Auction", "<t:" + jumboCrabStats.lastAuctionTime() + ">", false);
                eb.setFooter("Jumbo Crab Auctions occur every Tuesday 16:30 UTC");

            } catch (IOException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else if(subcommand.equalsIgnoreCase("zenbull")) {

        }

        return new CommandResponse<>(eb.build(), false);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        SubcommandData crab = new SubcommandData("crab", "Jumbo Crab data");
//        SubcommandData zenBull = new SubcommandData("zenbull", "Jumbo Zen Bull data");

        Command cmd = jda.upsertCommand(name, description).addSubcommands(crab).complete();

        commandId = cmd.getIdLong();
    }
}
