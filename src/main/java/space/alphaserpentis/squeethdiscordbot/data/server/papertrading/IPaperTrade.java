package space.alphaserpentis.squeethdiscordbot.data.server.papertrading;

public interface IPaperTrade {
    enum Action {
        BUY,
        SELL
    }

    enum Asset {
        ETH,
        USDC,
        OSQTH,
        CRAB
    }
}
