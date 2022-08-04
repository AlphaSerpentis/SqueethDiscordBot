// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import space.alphaserpentis.squeethdiscordbot.data.server.ServerData;
import space.alphaserpentis.squeethdiscordbot.handler.ServerDataHandler;
import space.alphaserpentis.squeethdiscordbot.handler.SquizHandler;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Settings extends BotCommand<MessageEmbed> {

    private static final HashMap<String, String> defaultResponses = new HashMap<>() {{
            put("ephemeral", "Sets the server to use ephemeral messages or not. If enabled, messages can only be seen by the user who ran the command, otherwise it is publicly visible to everyone.");
            put("leaderboard", "Sets the channel to display a public leaderboard that gets updated. If disabled, the leaderboard will not be updated/posted.");
            put("random_squiz_questions", "Sets whether or not the bot should issue random Squiz questions. If enabled, the bot will post random Squiz questions every so often for everyone to see.");
            put("random_squiz_questions_channel", "Sets the channel(s) to post random Squiz questions to. To remove the channel, input the already-added channel again into the command.");
            put("random_squiz_intervals", "Sets the 'base' interval of how long until the next Squiz question appears");
    }};

    private static final String error = "An error occurred, please try again later. If this persists, please contact AlphaSerpentis#3203 at discord.gg/opyn";

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

        String subcommandGroup = event.getSubcommandGroup();
        String subcommandName = event.getSubcommandName();

        if(verifyServerPerms(event.getMember())) {
            if(optionMappingList.isEmpty()) {
                eb.addField(subcommandName, defaultResponses.get(event.getSubcommandName()), false);
            } else {
                if(subcommandGroup != null) {
                    if(subcommandGroup.equalsIgnoreCase("squiz")) {
                        switch(subcommandName) {
                            case "leaderboard" -> setChangeLeaderboard(event.getGuild().getIdLong(), event.getOptions().get(0).getAsChannel(), eb);
                            case "random_questions" -> enableRandomQuestions(event.getGuild().getIdLong(), event.getOptions().get(0).getAsBoolean(), eb);
                            case "add_channel" -> addChannelFromEligibleChannels(event.getGuild().getIdLong(), event.getOptions().get(0).getAsChannel(), eb);
                            case "remove_channel" -> removeChannelFromEligibleChannels(event.getGuild().getIdLong(), event.getOptions().get(0).getAsChannel(), eb);
                            case "interval" -> setRandomSquizBaseInterval(event.getGuild().getIdLong(), event.getOptions().get(0).getAsLong(), eb);
                        }
                    } else if(subcommandGroup.equalsIgnoreCase("crab")) {
                        switch(subcommandName) {
                            case "auction_notifications" -> setAuctionNotifications(event.getGuild().getIdLong(), event.getOptions().get(0).getAsBoolean(), eb);
                            case "auction_channel" -> setAuctionChannel(event.getGuild().getIdLong(), event.getOptions().get(0).getAsChannel(), eb);
                        }
                    }
                } else {
                    switch(subcommandName.toLowerCase()) {
                        case "ephemeral" -> setChangeOnlyEphemeral(event.getGuild().getIdLong(), optionMappingList.get(0).getAsString(), eb);
                    }
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
        SubcommandGroupData squiz = new SubcommandGroupData("squiz", "Settings related to Squiz")
                .addSubcommands(
                        new SubcommandData("leaderboard", "Set the leaderboard channel").addOption(OptionType.CHANNEL, "channel", "The channel for the leaderboard"),
                        new SubcommandData("random_questions", "Setting if random questions are active").addOption(OptionType.BOOLEAN, "setting", "Setting whether or not random questions are actively running"),
                        new SubcommandData("add_channel", "Setting to add the channel eligible for random questions").addOption(OptionType.CHANNEL, "channel", "Channel to add to the list of eligible channels for random questions"),
                        new SubcommandData("remove_channel", "Setting to remove the channel eligible for random questions").addOption(OptionType.CHANNEL, "channel", "Channel to remove from the list of eligible channels for random questions"),
                        new SubcommandData("interval", "Setting on how often the next random Squiz will appear if eligible").addOption(OptionType.INTEGER, "seconds", "Number of seconds for each interval")
                );
        SubcommandGroupData crab = new SubcommandGroupData("crab", "Settings related to Crab")
                .addSubcommands(
                        new SubcommandData("auction_notifications", "Setting to allow the server to be notified of a Crab auction").addOption(OptionType.BOOLEAN, "setting", "Setting whether or not to allow Crab auction notifications to be sent", true),
                        new SubcommandData("auction_channel", "Setting to set the channel where auction notices are posted").addOption(OptionType.CHANNEL, "channel", "Channel where auction notices will be posted", true)
                );

        Command cmd = jda.upsertCommand(name, description).addSubcommands(ephemeral).addSubcommandGroups(squiz, crab).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@Nonnull JDA jda) {
        SubcommandData ephemeral = new SubcommandData("ephemeral", "Set messages to be ephemeral (private) or not (public)")
                .addOption(OptionType.BOOLEAN, "setting", "Setting to set to");
        SubcommandGroupData squiz = new SubcommandGroupData("squiz", "Settings related to Squiz")
                .addSubcommands(
                        new SubcommandData("leaderboard", "Set the leaderboard channel").addOption(OptionType.CHANNEL, "channel", "The channel for the leaderboard"),
                        new SubcommandData("random_questions", "Setting if random questions are active").addOption(OptionType.BOOLEAN, "setting", "Setting whether or not random questions are actively running"),
                        new SubcommandData("add_channel", "Setting to add the channel eligible for random questions").addOption(OptionType.CHANNEL, "channel", "Channel to add to the list of eligible channels for random questions"),
                        new SubcommandData("remove_channel", "Setting to remove the channel eligible for random questions").addOption(OptionType.CHANNEL, "channel", "Channel to remove from the list of eligible channels for random questions"),
                        new SubcommandData("interval", "Setting on how often the next random Squiz will appear if eligible").addOption(OptionType.INTEGER, "seconds", "Number of seconds for each interval")
                );
        SubcommandGroupData crab = new SubcommandGroupData("crab", "Settings related to Crab")
                .addSubcommands(
                        new SubcommandData("auction_notifications", "Setting to allow the server to be notified of a Crab auction").addOption(OptionType.BOOLEAN, "setting", "Setting whether or not to allow Crab auction notifications to be sent", true),
                        new SubcommandData("auction_channel", "Setting to set the channel where auction notices are posted").addOption(OptionType.CHANNEL, "channel", "Channel where auction notices will be posted", true)
                );

        Command cmd = jda.upsertCommand(name, description).addSubcommands(ephemeral).addSubcommandGroups(squiz, crab).complete();

        commandId = cmd.getIdLong();
    }

    private void setChangeOnlyEphemeral(long serverId, @Nonnull String input, @Nonnull EmbedBuilder eb) {
        ServerDataHandler.serverDataHashMap.get(serverId).setOnlyEphemeral(Boolean.parseBoolean(input));

        try {
            ServerDataHandler.updateServerData();

            eb.setDescription("Setting your server to **" + (!Boolean.parseBoolean(input) ? "publicly " : "privately ") + "**display commands being ran");
        } catch(Exception e) {
            eb.setDescription(error);
        }
    }

    private void setChangeLeaderboard(long serverId, @Nonnull GuildChannelUnion channel, @Nonnull EmbedBuilder eb) {
        ServerDataHandler.serverDataHashMap.get(serverId).setLeaderboardChannelId(channel.getIdLong());

        try {
            ServerDataHandler.updateServerData();

            eb.setDescription("Setting your server's leaderboard channel to " + channel.getAsMention());
            if(!canBotSendMessages(channel))
                eb.addField("Warning", "Bot does not have permissions to send messages in " + channel.getAsMention() + "!", false);
        } catch(Exception e) {
            eb.setDescription(error);
        }
    }

    private void enableRandomQuestions(long serverId, boolean setting, @Nonnull EmbedBuilder eb) {
        ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);
        sd.setDoRandomSquizQuestions(setting);

        try {
            ServerDataHandler.updateServerData();
            eb.setTitle("Squiz Random Questions");
            eb.setDescription("Random questions are toggled " + (setting ? "on" : "off") + ".");

            if(setting) {
                if(SquizHandler.isServerValidForRandomSquiz(serverId)) {
                    SquizHandler.scheduleServer(serverId);
                } else {
                    eb.addField("Warning", "Random Squiz settings might be misconfigured! Make sure Sqreeks has sufficient permissions to send messages!", false);
                }
            }
        } catch (IOException e) {
            eb.setTitle("Squiz Random Questions");
            eb.setDescription(error);
            e.printStackTrace();
        }
    }

    private void addChannelFromEligibleChannels(long serverId, @Nonnull GuildChannelUnion channel, @Nonnull EmbedBuilder eb) {
        ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);

        sd.getRandomSquizQuestionsChannels().add(channel.getIdLong());

        try {
            ServerDataHandler.updateServerData();
            eb.setDescription("Added " + channel.getAsMention() + " to the list of eligible channels for random questions.");

            if(SquizHandler.isServerValidForRandomSquiz(serverId)) {
                if(sd.doRandomSquizQuestions())
                    SquizHandler.scheduleServer(serverId);
            } else {
                eb.addField("Warning", "Random Squiz settings might be misconfigured! Make sure Sqreeks has sufficient permissions to send messages!", false);
            }
        } catch(Exception e) {
            eb.setDescription(error);
        }
    }

    private void removeChannelFromEligibleChannels(long serverId, @Nonnull GuildChannelUnion channel, @Nonnull EmbedBuilder eb) {
        ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);

        boolean result = sd.getRandomSquizQuestionsChannels().remove(channel.getIdLong());

        if(result) {
            try {
                ServerDataHandler.updateServerData();

                eb.setDescription("Removed " + channel.getAsMention() + " from the list of eligible channels for random questions.");
            } catch(Exception e) {
                eb.setDescription(error);
            }
        } else {
            eb.setDescription("Channel was not in the list of eligible channels for random questions");
        }
    }

    private void setRandomSquizBaseInterval(long serverId, long interval, @Nonnull EmbedBuilder eb) {
        ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);

        sd.setRandomSquizBaseIntervals(interval);

        try {
            ServerDataHandler.updateServerData();

            eb.setDescription("For every " + interval + " seconds + (random * " + interval + "), a new Squiz will be sent provided that" +
                    " there is no active random Squiz");

            SquizHandler.restartServerSquiz(serverId);
        } catch (IOException e) {
            eb.setDescription(error);
        }
    }

    private void setAuctionNotifications(long serverId, boolean setting, @Nonnull EmbedBuilder eb) {
        ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);

        sd.setListenToCrabAuctions(setting);

         try {
             ServerDataHandler.updateServerData();
             TextChannel channel = Launcher.api.getGuildById(serverId).getTextChannelById(sd.getCrabAuctionChannelId());

             eb.setDescription(setting ? "You are now listening to auction notifications" : "You are no longer listening to auction notifications");
             if(sd.getCrabAuctionChannelId() == 0 || channel == null) {
                 eb.addField("Warning", "Channel is not set for auction notifications!", false);
             } else if(!canBotSendMessages(channel)) {
                    eb.addField("Warning", "Bot is unable to talk in " + channel.getAsMention(), false);
             }

             ArrayList<Long> serverIds = Crab.v2.Auction.serversListening;

             if(setting) {
                 if(!serverIds.contains(serverId)) serverIds.add(serverId);
             } else {
                 serverIds.remove(serverId);
             }
         } catch(IOException e) {
             eb.setDescription(error);
         }
    }

    private void setAuctionChannel(long serverId, @Nonnull GuildChannelUnion channel, @Nonnull EmbedBuilder eb) {
        ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);

        sd.setCrabAuctionChannelId(channel.getIdLong());

        try {
            ServerDataHandler.updateServerData();

            eb.setDescription("Crab Auction notifications will be shown in " + channel.getAsMention());

            if(!canBotSendMessages(channel))
                eb.addField("Warning", "Bot does not have permissions to send messages in " + channel.getAsMention() + "!", false);
            if(sd.getListenToCrabAuctions()) {
                ArrayList<Long> serverIds = Crab.v2.Auction.serversListening;

                if(!serverIds.contains(serverId)) serverIds.add(serverId);
            }
        } catch(IOException e) {
            eb.setDescription(error);
        }
    }

    private boolean verifyServerPerms(Member member) {
        return member.hasPermission(
                Permission.MANAGE_SERVER
        );
    }

    private boolean canBotSendMessages(@Nonnull GuildChannelUnion channel) {
        return channel.asTextChannel().canTalk();
    }
    private boolean canBotSendMessages(@Nonnull TextChannel channel) {
        return channel.canTalk();
    }
}
