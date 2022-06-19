// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import space.alphaserpentis.squeethdiscordbot.handler.CommandsHandler;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import javax.annotation.Nonnull;

public class Shutdown extends BotCommand {

    public Shutdown() {
        name = "shutdown";
        description = "Shuts down the bot (bot admin only)";
    }

    @Nonnull
    @Override
    public Message runCommand(long userId, @Nonnull SlashCommandInteractionEvent event) {
        if(userId == CommandsHandler.adminUserID) {
            Launcher.shutdown();

            return new MessageBuilder("Shutting down").build();
        } else {
            return new MessageBuilder("Not authorized").build();
        }
    }

    @Override
    public void addCommand(@Nonnull JDA jda) {
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@Nonnull JDA jda) {

    }

}
