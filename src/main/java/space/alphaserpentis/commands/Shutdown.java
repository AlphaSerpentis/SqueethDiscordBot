package space.alphaserpentis.commands;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import space.alphaserpentis.handler.CommandsHandler;
import space.alphaserpentis.main.Launcher;

public class Shutdown extends ICommand {

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

}
