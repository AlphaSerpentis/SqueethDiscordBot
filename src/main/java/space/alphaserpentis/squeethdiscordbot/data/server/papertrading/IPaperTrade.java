package space.alphaserpentis.squeethdiscordbot.data.server.papertrading;

public interface IPaperTrade {
    enum Action {
        BUY,
        SELL
    }

    enum Asset {
        ETH ("ETH"),
        USDC ("USDC"),
        OSQTH ("oSQTH"),
        CRAB ("Crab");

        private final String properName;

        Asset(String properName) {
            this.properName = properName;
        }

        @Override
        public String toString() {
            return properName;
        }
    }
}
