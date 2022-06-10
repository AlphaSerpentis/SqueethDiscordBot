package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import space.alphaserpentis.squeethdiscordbot.handler.ServerDataHandler;

import java.util.Collection;

public abstract class BotCommand<T> {

    protected String name, description;
    protected long commandId;
    protected boolean onlyEmbed, onlyEphemeral, isActive = true, deferReplies;

    abstract public T runCommand(long userId, @NotNull SlashCommandInteractionEvent event);

    abstract public void addCommand(@NotNull JDA jda);
    abstract public void updateCommand(@NotNull JDA jda);

    public void setCommandId(long id) {
        commandId = id;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public long getCommandId() {
        return commandId;
    }
    public boolean isOnlyEmbed() { return onlyEmbed; }
    public boolean isOnlyEphemeral() {
        return onlyEphemeral || !isActive;
    }
    public boolean isActive() {
        return isActive;
    }
    public boolean isDeferReplies() {
        return deferReplies;
    }

    public static Message handleReply(@NotNull SlashCommandInteractionEvent event, BotCommand cmd) {
        boolean sendAsEphemeral = cmd.isOnlyEphemeral();
        Object response;
        ReplyCallbackAction reply;

        if (cmd.isDeferReplies()) {
            InteractionHook hook = event.getHook();
            if (!sendAsEphemeral && event.getGuild() != null) {
                sendAsEphemeral = ServerDataHandler.serverDataHashMap.get(event.getGuild().getIdLong()).isOnlyEphemeral();
            }

            if (cmd.isOnlyEmbed()) {
                if (!sendAsEphemeral && event.getGuild() != null) {
                    event.deferReply(false).complete();
                } else {
                    event.deferReply(sendAsEphemeral).complete();
                }
                response = cmd.isActive() ? cmd.runCommand(event.getUser().getIdLong(), event) : inactiveCommandResponse();

                return hook.sendMessageEmbeds((MessageEmbed) response).complete();
            } else {
                if (!sendAsEphemeral && event.getGuild() != null) {
                    hook.setEphemeral(false);
                } else {
                    hook.setEphemeral(sendAsEphemeral);
                }
                response = cmd.isActive() ? cmd.runCommand(event.getUser().getIdLong(), event) : inactiveCommandResponse();

                return hook.sendMessage((String) response).complete();
            }
        }

        response = cmd.isActive() ? cmd.runCommand(event.getUser().getIdLong(), event) : inactiveCommandResponse();

        if (!sendAsEphemeral && event.getGuild() != null)
            sendAsEphemeral = ServerDataHandler.serverDataHashMap.get(event.getGuild().getIdLong()).isOnlyEphemeral();

        if (cmd.isOnlyEmbed()) {
            if (!sendAsEphemeral && event.getGuild() != null) {
                reply = event.replyEmbeds((MessageEmbed) response).setEphemeral(false);
            } else {
                reply = event.replyEmbeds((MessageEmbed) response).setEphemeral(sendAsEphemeral);
            }
        } else {
            if (!sendAsEphemeral && event.getGuild() != null) {
                reply = event.reply((Message) response).setEphemeral(false);
            } else {
                reply = event.reply((Message) response).setEphemeral(sendAsEphemeral);
            }
        }

        if (cmd instanceof ButtonCommand) {
            Collection<ItemComponent> buttons = ((ButtonCommand) cmd).addButtons(event);

            if(!buttons.isEmpty())
                reply.addActionRow(buttons);
        }

        return reply.complete().retrieveOriginal().complete();
    }

    private static MessageEmbed inactiveCommandResponse() {
        return new EmbedBuilder().setDescription("This command is currently not active").build();
    }
}
