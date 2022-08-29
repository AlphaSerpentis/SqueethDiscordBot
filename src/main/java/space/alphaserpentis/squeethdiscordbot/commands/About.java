// SPDX-License-Identifier: GPL-2.0-only
package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;

import javax.annotation.Nonnull;

public class About extends BotCommand<MessageEmbed> {

    public About() {
        super(new BotCommandOptions(
            "about",
            "Description of the bot",
            true,
            false
        ));
    }

    @Nonnull
    @Override
    public MessageEmbed runCommand(long userId, @Nonnull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("About Sqreeks²");
        eb.setDescription("Sqreeks² is a Discord bot that provides you with quick and easy to understand information about the Squeeth ecosystem developed by the Opyn team!");
        eb.addField("GitHub Repository", "https://github.com/AlphaSerpentis/SqueethDiscordBot", false);
        eb.addField("Opyn Discord", "https://discord.gg/opyn", false);
        eb.addField("Privacy Policy", "https://squeeth.com/privacy-policy", false);
        eb.addField("Acknowledgements", "Thanks to geckah for the bot name!\n\nThanks to Berry, Johns, and hanxilgf for beta testing features\n\nThanks to the Laevitas team for allowing us to use their API!", false);
        eb.setFooter("Developed by Amethyst C.");

        return eb.build();
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
