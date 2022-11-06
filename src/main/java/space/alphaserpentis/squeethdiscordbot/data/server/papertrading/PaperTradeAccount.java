package space.alphaserpentis.squeethdiscordbot.data.server.papertrading;

import net.dv8tion.jda.api.EmbedBuilder;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.PositionsDataHandler;
import space.alphaserpentis.squeethdiscordbot.handler.games.PaperTradingHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class PaperTradeAccount {
    public HashMap<IPaperTrade.Asset, Double> balance = new HashMap<>();
    public ArrayList<FinalizedTrade> history = new ArrayList<>();
    public long lastBlock;

    public record FinalizedTrade(
        long block,
        @Nonnull IPaperTrade.Action action,
        @Nonnull IPaperTrade.Asset asset,
        double amount
    ) {}

    @Nonnull
    public EmbedBuilder trade(@Nonnull IPaperTrade.Action action, @Nonnull IPaperTrade.Asset asset, double amount, @Nonnull EmbedBuilder eb) {
        final HashMap<IPaperTrade.Asset, Double> originalBalance = balance;
        final ArrayList<FinalizedTrade> originalHistory = history;
        double assetUsdValue = assetPriceInUsd(asset);
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

        return eb
                .addField(action == IPaperTrade.Action.BUY ? "Bought" : "Sold", amount + " " + asset + " for $" + assetUsdValue * amount + "",false)
                .addField(asset + " Balance", balance.get(asset) + " " + asset, false)
                .addField("USD Balance", "$" + balance.get(IPaperTrade.Asset.USDC), false);
    }

    public void resetAccount() {
        balance = new HashMap<>() {{
            put(IPaperTrade.Asset.USDC, 10000.00);
            put(IPaperTrade.Asset.CRAB, 0.0);
            put(IPaperTrade.Asset.LONG_OSQTH, 0.0);
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

    public double assetPriceInUsd(@Nonnull IPaperTrade.Asset asset) {
        PriceData priceData = null;
        try {
            PriceData.Prices price = assetToPrices(asset);
            lastBlock = EthereumRPCHandler.web3.ethBlockNumber().send().getBlockNumber().longValue();
            if(price != null)
                priceData = PositionsDataHandler.getPriceData(
                        lastBlock,
                        new PriceData.Prices[]{price, PriceData.Prices.ETHUSD}
                );
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
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
            case LONG_OSQTH -> {
                return PriceData.convertToDouble(priceData.osqthEth, 18) * PriceData.convertToDouble(priceData.ethUsdc, 18);
            }
        }
        return -1;
    }

    public double balanceInUsd(@Nonnull IPaperTrade.Asset asset) {
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
            case LONG_OSQTH -> {
                return balance.get(asset) * PriceData.convertToDouble(priceData.osqthEth, 18) * PriceData.convertToDouble(priceData.ethUsdc, 18);
            }
        }
        return -1; // why java?
    }

    @Nullable
    public static PriceData.Prices assetToPrices(@Nonnull IPaperTrade.Asset asset) {
        switch(asset) {
            case ETH -> {
                return PriceData.Prices.ETHUSD;
            }
            case CRAB -> {
                return PriceData.Prices.CRABV2ETH;
            }
            case LONG_OSQTH -> {
                return PriceData.Prices.OSQTHETH;
            }
        }
        return null;
    }
}
