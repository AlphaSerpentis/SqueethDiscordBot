package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import space.alphaserpentis.squeethdiscordbot.data.SqueethData;
import space.alphaserpentis.squeethdiscordbot.handler.LaevitasHandler;

import java.awt.*;
import java.text.NumberFormat;

public class Stats extends BotCommand {

    public Stats() {
        name = "stats";
        description = "Get current statistics on Squeeth!";
        onlyEmbed = true;
    }

    @Override
    public Object runCommand(long userId, @NotNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        SqueethData data = LaevitasHandler.latestSqueethData;

        eb.setThumbnail("https://c.tenor.com/URrZkAPGQjAAAAAC/cat-squish-cat.gif");
        eb.setTitle("Squeeth Statistics");
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

        return eb.build();
    }

    @Override
    public void addCommand(JDA jda) {
        net.dv8tion.jda.api.interactions.commands.Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(JDA jda) {

    }
}
