package space.alphaserpentis.squeethdiscordbot.data.server.papertrading;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import net.dv8tion.jda.api.EmbedBuilder;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.PositionsDataHandler;
import space.alphaserpentis.squeethdiscordbot.handler.games.PaperTradingHandler;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class PaperTradeAccount {
    public HashMap<IPaperTrade.Asset, Double> balance = new HashMap<>();
    public ArrayList<FinalizedTrade> history = new ArrayList<>();
    public long lastBlock;

    public record FinalizedTrade(
        long block,
        @NonNull IPaperTrade.Action action,
        @NonNull IPaperTrade.Asset asset,
        double assetPriceInUsd,
        double amount
    ) {
        @Override
        public String toString() {
            long timestampFromBlock;
            try {
                timestampFromBlock = EthereumRPCHandler.web3.ethGetBlockByNumber(
                        new DefaultBlockParameterNumber(block),
                        true
                ).send().getBlock().getTimestamp().longValue();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return "On <t:" + timestampFromBlock + ">, you "
                    + (action.equals(IPaperTrade.Action.BUY) ? "bought " : "sold ") + amount + " " + asset;
        }
    }

    @NonNull
    public EmbedBuilder trade(@NonNull IPaperTrade.Action action, @NonNull IPaperTrade.Asset asset, double amount, @NonNull EmbedBuilder eb) {
        final HashMap<IPaperTrade.Asset, Double> originalBalance = balance;
        final ArrayList<FinalizedTrade> originalHistory = history;
        double assetUsdValue;
        PriceData priceData;

        try {
            PriceData.Prices price = assetToPrices(asset);
            lastBlock = EthereumRPCHandler.getLatestBlockNumber().longValue();
            if(price != null)
                priceData = PositionsDataHandler.getPriceData(
                        lastBlock,
                        new PriceData.Prices[]{price, PriceData.Prices.ETHUSD}
                );
            else
                throw new UnsupportedOperationException("assetToPrices returned null");
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        assetUsdValue = assetPriceInUsd(asset, priceData);
        
        // Check if they have the sufficient amount
        if (amount <= 0) {
            return eb.setDescription("Amount to buy/sell cannot be zero or less");
        }
        if(assetUsdValue == 0) {
            return eb.setDescription("Price could not be obtained! Report this issue to the bot owner!");
        }
        if(action == IPaperTrade.Action.BUY) {
            if(balance.get(IPaperTrade.Asset.USDC) - assetUsdValue * amount < 0) {
                return eb.setDescription("Insufficient funds to buy " + asset);
            }
        } else {
            if(balance.get(asset) - amount < 0) {
                return eb.setDescription("Insufficient funds to sell " + asset);
            }
        }

        // Execute the trade
        if(action == IPaperTrade.Action.SELL) {
            balance.put(asset, balance.get(asset) - amount);
            balance.put(IPaperTrade.Asset.USDC, balance.get(IPaperTrade.Asset.USDC) + (assetUsdValue * amount));
        } else {
            balance.put(IPaperTrade.Asset.USDC, balance.get(IPaperTrade.Asset.USDC) - (assetUsdValue * amount));
            balance.put(asset, balance.get(asset) + amount);
        }
        // Register the trade
        history.add(
                new FinalizedTrade(
                    lastBlock,
                    action,
                    asset,
                    assetUsdValue,
                    amount
                )
        );

        try {
            PaperTradingHandler.updateJson();
        } catch(IOException e) {
            balance = originalBalance;
            history = originalHistory;
            throw new RuntimeException(e);
        }

        NumberFormat instance = NumberFormat.getInstance();

        return eb
                .addField(
                        action == IPaperTrade.Action.BUY ? "Bought" : "Sold",
                        amount + " " + asset + " for $" + instance.format(assetUsdValue * amount),false
                )
                .addField(asset + " Balance", instance.format(balance.get(asset)) + " " + asset, false)
                .addField("USDC Balance", "$" + instance.format(balance.get(IPaperTrade.Asset.USDC)), false);
    }

    public void resetAccount() {
        balance = new HashMap<>() {{
            put(IPaperTrade.Asset.USDC, 10000.00);
            put(IPaperTrade.Asset.CRAB, 0.0);
            put(IPaperTrade.Asset.OSQTH, 0.0);
            put(IPaperTrade.Asset.ETH, 0.0);
        }};
        history.clear();
        lastBlock = 0;
    }

    public double portfolioValueInUsd() {
        double value = 0;

        for(IPaperTrade.Asset asset: balance.keySet()) {
            value += balanceInUsd(asset);
        }

        return value;
    }

    public static double assetPriceInUsd(@NonNull IPaperTrade.Asset asset, @NonNull PriceData priceData) {
        switch(asset) {
            case ETH -> {
                return PriceData.convertToDouble(priceData.ethUsdc, 18);
            }
            case CRAB -> {
                return PriceData.convertToDouble(priceData.crabV2Eth, 18) * PriceData.convertToDouble(priceData.ethUsdc, 18);
            }
            case USDC -> {
                return 1;
            }
            case OSQTH -> {
                return PriceData.convertToDouble(priceData.osqthEth, 18) * PriceData.convertToDouble(priceData.ethUsdc, 18);
            }
        }
        return -1;
    }
    
    public double balanceInUsd(@NonNull IPaperTrade.Asset asset) {
        PriceData priceData = null;
        try {
            PriceData.Prices price = assetToPrices(asset);
            if(price != null)
                priceData = PositionsDataHandler.getPriceData(
                        new PriceData.Prices[]{price, PriceData.Prices.ETHUSD}
                );
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }

        switch(asset) {
            case ETH -> {
                return balance.get(asset) * PriceData.convertToDouble(priceData.ethUsdc, 18);
            }
            case CRAB -> {
                return balance.get(asset) * PriceData.convertToDouble(priceData.crabV2Eth, 18) * PriceData.convertToDouble(priceData.ethUsdc, 18);
            }
            case USDC -> {
                return balance.get(asset);
            }
            case OSQTH -> {
                return balance.get(asset) * PriceData.convertToDouble(priceData.osqthEth, 18) * PriceData.convertToDouble(priceData.ethUsdc, 18);
            }
        }
        return -1; // why java?
    }

    @Nullable
    public static PriceData.Prices assetToPrices(@NonNull IPaperTrade.Asset asset) {
        switch(asset) {
            case ETH -> {
                return PriceData.Prices.ETHUSD;
            }
            case CRAB -> {
                return PriceData.Prices.CRABV2ETH;
            }
            case OSQTH -> {
                return PriceData.Prices.OSQTHETH;
            }
        }
        return null;
    }
}
