package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;
import space.alphaserpentis.squeethdiscordbot.handler.LaevitasHandler;

import java.awt.*;
import java.math.BigDecimal;

public class Greeks extends BotCommand {

    public Greeks() {
        name = "greeks";
        description = "Display the Greeks for the Squeeth!";
        onlyEmbed = true;
    }

    @Override
    public MessageEmbed runCommand(long userId, @NotNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        Double[] greeks = LaevitasHandler.latestSqueethData.getGreeks();

        eb.setTitle("Squeethy Greeks");
        eb.addField("Δ Delta: " + greeks[0].toString(), "For every $1 ETH moves, oSQTH moves by $" + greeks[0], false);
        eb.addField("Γ Gamma: " + new BigDecimal(greeks[1].toString()).toPlainString(), "For every $1 ETH changes, the delta of oSQTH changes by " + new BigDecimal(greeks[1].toString()).toPlainString(), false);
        eb.addField("ν Vega: " + greeks[2].toString(), "For every 100% IV changes, oSQTH changes by $" + greeks[2], false);
        eb.addField("Θ Theta: " + greeks[3].toString(), "For every hour that goes by, oSQTH decays by $" + greeks[3], false);
        eb.addField("Current IV: " + greeks[4].toString() + "%" , "As IV increases, oSQTH will increase in value and vice versa", false);
        eb.setFooter("Last Updated at " + LaevitasHandler.latestSqueethData.getDate() + " | API Data by Laevitas");
        eb.setColor(new Color(14, 255, 212, 76));

        return eb.build();
    }

    @Override
    public void addCommand(@NotNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@NotNull JDA jda) {

    }
}
