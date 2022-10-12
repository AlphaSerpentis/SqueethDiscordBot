package space.alphaserpentis.squeethdiscordbot.handler.games;

import space.alphaserpentis.squeethdiscordbot.data.server.papertrading.PaperTradingLeaderboard;

import java.nio.file.Path;
import java.util.HashMap;

public class PaperTradingHandler {
    public static Path paperTradingJson;
    public static HashMap<Long, PaperTradingLeaderboard> paperTradingLeaderboard = new HashMap<>();
}
