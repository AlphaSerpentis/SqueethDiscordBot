package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import space.alphaserpentis.squeethdiscordbot.handler.CommandsHandler;

import java.awt.*;
import java.util.List;

public class Help extends BotCommand {

    public Help() {
        name = "help";
        description = "Lists all of the available commands";
        onlyEmbed = true;
    }

    @Override
    public Object runCommand(long userId) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("List of Commands");
        eb.setDescription("All of the commands are slash commands (e.g., `/greeks`). You can call these commands in a server that this bot is in or in my DMs!");
        for(BotCommand cmd: CommandsHandler.mappingOfCommands.values()) {
            eb.addField(cmd.getName(), cmd.getDescription(), false);
        }
        eb.setColor(new Color(14, 255, 212, 76));

        return eb.build();
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
