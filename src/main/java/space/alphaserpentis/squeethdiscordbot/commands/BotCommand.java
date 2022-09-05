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
import java.util.concurrent.TimeUnit;

public abstract class BotCommand<T> {

    public static class BotCommandOptions {
        protected final String name;
        protected final String description;
        protected final long ratelimitLength;
        protected final long messageExpirationLength;
        protected final boolean onlyEmbed;
        protected final boolean onlyEphemeral;
        protected final boolean isActive;
        protected final boolean deferReplies;
        protected final boolean useRatelimits;
        protected final boolean messagesExpire;
        protected final long defaultRatelimitLength = 0;
        protected final long defaultMessageExpirationLength = 0;
        protected final boolean defaultOnlyEmbed = false;
        protected final boolean defaultOnlyEphemeral = false;
        protected final boolean defaultIsActive = true;
        protected final boolean defaultDeferReplies = false;
        protected final boolean defaultUseRatelimits = false;
        protected final boolean defaultMessagesExpire = false;

        public BotCommandOptions(
                @Nonnull String name,
                @Nonnull String description
        ) {
            this.name = name;
            this.description = description;
            ratelimitLength = defaultRatelimitLength;
            messageExpirationLength = defaultMessageExpirationLength;
            onlyEmbed = defaultOnlyEmbed;
            onlyEphemeral = defaultOnlyEphemeral;
            isActive = defaultIsActive;
            deferReplies = defaultDeferReplies;
            useRatelimits = defaultUseRatelimits;
            messagesExpire = defaultMessagesExpire;
        }

        public BotCommandOptions(
                @Nonnull String name,
                @Nonnull String description,
                boolean onlyEmbed,
                boolean onlyEphemeral
        ) {
            this.name = name;
            this.description = description;
            this.onlyEmbed = onlyEmbed;
            this.onlyEphemeral = onlyEphemeral;
            ratelimitLength = defaultRatelimitLength;
            messageExpirationLength = defaultMessageExpirationLength;
            isActive = defaultIsActive;
            deferReplies = defaultDeferReplies;
            useRatelimits = defaultUseRatelimits;
            messagesExpire = defaultMessagesExpire;
        }

        public BotCommandOptions(
                @Nonnull String name,
                @Nonnull String description,
                long ratelimitLength,
                long messageExpirationLength,
                boolean onlyEmbed,
                boolean onlyEphemeral,
                boolean isActive,
                boolean deferReplies,
                boolean useRatelimits,
                boolean messagesExpire
        ) {
            this.name = name;
            this.description = description;
            this.ratelimitLength = ratelimitLength;
            this.messageExpirationLength = messageExpirationLength;
            this.onlyEmbed = onlyEmbed;
            this.onlyEphemeral = onlyEphemeral;
            this.isActive = isActive;
            this.deferReplies = deferReplies;
            this.useRatelimits = useRatelimits;
            this.messagesExpire = messagesExpire;
        }
    }

    protected final HashMap<Long, Long> ratelimitMap = new HashMap<>();
    protected final String name;
    protected final String description;
    protected final long ratelimitLength;
    protected final long messageExpirationLength;
    protected final boolean onlyEmbed;
    protected final boolean onlyEphemeral;
    protected final boolean isActive;
    protected final boolean deferReplies;
    protected final boolean useRatelimits;
    protected final boolean messagesExpire;
    protected long commandId;

    public BotCommand() {
        throw new RuntimeException("Unsupported constructor");
    }

    public BotCommand(@Nonnull BotCommandOptions options) {
        name = options.name;
        description = options.description;
        ratelimitLength = options.ratelimitLength;
        messageExpirationLength = options.messageExpirationLength;
        onlyEmbed = options.onlyEmbed;
        onlyEphemeral = options.onlyEphemeral;
        isActive = options.isActive;
        deferReplies = options.deferReplies;
        useRatelimits = options.useRatelimits;
        messagesExpire = options.messagesExpire;
    }

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
            message.delete().queueAfter(command.getMessageExpirationLength(), TimeUnit.SECONDS,
                    (ignored) -> {},
                    (fail) -> {
                        throw new RuntimeException(fail);
                    }
            );
        }
    }

    @Nonnull
    private static MessageEmbed handleError(@Nonnull Exception e) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Command Failed To Execute");
        eb.setDescription("The command failed to execute due to: " + e.getClass().getSimpleName());
        if(e.getMessage() != null) {
            if(e.getMessage().length() > 1800) {
                eb.addField("Error Message", "Error too long...", false);
            } else {
                eb.addField("Error Message", e.getMessage(), false);
            }
        } else {
            eb.addField("Error Message", "Error message unable to be generated? Cause of error: " + e.getCause(), false);
        }
        eb.addField("Error Stack 0", e.getStackTrace()[0].toString(), false);

        return eb.build();
    }

    @Nonnull
    private static MessageEmbed inactiveCommandResponse() {
        return new EmbedBuilder().setDescription("This command is currently not active").build();
    }
}
