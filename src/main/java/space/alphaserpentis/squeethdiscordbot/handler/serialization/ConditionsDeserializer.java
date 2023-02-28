package space.alphaserpentis.squeethdiscordbot.handler.serialization;

import com.google.gson.*;
import space.alphaserpentis.squeethdiscordbot.data.bot.ConditionsCarrier;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ConditionsDeserializer implements JsonDeserializer<Map<String, ConditionsCarrier>> {
    @Override
    public Map<String, ConditionsCarrier> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Map<String, ConditionsCarrier> conditions = new HashMap<>();
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            conditions.put(key, jsonDeserializationContext.deserialize(value, ConditionsCarrier.class));
        }
        return conditions;
    }
}
