package space.alphaserpentis.squeethdiscordbot.handler;

import com.google.gson.Gson;
import space.alphaserpentis.squeethdiscordbot.data.SquizLeaderboard;
import space.alphaserpentis.squeethdiscordbot.data.SquizQuestions;

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
    public static HashMap<Long, SquizLeaderboard> squizLeaderboardHashMap = new HashMap<>();
    public static ArrayList<SquizQuestions> squizQuestions = new ArrayList<>();

    public static void init(Path squizLeaderboardJson, Path squizQuestionsJson) {
        SquizHandler.squizLeaderboardJson = squizLeaderboardJson;
        SquizHandler.squizQuestionsJson = squizQuestionsJson;
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
