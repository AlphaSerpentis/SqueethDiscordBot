// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.ServerDataHandler;

import java.awt.*;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public abstract class BotCommand<T> {

    enum TypeOfEphemeral {
        DEFAULT,
        DYNAMIC
    }

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
        protected final TypeOfEphemeral typeOfEphemeral;
        protected final long defaultRatelimitLength = 0;
        protected final long defaultMessageExpirationLength = 0;
        protected final boolean defaultOnlyEmbed = false;
        protected final boolean defaultOnlyEphemeral = false;
        protected final boolean defaultIsActive = true;
        protected final boolean defaultDeferReplies = false;
        protected final boolean defaultUseRatelimits = false;
        protected final boolean defaultMessagesExpire = false;
        protected final TypeOfEphemeral defaultTypeOfEphemeral = TypeOfEphemeral.DEFAULT;

        public BotCommandOptions(
                @NonNull String name,
                @NonNull String description
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
            typeOfEphemeral = defaultTypeOfEphemeral;
        }

        public BotCommandOptions(
                @NonNull String name,
                @NonNull String description,
                boolean onlyEmbed,
                boolean onlyEphemeral,
                @NonNull TypeOfEphemeral typeOfEphemeral
        ) {
            this.name = name;
            this.description = description;
            this.onlyEmbed = onlyEmbed;
            this.onlyEphemeral = onlyEphemeral;
            this.typeOfEphemeral = typeOfEphemeral;
            ratelimitLength = defaultRatelimitLength;
            messageExpirationLength = defaultMessageExpirationLength;
            isActive = defaultIsActive;
            deferReplies = defaultDeferReplies;
            useRatelimits = defaultUseRatelimits;
            messagesExpire = defaultMessagesExpire;
        }

        public BotCommandOptions(
                @NonNull String name,
                @NonNull String description,
                boolean onlyEmbed,
                boolean onlyEphemeral,
                @NonNull TypeOfEphemeral typeOfEphemeral,
                boolean deferReplies
        ) {
            this.name = name;
            this.description = description;
            this.onlyEmbed = onlyEmbed;
            this.onlyEphemeral = onlyEphemeral;
            this.typeOfEphemeral = typeOfEphemeral;
            this.deferReplies = deferReplies;
            ratelimitLength = defaultRatelimitLength;
            messageExpirationLength = defaultMessageExpirationLength;
            isActive = defaultIsActive;
            useRatelimits = defaultUseRatelimits;
            messagesExpire = defaultMessagesExpire;
        }

        public BotCommandOptions(
                @NonNull String name,
                @NonNull String description,
                long ratelimitLength,
                long messageExpirationLength,
                boolean onlyEmbed,
                boolean onlyEphemeral,
                @NonNull TypeOfEphemeral typeOfEphemeral,
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
            this.typeOfEphemeral = typeOfEphemeral;
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
    protected final TypeOfEphemeral ephemeralType;
    protected long commandId;

    public BotCommand() {
        throw new UnsupportedOperationException("Unsupported constructor");
    }

    public BotCommand(@NonNull BotCommandOptions options) {
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
        ephemeralType = options.typeOfEphemeral;
    }

    @NonNull
    abstract public CommandResponse<T> runCommand(long userId, @NonNull SlashCommandInteractionEvent event);

    abstract public void updateCommand(@NonNull JDA jda);

    /**
     * A method that REQUIRES to be overridden if to be used for any BotCommand with an ephemeralType of TypeOfEphemeral.DYNAMIC.
     * This method is currently only called if the command is DEFERRED.
     * Operations inside must NOT exceed the time it requires to ACKNOWLEDGE the API!
     * @param userId is a long ID provided by Discord for the user calling the command
     * @param event is a SlashCommandInteractionEvent that contains the interaction
     * @return a nonnull CommandResponse containing either a MessageEmbed or Message
     */
    @NonNull
    public CommandResponse<T> beforeRunCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        throw new UnsupportedOperationException("beforeRunCommand needs to be overridden!");
    }

    /**
     * A method that checks and handles a potentially rate-limited user
     * Commands not using embeds must override this method to return a CommandResponse containing Message
     * @param userId is a long ID provided by Discord for the user calling the command
     * @return a nullable CommandResponse that by default returns a MessageEmbed
     */
    @Nullable
    public CommandResponse<?> checkAndHandleRateLimitedUser(long userId) {
        if(isUserRatelimited(userId)) {
            return new CommandResponse<>(
                    new EmbedBuilder().setDescription(
                            "You are still rate limited. Expires in " + (ratelimitMap.get(userId) - Instant.now().getEpochSecond()) + " seconds."
                    ).build(),
                    onlyEphemeral
            );
        } else {
            return null;
        }
    }

    public void setCommandId(long id) {
        commandId = id;
    }
    @NonNull
    public String getName() {
        return name;
    }
    @NonNull
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
    public TypeOfEphemeral getEphemeralType() {
        return ephemeralType;
    }

    @NonNull
    public static Message handleReply(@NonNull SlashCommandInteractionEvent event, @NonNull BotCommand<?> cmd) {
        boolean sendAsEphemeral = cmd.isOnlyEphemeral();
        CommandResponse<?> responseFromCommand;
        Object response;
        ReplyCallbackAction reply;

        if (cmd.isDeferReplies()) {
            InteractionHook hook = event.getHook();
            try {
                if (!sendAsEphemeral && event.getGuild() != null) {
                    sendAsEphemeral = ServerDataHandler.serverDataHashMap.get(event.getGuild().getIdLong()).isOnlyEphemeral();
                }

                if (cmd.isOnlyEmbed()) {
                    if(cmd.ephemeralType == TypeOfEphemeral.DEFAULT) {
                        if (!sendAsEphemeral && event.getGuild() != null) {
                            event.deferReply(false).complete();
                        } else {
                            event.deferReply(sendAsEphemeral).complete();
                        }
                    } else {
                        CommandResponse<?> responseBeforeRunning = cmd.beforeRunCommand(event.getUser().getIdLong(), event);

                        event.deferReply(responseBeforeRunning.messageIsEphemeral()).complete();
                        if(responseBeforeRunning.messageResponse() != null) {
                            MessageEmbed message = (MessageEmbed) responseBeforeRunning.messageResponse();

                            event.replyEmbeds(message).complete();
                        }
                    }

                    responseFromCommand = cmd.isActive() ? cmd.runCommand(event.getUser().getIdLong(), event) : inactiveCommandResponse();
                    response = responseFromCommand.messageResponse();

                    if (cmd instanceof ButtonCommand) {
                        Collection<ItemComponent> buttons = ((ButtonCommand<?>) cmd).addButtons(event);

                        if (cmd.isUsingRatelimits() && !cmd.isUserRatelimited(event.getUser().getIdLong())) {
                            cmd.ratelimitMap.put(event.getUser().getIdLong(), Instant.now().getEpochSecond() + cmd.getRatelimitLength());
                        }

                        if(!buttons.isEmpty())
                            return hook.sendMessageEmbeds((MessageEmbed) response).addActionRow(buttons).complete();
                    }

                    if (cmd.isUsingRatelimits() && !cmd.isUserRatelimited(event.getUser().getIdLong())) {
                        cmd.ratelimitMap.put(event.getUser().getIdLong(), Instant.now().getEpochSecond() + cmd.getRatelimitLength());
                    }

                    return hook.sendMessageEmbeds((MessageEmbed) response).setEphemeral(responseFromCommand.messageIsEphemeral()).complete();
                } else {
                    if(cmd.getEphemeralType() == TypeOfEphemeral.DEFAULT) {
                        if (!sendAsEphemeral && event.getGuild() != null) {
                            hook.setEphemeral(false);
                        } else {
                            hook.setEphemeral(sendAsEphemeral);
                        }
                    } else {
                        CommandResponse<?> responseBeforeRunning = cmd.beforeRunCommand(event.getUser().getIdLong(), event);

                        event.deferReply(responseBeforeRunning.messageIsEphemeral()).complete();
                        if(responseBeforeRunning.messageResponse() != null) {
                            event.reply((String) responseBeforeRunning.messageResponse()).complete();
                        }
                    }

                    responseFromCommand = cmd.isActive() ? cmd.runCommand(event.getUser().getIdLong(), event) : inactiveCommandResponse();
                    response = responseFromCommand.messageResponse();

                    return hook.sendMessage((String) response).complete();
                }
            } catch(Exception e) {
                return hook.sendMessageEmbeds(handleError(e)).complete();
            }
        }

        try {
            responseFromCommand = cmd.isActive() ? cmd.runCommand(event.getUser().getIdLong(), event) : inactiveCommandResponse();
            response = responseFromCommand.messageResponse();

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
                    reply = event.reply((String) response).setEphemeral(false);
                } else {
                    reply = event.reply((String) response).setEphemeral(sendAsEphemeral);
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

    protected static void letMessageExpire(@NonNull BotCommand<?> command, @NonNull Message message) {
        if(command.doMessagesExpire()) {
            message.delete().queueAfter(command.getMessageExpirationLength(), TimeUnit.SECONDS,
                    (ignored) -> {},
                    (fail) -> {
                        throw new RuntimeException(fail);
                    }
            );
        }
    }

    @NonNull
    protected static MessageEmbed handleError(@NonNull Exception e) {
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
        for(int i = 0; i < e.getStackTrace().length; i++) {
            eb.addField("Error Stack " + i, e.getStackTrace()[i].toString(), false);
        }
        eb.setColor(Color.RED);

        return eb.build();
    }

    @NonNull
    private static CommandResponse<MessageEmbed> inactiveCommandResponse() {
        return new CommandResponse<>(new EmbedBuilder().setDescription("This command is currently not active").build(), null);
    }
}
