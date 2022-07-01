// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler.serialization;

import com.google.gson.*;
import space.alphaserpentis.squeethdiscordbot.data.api.alchemy.SimpleTokenTransferResponse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PositionsDataDeserializer implements JsonDeserializer<Map<String, ArrayList<SimpleTokenTransferResponse>>> {
    @Override
    public Map<String, ArrayList<SimpleTokenTransferResponse>> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Map<String, ArrayList<SimpleTokenTransferResponse>> positionsDataMap = new HashMap<>();

        Gson gson = new Gson();
        JsonObject object = jsonElement.getAsJsonObject();

        for(Map.Entry<String, JsonElement> entry: object.entrySet()) {
            positionsDataMap.put(String.valueOf(entry.getKey()), gson.fromJson(entry.getValue(), new ArrayList<SimpleTokenTransferResponse>().getClass()));
        }

        return positionsDataMap;
    }
}
