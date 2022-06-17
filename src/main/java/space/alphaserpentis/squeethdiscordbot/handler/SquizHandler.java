// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import space.alphaserpentis.squeethdiscordbot.data.SquizLeaderboard;
import space.alphaserpentis.squeethdiscordbot.data.SquizQuestions;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.SquizLeaderboardDeserializer;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.SquizQuestionsDeserializer;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class SquizHandler {

    public static Path squizLeaderboardJson;
    public static Path squizQuestionsJson;
    /**
     * Key: serverId
     * Value: SquizLeaderboard
     */
    public static HashMap<Long, SquizLeaderboard> squizLeaderboardHashMap = new HashMap<>();
    public static ArrayList<SquizQuestions> squizQuestions = new ArrayList<>();

    public static void init(Path squizLeaderboardJson, Path squizQuestionsJson) throws IOException {
        SquizHandler.squizLeaderboardJson = squizLeaderboardJson;
        SquizHandler.squizQuestionsJson = squizQuestionsJson;

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(squizLeaderboardHashMap.getClass(), new SquizLeaderboardDeserializer())
                .create();

        squizLeaderboardHashMap = gson.fromJson(Files.newBufferedReader(squizLeaderboardJson), new TypeToken<HashMap<Long, SquizLeaderboard>>(){}.getType());

        gson = new GsonBuilder()
                .registerTypeAdapter(squizQuestions.getClass(), new SquizQuestionsDeserializer())
                .create();

        squizQuestions = gson.fromJson(Files.newBufferedReader(squizQuestionsJson), new TypeToken<ArrayList<SquizQuestions>>(){}.getType());
    }

    public static void updateSquizLeaderboard() throws IOException {
        Gson gson = new Gson();

        writeToJSON(gson, gson.toJson(squizLeaderboardHashMap));
    }

    public static void writeToJSON(Gson gson, String json) throws IOException {
        Path path = Paths.get(squizLeaderboardJson.toString());
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(json, writer);
        }
    }

}
