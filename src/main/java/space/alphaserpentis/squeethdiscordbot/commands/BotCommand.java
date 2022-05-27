package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.jetbrains.annotations.NotNull;
import space.alphaserpentis.squeethdiscordbot.handler.ServerDataHandler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Objects;

public abstract class BotCommand {

    protected String name, description;
    protected long commandId;
    protected boolean onlyEmbed, onlyEphemeral, isActive = true, deferReplies;
    protected HashMap<String, Button> buttonHashMap = new HashMap<>();

    abstract public Object runCommand(long userId, @NotNull SlashCommandInteractionEvent event);

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
    @Nullable
    public Button getButton(String key) {
        return buttonHashMap.get(key);
    }
    @Nullable
    public HashMap<String, Button> getButtonHashMap() {
        return buttonHashMap;
    }

    public static Message handleReply(@NotNull SlashCommandInteractionEvent event, BotCommand cmd) {
        boolean sendAsEphemeral = cmd.isOnlyEphemeral();
        Object response;
        ReplyCallbackAction reply;

        if (cmd.isDeferReplies()) {
            event.deferReply().complete();
            InteractionHook hook = event.getHook();
            response = cmd.isActive() ? cmd.runCommand(event.getUser().getIdLong(), event) : inactiveCommandResponse();

            if (!sendAsEphemeral && event.getGuild() != null) {
                sendAsEphemeral = ServerDataHandler.serverDataHashMap.get(event.getGuild().getIdLong()).isOnlyEphemeral();
            }

            if (cmd.isOnlyEmbed()) {
                if (!sendAsEphemeral && event.getGuild() != null) {
                    hook.setEphemeral(false);
                } else {
                    hook.setEphemeral(sendAsEphemeral);
                }

                return hook.sendMessageEmbeds((MessageEmbed) response).complete();
            } else {
                if (!sendAsEphemeral && event.getGuild() != null) {
                    hook.setEphemeral(false);
                } else {
                    hook.setEphemeral(sendAsEphemeral);

                }

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

        if (!Objects.requireNonNull(cmd.getButtonHashMap()).isEmpty())
            reply.addActionRow(((ButtonCommand) cmd).addButtons());

        return reply.complete().retrieveOriginal().complete();
    }

    private static MessageEmbed inactiveCommandResponse() {
        return new EmbedBuilder().setDescription("This command is currently not active").build();
    }
}
