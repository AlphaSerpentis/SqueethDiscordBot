package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import space.alphaserpentis.squeethdiscordbot.handler.CommandsHandler;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import java.util.List;

public class Shutdown extends BotCommand {

    public Shutdown() {
        name = "shutdown";
        description = "Shuts down the bot (bot admin only)";
    }

    @Override
    public Message runCommand(long userId) {
        if(userId == CommandsHandler.adminUserID) {
            Launcher.shutdown();

            return new MessageBuilder("Shutting down").build();
        } else {
            return new MessageBuilder("Not authorized").build();
        }
    }

    @Override
    public Object runCommand(long userId, List<OptionMapping> optionMappingList) {
        return runCommand(userId);
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
