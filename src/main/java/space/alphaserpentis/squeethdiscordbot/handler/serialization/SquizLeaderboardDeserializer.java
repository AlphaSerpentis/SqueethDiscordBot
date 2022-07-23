// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler.serialization;

import com.google.gson.*;
import space.alphaserpentis.squeethdiscordbot.data.server.squiz.SquizLeaderboard;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class SquizLeaderboardDeserializer implements JsonDeserializer<Map<Long, SquizLeaderboard>> {
    @Override
    public Map<Long, SquizLeaderboard> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Map<Long, SquizLeaderboard> squizLeaderboardMap = new HashMap<>();

        Gson gson = new Gson();
        JsonObject object = JsonParser.parseString(jsonElement.toString()).getAsJsonObject();

        for(Map.Entry<String, JsonElement> entry: object.entrySet()) {
            squizLeaderboardMap.put(Long.valueOf(entry.getKey()), gson.fromJson(entry.getValue(), SquizLeaderboard.class));
        }

        return squizLeaderboardMap;
    }
}
