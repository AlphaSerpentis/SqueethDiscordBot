package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
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
        String subcommandGroup = event.getSubcommandGroup();
        String subcommand = event.getSubcommandName();

        if(subcommandGroup.equalsIgnoreCase("crab")) {
            eb.setColor(Color.RED);
            switch(subcommand.toLowerCase()) {
                case "stats" -> statsPage(eb);
                case "netting" -> nettingPage(eb);
                case "auction" -> auctionPage(eb);
            }
        }

        return new CommandResponse<>(eb.build(), false);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        SubcommandGroupData crab = new SubcommandGroupData("crab", "View statistics on Jumbo Crab")
                .addSubcommands(
                        new SubcommandData("stats", "View the latest Jumbo Crab statistics"),
                        new SubcommandData("netting", "View the latest Jumbo Crab netting"),
                        new SubcommandData("auction", "View the latest Jumbo Crab auction")
                                .addOption(OptionType.NUMBER, "id", "The ID of the auction to view", false)
                );

        Command cmd = jda.upsertCommand(name, description).addSubcommandGroups(crab).complete();

        commandId = cmd.getIdLong();
    }

    public void statsPage(@NonNull EmbedBuilder eb) {
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
            eb.setDescription("Live statistics of Jumbo Crab!");
            eb.addField("Queued USDC", cf.format(jumboCrabStats.pendingUsdc()), false);
            eb.addField("Queued Crab Tokens", nf.format(jumboCrabStats.pendingCrabTokens()), false);
            eb.addField("Netting Possible?", (nettingEstimates == null ? "No" : "Yes"), false);
            if(nettingEstimates != null)
                eb.addField(nettingEstimates);
            eb.addField("Last Netting", "<t:" + jumboCrabStats.lastNet().time() + ">", false);
            eb.addField("Last Auction", "<t:" + jumboCrabStats.lastAuction().time() + ">", false);
            eb.setFooter("Jumbo Crab Auctions occur every Tuesday 16:30 UTC");

        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void nettingPage(@NonNull EmbedBuilder eb) {
        try {
            JumboHandler.JumboCrabNetResults jumboCrabStats = JumboHandler.getLastJumboCrabNet();
            NumberFormat cf = NumberFormat.getCurrencyInstance(Locale.US);
            NumberFormat nf = NumberFormat.getInstance();

            eb.setTitle("Jumbo Crab Netting");
            eb.setDescription("Statistics on the last Jumbo Crab netting");
            eb.addField("USDC", cf.format(jumboCrabStats.usdcAmountNetted()), false);
            eb.addField("Crab Tokens", nf.format(jumboCrabStats.crabAmountNetted()), false);
            eb.addField("Time of Netting", "<t:" + jumboCrabStats.time() + ">", false);
        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void auctionPage(@NonNull EmbedBuilder eb) {
        try {
            JumboHandler.JumboCrabAuctionResults jumboAuctionResults = JumboHandler.getLastJumboCrabAuction();
            NumberFormat cf = NumberFormat.getCurrencyInstance(Locale.US);
            NumberFormat nf = NumberFormat.getInstance();

            eb.setTitle("Jumbo Crab Auction");
            eb.setDescription("Statistics on the last Jumbo Crab auction\n\n" +
                    "[View Auction History](https://squeethportal.xyz/auctionHistory/" + jumboAuctionResults.auctionId() + ")");
            if(jumboAuctionResults.isBuying()) {
                eb.addField("Amount of USDC Auctioned Off", cf.format(jumboAuctionResults.amountCleared()), false);
            } else {
                eb.addField("Amount of Crab Tokens Auctioned Off", nf.format(jumboAuctionResults.amountCleared()), false);
            }
            eb.addField("Squeeth Vol. at Clearing", nf.format(jumboAuctionResults.squeethVol()), false);
            eb.addField("Squeeth Ref. Vol.", nf.format(jumboAuctionResults.auction().osqthRefVol), false);
            eb.addField("Time of Auction", "<t:" + jumboAuctionResults.time() + ">", false);
        } catch (IOException | ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
