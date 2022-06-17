// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler.serialization;

import com.google.gson.*;
import space.alphaserpentis.squeethdiscordbot.data.SquizQuestions;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

public class SquizQuestionsDeserializer implements JsonDeserializer<ArrayList<SquizQuestions>> {
    @Override
    public ArrayList<SquizQuestions> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        ArrayList<SquizQuestions> squizQuestionsArrayList = new ArrayList<>();

        Gson gson = new Gson();
        JsonObject object = jsonElement.getAsJsonObject();

        for(Map.Entry<String, JsonElement> entry: object.entrySet()) {
            squizQuestionsArrayList.add(gson.fromJson(entry.getValue(), new SquizQuestions().getClass()));
        }

        return squizQuestionsArrayList;
    }
}
