package space.alphaserpentis.squeethdiscordbot.handler.serialization;

import com.google.gson.*;
import space.alphaserpentis.squeethdiscordbot.data.PriceData;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class PriceDataDeserializer implements JsonDeserializer<Map<Long, PriceData>> {

    @Override
    public Map<Long, PriceData> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Map<Long, PriceData> priceDataMap = new HashMap<>();

        Gson gson = new Gson();
        JsonObject object = JsonParser.parseString(jsonElement.getAsString()).getAsJsonObject();

        for(Map.Entry<String, JsonElement> entry: object.entrySet()) {
            priceDataMap.put(Long.valueOf(entry.getKey()), gson.fromJson(entry.getValue(), PriceData.class));
        }

        return priceDataMap;
    }
}
