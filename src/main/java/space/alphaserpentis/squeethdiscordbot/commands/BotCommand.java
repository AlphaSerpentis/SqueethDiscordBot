// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import space.alphaserpentis.squeethdiscordbot.handler.ServerDataHandler;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;

public abstract class BotCommand<T> {

    protected HashMap<Long, Long> ratelimitMap = new HashMap<>();
    protected String name, description;
    protected long commandId, ratelimitLength, messageExpirationLength;
    protected boolean onlyEmbed, onlyEphemeral, isActive = true, deferReplies, useRatelimits, messagesExpire;

    @Nonnull
    abstract public T runCommand(long userId, @Nonnull SlashCommandInteractionEvent event);

    abstract public void addCommand(@Nonnull JDA jda);
    abstract public void updateCommand(@Nonnull JDA jda);

    public void setCommandId(long id) {
        commandId = id;
    }
    @Nonnull
    public String getName() {
        return name;
    }
    @Nonnull
    public String getDescription() {
        return description;
    }
    public long getCommandId() {
        return commandId;
    }
    public long getRatelimitLength() {
        return ratelimitLength;
    }
    public long getMessageExpirationLength() {
        return messageExpirationLength;
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
    public boolean isUsingRatelimits() {
        return useRatelimits;
    }
    public boolean isUserRatelimited(long userId) {
        long ratelimit = ratelimitMap.getOrDefault(userId, 0L);

        if(ratelimit != 0) {
            return ratelimit > Instant.now().getEpochSecond();
        } else {
            return false;
        }
    }
    public boolean doMessagesExpire() {
        return messagesExpire;
    }

    @Nonnull
    public static Message handleReply(@Nonnull SlashCommandInteractionEvent event, @Nonnull BotCommand<?> cmd) {
        boolean sendAsEphemeral = cmd.isOnlyEphemeral();
        Object response;
        ReplyCallbackAction reply;

        if (cmd.isDeferReplies()) {
            InteractionHook hook = event.getHook();
            try {
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

                    if (cmd instanceof ButtonCommand) {
                        Collection<ItemComponent> buttons = ((ButtonCommand<?>) cmd).addButtons(event);

                        if (cmd.isUsingRatelimits() && !cmd.isUserRatelimited(event.getUser().getIdLong())) {
                            cmd.ratelimitMap.put(event.getUser().getIdLong(), Instant.now().getEpochSecond() + cmd.ratelimitLength);
                        }

                        if(!buttons.isEmpty())
                            return hook.sendMessageEmbeds((MessageEmbed) response).addActionRow(buttons).complete();
                    }

                    if (cmd.isUsingRatelimits() && !cmd.isUserRatelimited(event.getUser().getIdLong())) {
                        cmd.ratelimitMap.put(event.getUser().getIdLong(), Instant.now().getEpochSecond() + cmd.ratelimitLength);
                    }

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
            } catch(Exception e) {
                return hook.sendMessageEmbeds(handleError(e)).complete();
            }
        }

        try {
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
                Collection<ItemComponent> buttons = ((ButtonCommand<?>) cmd).addButtons(event);

                if(!buttons.isEmpty())
                    reply = reply.addActionRow(buttons);
            }

            return reply.complete().retrieveOriginal().complete();
        } catch(Exception e) {
            return event.replyEmbeds(handleError(e)).setEphemeral(true).complete().retrieveOriginal().complete();
        }
    }

    protected static void letMessageExpire(@Nonnull BotCommand<?> command, @Nonnull Message message) {
        if(command.doMessagesExpire()) {
            new Thread(
                    () -> {
                        try {
                            Thread.sleep(command.getMessageExpirationLength() * 1000);
                            message.delete().complete();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
            ).start();
        }
    }

    @Nonnull
    private static MessageEmbed handleError(@Nonnull Exception e) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Command Failed To Execute");
        eb.setDescription("The command failed to execute due to: " + e.getClass().getSimpleName());
        if(e.getMessage().length() > 1800) {
            eb.addField("Error Message", "Error too long...", false);
        } else {
            eb.addField("Error Message", e.getMessage(), false);
        }

        return eb.build();
    }

    @Nonnull
    private static MessageEmbed inactiveCommandResponse() {
        return new EmbedBuilder().setDescription("This command is currently not active").build();
    }
}
