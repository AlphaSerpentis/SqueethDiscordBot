package space.alphaserpentis.squeethdiscordbot.handler.serialization;

import com.google.gson.*;
import space.alphaserpentis.squeethdiscordbot.data.server.papertrading.ServerPaperTrades;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class PaperTradingDeserializer implements JsonDeserializer<HashMap<Long, ServerPaperTrades>> {

    @Override
    public HashMap<Long, ServerPaperTrades> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        HashMap<Long, ServerPaperTrades> paperTradingMap = new HashMap<>();

        Gson gson = new Gson();
        JsonObject object = jsonElement.getAsJsonObject();

        for(Map.Entry<String, JsonElement> entry: object.entrySet()) {
            paperTradingMap.put(Long.valueOf(String.valueOf(entry.getKey())), gson.fromJson(entry.getValue(), ServerPaperTrades.class));
        }

        return paperTradingMap;
    }
}
