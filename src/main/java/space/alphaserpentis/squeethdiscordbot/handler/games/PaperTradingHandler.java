package space.alphaserpentis.squeethdiscordbot.handler.games;

import space.alphaserpentis.squeethdiscordbot.data.server.papertrading.PaperTradingLeaderboard;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.HashMap;

public class PaperTradingHandler {
    public static Path paperTradingJson;
    public static HashMap<Long, PaperTradingLeaderboard> paperTradingLeaderboard = new HashMap<>();

    public static void init(@Nonnull Path paperTradingJson) {
        PaperTradingHandler.paperTradingJson = paperTradingJson;

    }

}
