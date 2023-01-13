// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.data.server.ServerCache;
import io.reactivex.annotations.NonNull;

public class Clean extends BotCommand<MessageEmbed> {

    public Clean() {
        super(new BotCommandOptions(
            "clean",
            "Cleans the last couple of messages",
            true,
            true,
            TypeOfEphemeral.DEFAULT
        ));
    }

    @NonNull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        if(event.getGuild() == null) {
            eb.setDescription("Only works in servers!");
            return new CommandResponse<>(eb.build(), onlyEphemeral);
        }

        if(verifyServerPerms(event.getMember())) {
            ServerCache.removeMessages(event.getGuild().getIdLong());
            eb.setDescription("Cleaned cached messages");
        } else {
            eb.setDescription("You do not have `Manage Messages` permissions");
        }

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }

    private boolean verifyServerPerms(@NonNull Member member) {
        return member.hasPermission(
                Permission.MESSAGE_MANAGE
        );
    }
}
