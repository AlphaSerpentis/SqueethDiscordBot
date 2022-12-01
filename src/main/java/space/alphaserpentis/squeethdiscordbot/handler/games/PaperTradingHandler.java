package space.alphaserpentis.squeethdiscordbot.handler.games;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import space.alphaserpentis.squeethdiscordbot.data.server.papertrading.PaperTradeAccount;
import space.alphaserpentis.squeethdiscordbot.data.server.papertrading.ServerPaperTrades;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.PaperTradingDeserializer;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class PaperTradingHandler {
    public static Path paperTradingJson;
    private static HashMap<Long, ServerPaperTrades> paperTradingLeaderboard = new HashMap<>();

    public static void init(@NonNull Path paperTradingJson) throws IOException {
        PaperTradingHandler.paperTradingJson = paperTradingJson;

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(paperTradingLeaderboard.getClass(), new PaperTradingDeserializer())
                .create();

        paperTradingLeaderboard = gson.fromJson(Files.newBufferedReader(paperTradingJson), new TypeToken<HashMap<Long, ServerPaperTrades>>(){}.getType());

        if(paperTradingLeaderboard == null) paperTradingLeaderboard = new HashMap<>();
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
}
