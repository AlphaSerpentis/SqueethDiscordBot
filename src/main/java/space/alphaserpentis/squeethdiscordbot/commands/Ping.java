package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;

public class Ping extends ICommand {

    public Ping() {
        name = "ping";
        description = "Pong";
    }

    @Override
    public Message runCommand(long userId) {
        return new MessageBuilder("Pong").build();
    }
}
