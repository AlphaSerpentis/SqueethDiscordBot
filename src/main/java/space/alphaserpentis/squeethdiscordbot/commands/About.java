package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;

public class About extends ICommand {

    public About() {
        name = "about";
        description = "Description of the bot";
        onlyEmbed = true;
    }

    @Override
    public Object runCommand(long userId) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("About [Name To Be Determined]");
        eb.setDescription("[Name To Be Determined] is a Discord bot that provides you with quick and easy to understand information about the Squeeth ecosystem developed by the Opyn team!");
        eb.addField("GitHub Repository", "https://github.com/AlphaSerpentis/SqueethDiscordBot", false);
        eb.addField("Opyn Discord", "https://discord.gg/opyn", true);
        eb.setFooter("Developed by Amethyst C. | API Data by Laevitas");

        return eb.build();
    }
}
