package space.alphaserpentis.squeethdiscordbot.handler.games;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.reactivex.annotations.CheckReturnValue;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import space.alphaserpentis.squeethdiscordbot.data.server.ServerData;
import space.alphaserpentis.squeethdiscordbot.data.server.papertrading.PaperTradeAccount;
import space.alphaserpentis.squeethdiscordbot.data.server.papertrading.ServerPaperTrades;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.servers.ServerDataHandler;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.PaperTradingDeserializer;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PaperTradingHandler {
    public static Path paperTradingJson;
    private static HashMap<Long, ServerPaperTrades> paperTradingLeaderboard = new HashMap<>();
    private static HashMap<Long, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
    private static ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    public static void init(@NonNull Path paperTradingJson) throws IOException {
        PaperTradingHandler.paperTradingJson = paperTradingJson;

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(paperTradingLeaderboard.getClass(), new PaperTradingDeserializer())
                .create();

        paperTradingLeaderboard = gson.fromJson(Files.newBufferedReader(paperTradingJson), new TypeToken<HashMap<Long, ServerPaperTrades>>(){}.getType());

        if(paperTradingLeaderboard == null) paperTradingLeaderboard = new HashMap<>();

        startUpdateChecks();
    }

    /**
     * Called at {@link #init init} to start loading up eligible servers
     */
    public static void startUpdateChecks() {
        scheduledExecutor.execute(() -> {
            for(long serverId: ServerDataHandler.serverDataHashMap.keySet()) {
                if(ServerDataHandler.serverDataHashMap.get(serverId).isDoPaperTrading()) {
                    scheduleServer(serverId);
                }
            }
        });
    }

    public static void scheduleServer(long serverId) {
        scheduledTasks.put(serverId, scheduledExecutor.scheduleAtFixedRate(() -> updateLeaderboard(serverId), 10, 10, TimeUnit.MINUTES));
    }

    /**
     * Tries to stop a task for a server
     * @param serverId The server ID
     */
    public static void removeTask(long serverId) {

    }

    public static void updateLeaderboard(long serverId) {
        ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);
        long messageId = sd.getLastPaperTradingLeaderboardMessageId();

        // Check if it has a message id
        if(messageId != 0) {

        } else {

        }
    }

    /**
     * Opens (or resets) an account for a user under a server
     * @param serverId Discord ID of the server to register the user
     * @param userId Discord ID of the user
     * @return an IOException if failed, otherwise null
     */
    @Nullable
    public static IOException openNewAccount(long serverId, long userId) {
        ServerPaperTrades serverPaperTrades = paperTradingLeaderboard.getOrDefault(serverId, new ServerPaperTrades());
        PaperTradeAccount account = new PaperTradeAccount();

        serverPaperTrades.paperTradeAccounts.put(userId, account);
        account.resetAccount();
        paperTradingLeaderboard.put(serverId, serverPaperTrades);
        try {
            updateJson();
            return null;
        } catch(IOException e) {
            return e;
        }
    }

    public static void updateJson() throws IOException {
        try(Writer writer = Files.newBufferedWriter(paperTradingJson)) {
            new Gson().toJson(paperTradingLeaderboard, writer);
        }
    }

    @Nullable
    public static PaperTradeAccount getAccount(long serverId, long userId) {
        if(!paperTradingLeaderboard.containsKey(serverId)) return null;

        return paperTradingLeaderboard.get(serverId).paperTradeAccounts.get(userId);
    }

    /**
     * Creates a new leaderboard message. If passed a valid ID, the method will edit the message instead, otherwise sends a new message.
     * @param messageId A nullable message ID of the leaderboard message to edit if applicable
     * @return A message ID
     */
    @CheckReturnValue
    private static long updateLeaderboardMessage(long channelId, @Nullable Long messageId) throws IllegalArgumentException {
        TextChannel channel = Launcher.api.getTextChannelById(channelId);

        if(channel == null) {
            throw new IllegalArgumentException("Channel ID used does not return a valid channel (other errors possible)!");
        }

        if(messageId == null) {

        } else {
            try {
                channel.editMessageEmbedsById(
                        messageId,
                        generateLeaderboardMessage(channel.getGuild().getIdLong()).build()
                ).complete();
            } catch(InsufficientPermissionException e) {
                // TODO: Add a feature that sends warnings/errors to a log channel
            }

            return messageId;
        }

        return 0;
    }

    private static EmbedBuilder generateLeaderboardMessage(long serverId) {
        EmbedBuilder eb = new EmbedBuilder();

        return eb;
    }
}
