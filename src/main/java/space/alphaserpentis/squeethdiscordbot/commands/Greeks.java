// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.LaevitasHandler;

import java.awt.*;
import java.math.BigDecimal;

public class Greeks extends BotCommand<MessageEmbed> {

    public Greeks() {
        super(new BotCommandOptions(
                "greeks",
                "Display the Greeks for the Squeeth!",
                true,
                false,
                TypeOfEphemeral.DEFAULT
        ));
    }

    @NonNull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        Double[] greeks = LaevitasHandler.latestSqueethData.data.getGreeks();

        eb.setTitle("Squeethy Greeks");
        if(LaevitasHandler.isDataStale()) {
            eb.setDescription("The Squeeth data is stale! Last updated at <t:" + LaevitasHandler.lastSuccessfulPoll + ">");
        }
        eb.addField("Δ Delta: " + greeks[0].toString(), "For every $1 ETH moves, oSQTH moves by $" + greeks[0], false);
        eb.addField("Γ Gamma: " + new BigDecimal(greeks[1].toString()).toPlainString(), "For every $1 ETH changes, the delta of oSQTH changes by " + new BigDecimal(greeks[1].toString()).toPlainString(), false);
        eb.addField("ν Vega: " + greeks[2].toString(), "For every 100% IV changes, oSQTH changes by $" + greeks[2], false);
        eb.addField("Θ Theta: " + greeks[3].toString(), "For every day that goes by, oSQTH decays by $" + greeks[3], false);
        eb.addField("Current IV: " + greeks[4].toString() + "%" , "As IV increases, oSQTH will increase in value and vice versa", false);
        eb.setFooter("Last Updated at " + LaevitasHandler.latestSqueethData.data.getDate() + " | API Data by Laevitas");
        eb.setColor(new Color(14, 255, 212, 76));

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }
}
