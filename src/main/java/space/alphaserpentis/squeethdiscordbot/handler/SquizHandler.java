// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import space.alphaserpentis.squeethdiscordbot.commands.Squiz;
import space.alphaserpentis.squeethdiscordbot.data.server.ServerData;
import space.alphaserpentis.squeethdiscordbot.data.server.squiz.SquizLeaderboard;
import space.alphaserpentis.squeethdiscordbot.data.server.squiz.SquizQuestions;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.SquizLeaderboardDeserializer;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.SquizQuestionsDeserializer;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SquizHandler {

    public static Path squizLeaderboardJson;
    public static Path squizQuestionsJson;
    /**
     * Key: serverId
     * Value: SquizLeaderboard
     */
    public static Map<Long, SquizLeaderboard> squizLeaderboardHashMap = new HashMap<>();
    public static volatile HashMap<Long, Thread> runningRandomSquiz = new HashMap<>();
    public static ArrayList<SquizQuestions> squizQuestions = new ArrayList<>();

    public static void startThreadForServerSquiz(long id) {
        Thread t = new Thread(() -> {
            ServerData sd = ServerDataHandler.serverDataHashMap.get(id);

            while(sd.doRandomSquizQuestions()) {
                try {
                    Thread.sleep((long) ((sd.getRandomSquizBaseIntervals() + (sd.getRandomSquizBaseIntervals() * Math.random())) * 1000));

                    if(sd.doRandomSquizQuestions()) {
                        if(sd.getRandomSquizQuestionsChannels().size() == 0 || sd.getLeaderboardChannelId() == 0) { // invalid state to run random squiz questions
                            runningRandomSquiz.remove(id);
                            break;
                        } else {
                            Squiz squiz = (Squiz) CommandsHandler.mappingOfCommands.get("squiz");

                            if(!Squiz.randomSquizSessionsHashMap.containsKey(id)) {
                                squiz.setRandomSquizQuestionSession(id);
                                squiz.sendRandomSquizQuestion(id);
                            }
                        }
                    } else {
                        runningRandomSquiz.remove(id);
                        Squiz.randomSquizSessionsHashMap.remove(id);
                        return;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        t.start();

        runningRandomSquiz.put(id, t);
    }

    public static void init(Path squizLeaderboardJson, Path squizQuestionsJson) throws IOException {
        SquizHandler.squizLeaderboardJson = squizLeaderboardJson;
        SquizHandler.squizQuestionsJson = squizQuestionsJson;

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(squizLeaderboardHashMap.getClass(), new SquizLeaderboardDeserializer())
                .create();

        squizLeaderboardHashMap = gson.fromJson(Files.newBufferedReader(squizLeaderboardJson), new TypeToken<Map<Long, SquizLeaderboard>>(){}.getType());

        if(squizLeaderboardHashMap == null) squizLeaderboardHashMap = new HashMap<>();

        gson = new GsonBuilder()
                .registerTypeAdapter(squizQuestions.getClass(), new SquizQuestionsDeserializer())
                .create();

        squizQuestions = gson.fromJson(Files.newBufferedReader(squizQuestionsJson), new TypeToken<ArrayList<SquizQuestions>>(){}.getType());

        for(Long serverId: ServerDataHandler.serverDataHashMap.keySet()) {
            if(ServerDataHandler.serverDataHashMap.get(serverId).doRandomSquizQuestions()) {
                startThreadForServerSquiz(serverId);
            }
        }
    }

    public static void updateSquizLeaderboard() throws IOException {
        Gson gson = new Gson();

        writeToJSON(gson, squizLeaderboardHashMap);
    }

    public static void writeToJSON(Gson gson, Object data) throws IOException {
        Path path = Paths.get(squizLeaderboardJson.toString());
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(data, writer);
        }
    }

}
