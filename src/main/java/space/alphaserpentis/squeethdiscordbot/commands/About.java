package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;

public class About extends BotCommand {

    public About() {
        name = "about";
        description = "Description of the bot";
        onlyEmbed = true;
    }

    @Override
    public Object runCommand(long userId, @NotNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("About [Name To Be Determined]");
        eb.setDescription("[Name To Be Determined] is a Discord bot that provides you with quick and easy to understand information about the Squeeth ecosystem developed by the Opyn team!");
        eb.addField("GitHub Repository", "https://github.com/AlphaSerpentis/SqueethDiscordBot", false);
        eb.addField("Opyn Discord", "https://discord.gg/opyn", true);
        eb.setFooter("Developed by Amethyst C. | API Data by Laevitas");

        return eb.build();
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
