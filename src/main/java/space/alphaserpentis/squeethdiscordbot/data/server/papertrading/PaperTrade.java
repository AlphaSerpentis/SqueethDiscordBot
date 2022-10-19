package space.alphaserpentis.squeethdiscordbot.data.server.papertrading;

public class PaperTrade {
    enum Action {
        BUY,
        SELL
    }

    enum Asset {
        ETH,
        USDC,
        LONG_OSQTH,
        CRAB
    }

    public long block;
    public Action action;
    public Asset asset;
    public double amount;
}
