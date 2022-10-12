package space.alphaserpentis.squeethdiscordbot.data.server.papertrading;

import java.util.ArrayList;
import java.util.HashMap;

public class PaperTradeAccount {
    public HashMap<PaperTrade.Asset, Double> balance = new HashMap<>();
    public ArrayList<PaperTrade> history = new ArrayList<>();

    public void trade(PaperTrade.Asset asset, PaperTrade.Action action, double amount) {

    }

    public void resetAccount() {
        balance = new HashMap<>() {{
            put(PaperTrade.Asset.USDC, 10000.00);
        }};
    }

    public double calculatePnl() {
        double pnl = 0;

        for(PaperTrade trade: history) {

        }

        return pnl;
    }
}
