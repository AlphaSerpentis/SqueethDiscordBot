package space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.data;

import io.reactivex.annotations.NonNull;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.Condition;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.PositionsDataHandler;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class SqueethVolatility extends TrackedData<Double> {

    public SqueethVolatility() {
        name = "Squeeth Volatility";
    }

    @Override
    public void update() {
        try {
            PriceData priceData = PositionsDataHandler.getPriceData(
                    new PriceData.Prices[]{PriceData.Prices.SQUEETHVOL}
            );

            currentData = priceData.squeethVol;
        } catch (ExecutionException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean checkCondition(@NonNull Condition<Double> condition) {
        return switch (condition.typeOfCondition) {
            case GREATER_THAN -> currentData > condition.condition;
            case LESS_THAN -> currentData < condition.condition;
            case EQUALS -> currentData.equals(condition.condition);
            case NOT_EQUALS -> !currentData.equals(condition.condition);
        };
    }
}
