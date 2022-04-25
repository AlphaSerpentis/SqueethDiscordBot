package space.alphaserpentis.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import space.alphaserpentis.handler.CommandsHandler;

import java.awt.*;

public class Help extends ICommand {

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
        for(ICommand cmd: CommandsHandler.mappingOfCommands.values()) {
            eb.addField(cmd.getName(), cmd.getDescription(), false);
        }
        eb.setColor(new Color(14, 255, 212, 76));

        return eb.build();
    }
}
