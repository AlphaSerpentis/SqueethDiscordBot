package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;
import space.alphaserpentis.squeethdiscordbot.handler.CommandsHandler;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

public class Shutdown extends BotCommand {

    public Shutdown() {
        name = "shutdown";
        description = "Shuts down the bot (bot admin only)";
    }

    @Override
    public Object runCommand(long userId, @NotNull SlashCommandInteractionEvent event) {
        if(userId == CommandsHandler.adminUserID) {
            Launcher.shutdown();

            return new MessageBuilder("Shutting down").build();
        } else {
            return new MessageBuilder("Not authorized").build();
        }
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
