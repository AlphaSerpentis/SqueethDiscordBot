package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;
import space.alphaserpentis.squeethdiscordbot.data.ServerCache;

public class Clean extends BotCommand {

    public Clean() {
        name = "clean";
        description = "Cleans the last couple of messages";
        onlyEmbed = true;
        onlyEphemeral = true;
    }

    @Override
    public Object runCommand(long userId, @NotNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        if(event.getGuild() == null) {
            eb.setDescription("Only works in servers!");
            return eb.build();
        }

        if(verifyServerPerms(event.getMember())) {
            ServerCache.removeMessages(event.getGuild().getIdLong());
            eb.setDescription("Cleaned cached messages");
        } else {
            eb.setDescription("You do not have `Manage Messages` permissions");
        }

        return eb.build();
    }

    @Override
    public void addCommand(JDA jda) {
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(JDA jda) {

    }

    private boolean verifyServerPerms(Member member) {
        return member.hasPermission(
                Permission.MESSAGE_MANAGE
        );
    }
}
