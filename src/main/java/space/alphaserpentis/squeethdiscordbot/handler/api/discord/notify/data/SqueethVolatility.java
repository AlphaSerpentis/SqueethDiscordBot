package space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.data;

import io.reactivex.annotations.NonNull;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.Condition;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.PositionsDataHandler;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class SqueethVolatility extends TrackedData<Double> {

    public SqueethVolatility() {
        super("Squeeth Volatility", "SqueethVol", "%");
    }

    @Override
    public void update() {
        try {
            PriceData priceData = PositionsDataHandler.getPriceData(
                    new PriceData.Prices[]{PriceData.Prices.SQUEETHVOL}
            );

            currentData = priceData.squeethVol * 100;
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean checkCondition(@NonNull Condition<Double> condition) {
        return switch (condition.comparisonOperator) {
            case GREATER_THAN -> currentData > condition.value;
            case LESS_THAN -> currentData < condition.value;
            case EQUALS -> currentData.equals(condition.value);
            case NOT_EQUALS -> !currentData.equals(condition.value);
        };
    }
}
