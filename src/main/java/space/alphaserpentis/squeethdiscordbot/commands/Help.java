// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.CommandsHandler;

import javax.annotation.Nonnull;
import java.awt.*;

public class Help extends BotCommand<MessageEmbed> {

    public Help() {
        super(new BotCommandOptions(
                "help",
                "Lists all of the available commands",
                true,
                false,
                TypeOfEphemeral.DEFAULT
        ));
    }

    @Nonnull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @Nonnull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("List of Commands");
        eb.setDescription("All of the commands are slash commands (e.g., `/greeks`). You can call these commands in a server that this bot is in or in my DMs!");
        for(BotCommand<?> cmd: CommandsHandler.mappingOfCommands.values()) {
            eb.addField(cmd.getName(), cmd.getDescription(), false);
        }
        eb.setColor(new Color(14, 255, 212, 76));

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@Nonnull JDA jda) {
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }
}
