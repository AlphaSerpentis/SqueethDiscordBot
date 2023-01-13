// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import space.alphaserpentis.squeethdiscordbot.data.api.SqueethData;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.LaevitasHandler;

import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.NumberFormat;

public class Stats extends BotCommand<MessageEmbed> {

    public Stats() {
        super(new BotCommandOptions(
            "stats",
            "Get current statistics on Squeeth",
            true,
            false,
            TypeOfEphemeral.DEFAULT
        ));
    }

    @NonNull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        SqueethData.Data data = LaevitasHandler.latestSqueethData.data;

        eb.setThumbnail("https://c.tenor.com/URrZkAPGQjAAAAAC/cat-squish-cat.gif");
        eb.setTitle("Squeeth Statistics");
        if(LaevitasHandler.isDataStale()) {
            eb.setDescription("The Squeeth data is stale! Last updated at <t:" + LaevitasHandler.lastSuccessfulPoll + ">");
        }
        eb.addField("oSQTH Price", "$" + NumberFormat.getInstance().format(data.getoSQTHPrice()), false);
        eb.addField("oSQTH Volume", NumberFormat.getInstance().format(data.getVolumeoSQTH()) + " ($" + NumberFormat.getInstance().format(data.getVolumeUSD()) + ")", false);
        eb.addField("ETH Price", "$" + NumberFormat.getInstance().format(data.getUnderlyingPrice()), false);
        eb.addField("Index Price", "$" + NumberFormat.getInstance().format(data.getIndex()), false);
        eb.addField("Mark Price", "$" + NumberFormat.getInstance().format(data.getMark()), false);
        eb.addField("Current Implied Funding", data.getCurrentImpliedFundingValue() + "%", false);
        eb.addField("Daily Funding", data.getDailyFundingValue() + "%", false);
        eb.addField("Current Implied Volatility", data.getCurrentImpliedVolatility() + "%", false);
        eb.addField("Daily Implied Volatility", data.getDailyImpliedVolatility() + "%", false);
        eb.addField("Ref. Volatility", NumberFormat.getInstance().format(getRefVol()) + "%", false);
        eb.addField("Normalization Factor", Double.toString(data.getNormalizationFactor()), false);
        eb.setFooter("Last Updated at " + LaevitasHandler.latestSqueethData.data.getDate() + " | API Data by Laevitas");
        eb.setColor(new Color(14, 255, 212, 76));

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }

    private double getRefVol() {
        try {
            HttpsURLConnection con = (HttpsURLConnection) new URL("https://squeeth.opyn.co/api/currentsqueethvol").openConnection();
            String response;

            con.setRequestMethod("GET");
            con.setInstanceFollowRedirects(true);
            con.setDoOutput(true);

            int responseCode = con.getResponseCode();

            if(responseCode == HttpsURLConnection.HTTP_OK) {
                response = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
            } else {
                return 0;
            }

            return Double.parseDouble(response) * 100;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}
