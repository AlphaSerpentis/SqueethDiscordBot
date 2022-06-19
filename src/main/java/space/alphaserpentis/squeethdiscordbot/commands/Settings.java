// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import space.alphaserpentis.squeethdiscordbot.handler.ServerDataHandler;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;

public class Settings extends BotCommand {

    private static final HashMap<String, String> defaultResponses = new HashMap<>() {{
            put("ephemeral", "Sets the server to use ephemeral messages or not. If enabled, messages can only be seen by the user who ran the command, otherwise it is publicly visible to everyone.");
            put("leaderboard", "Sets the channel to display a public leaderboard that gets updated. If disabled, the leaderboard will not be updated/posted.");
            put("random_squiz_questions", "Sets whether or not the bot should issue random Squiz questions. If enabled, the bot will post random Squiz questions every so often for everyone to see.");
            put("random_squiz_questions_channel", "Sets the channel(s) to post random Squiz questions to. To remove the channel, input the already-added channel again into the command.");
        }};

    public Settings() {
        name = "settings";
        description = "Configure the server settings";
        onlyEmbed = true;
    }

    @Nonnull
    @Override
    public MessageEmbed runCommand(long userId, @Nonnull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        List<OptionMapping> optionMappingList = event.getOptions();

        eb.setTitle("Server Settings");

        if(event.getGuild() == null) {
            eb.setDescription("Nice try.");
            eb.setImage("https://c.tenor.com/enAcmwfegOsAAAAC/wisers-slow-clap.gif");
            return eb.build();
        }

        if(verifyServerPerms(event.getMember())) {
            if(optionMappingList.isEmpty()) {
                eb.addField(event.getSubcommandName(), defaultResponses.get(event.getSubcommandName()), false);
            } else {
                switch (event.getSubcommandName().toLowerCase()) {
                    case "ephemeral" -> setChangeOnlyEphemeral(event.getGuild().getIdLong(), optionMappingList.get(0).getAsString(), eb);
                    case "leaderboard" -> setChangeLeaderboard(event.getGuild().getIdLong(), optionMappingList.get(0).getAsTextChannel(), eb);
                }
            }

        } else {
            eb.setDescription("You do not have `Manage Server` permissions");
        }

        // You remembered to add the case to the list of settings in defaultResponse, right?

        return eb.build();
    }

    @Override
    public void addCommand(@Nonnull JDA jda) {
        SubcommandData ephemeral = new SubcommandData("ephemeral", "Set messages to be ephemeral (private) or not (public)")
                .addOption(OptionType.BOOLEAN, "setting", "Setting to set to");
        SubcommandData leaderboard = new SubcommandData("leaderboard", "Configure the leaderboard settings")
                .addOption(OptionType.CHANNEL, "channel", "Channel to post the leaderboard to");

        Command cmd = jda.upsertCommand(name, description).addSubcommands(ephemeral, leaderboard).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@Nonnull JDA jda) {
        SubcommandData ephemeral = new SubcommandData("ephemeral", "Set messages to be ephemeral (private) or not (public)")
                .addOption(OptionType.BOOLEAN, "setting", "Setting to set to");
        SubcommandData leaderboard = new SubcommandData("leaderboard", "Configure the leaderboard settings")
                .addOption(OptionType.CHANNEL, "channel", "Channel to post the leaderboard to");

        Command cmd = jda.upsertCommand(name, description).addSubcommands(ephemeral, leaderboard).complete();

        commandId = cmd.getIdLong();

        System.out.println("[Settings] Updating command");
    }

    private void setChangeOnlyEphemeral(long id, String input, EmbedBuilder eb) {
        ServerDataHandler.serverDataHashMap.get(id).setOnlyEphemeral(Boolean.parseBoolean(input));

        try {
            ServerDataHandler.updateServerData();

            eb.setDescription("Setting your server to **" + (!Boolean.parseBoolean(input) ? "publicly " : "privately ") + "**display commands being ran");
        } catch(Exception e) {
            eb.setDescription("An error occured, please try again later. Report the issue if it still persists.");
        }
    }

    private void setChangeLeaderboard(long idLong, TextChannel channel, EmbedBuilder eb) {
        ServerDataHandler.serverDataHashMap.get(idLong).setLeaderboardChannelId(channel.getIdLong());

        try {
            ServerDataHandler.updateServerData();

            eb.setDescription("Setting your server's leaderboard channel to **" + channel.getName() + "**");
        } catch(Exception e) {
            eb.setDescription("An error occured, please try again later. Report the issue if it still persists.");
        }
    }

    private boolean verifyServerPerms(Member member) {
        return member.hasPermission(
                Permission.MANAGE_SERVER
        );
    }
}
