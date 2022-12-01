// SPDX-License-Identifier: GPL-2.0-only
package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import io.reactivex.annotations.NonNull;

public class About extends BotCommand<MessageEmbed> {

    public About() {
        super(new BotCommandOptions(
            "about",
            "Description of the bot",
            true,
            false,
            TypeOfEphemeral.DEFAULT
        ));
    }

    @NonNull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("About Sqreeks²");
        eb.setDescription("Sqreeks² is a Discord bot that provides you with quick and easy to understand information about the Squeeth ecosystem developed by the Opyn team!");
        eb.addField("GitHub Repository", "https://github.com/AlphaSerpentis/SqueethDiscordBot", false);
        eb.addField("Opyn Discord", "https://discord.gg/opyn", false);
        eb.addField("Privacy Policy", "https://squeeth.com/privacy-policy", false);
        eb.addField("Acknowledgements", "Thanks to geckah for the bot name!\n\nThanks to Berry, Johns, and hanxilgf for beta testing features\n\nThanks to the Laevitas team for allowing us to use their API!", false);
        eb.setFooter("Developed by Amethyst C.");

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }
}
