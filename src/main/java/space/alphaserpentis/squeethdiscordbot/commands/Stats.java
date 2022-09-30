// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import space.alphaserpentis.squeethdiscordbot.data.api.SqueethData;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.handler.LaevitasHandler;

import javax.annotation.Nonnull;
import java.awt.*;
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

    @Nonnull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @Nonnull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        SqueethData data = LaevitasHandler.latestSqueethData;

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
        eb.addField("Normalization Factor", Double.toString(data.getNormalizationFactor()), false);
        eb.setFooter("Last Updated at " + LaevitasHandler.latestSqueethData.getDate() + " | API Data by Laevitas");
        eb.setColor(new Color(14, 255, 212, 76));

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@Nonnull JDA jda) {
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }
}
