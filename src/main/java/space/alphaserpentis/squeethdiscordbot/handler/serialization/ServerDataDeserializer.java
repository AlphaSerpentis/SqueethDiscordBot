// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler.serialization;

import com.google.gson.*;
import space.alphaserpentis.squeethdiscordbot.data.ServerData;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ServerDataDeserializer implements JsonDeserializer<Map<Long, ServerData>> {

    @Override
    public Map<Long, ServerData> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Map<Long, ServerData> serverDataMap = new HashMap<>();

        Gson gson = new Gson();
        JsonObject object = JsonParser.parseString(jsonElement.getAsString()).getAsJsonObject();

        for(Map.Entry<String, JsonElement> entry: object.entrySet()) {
            serverDataMap.put(Long.valueOf(entry.getKey()), gson.fromJson(entry.getValue(), ServerData.class));
        }

        return serverDataMap;
    }
}
