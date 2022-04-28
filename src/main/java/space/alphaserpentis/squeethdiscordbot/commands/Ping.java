package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.List;

public class Ping extends BotCommand {

    public Ping() {
        name = "ping";
        description = "Pong";
    }

    @Override
    public Message runCommand(long userId) {
        return new MessageBuilder("Pong").build();
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
