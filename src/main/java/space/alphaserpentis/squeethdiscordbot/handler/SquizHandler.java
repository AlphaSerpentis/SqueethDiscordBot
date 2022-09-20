// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.entities.TextChannel;
import space.alphaserpentis.squeethdiscordbot.commands.Squiz;
import space.alphaserpentis.squeethdiscordbot.data.server.ServerData;
import space.alphaserpentis.squeethdiscordbot.data.server.squiz.SquizLeaderboard;
import space.alphaserpentis.squeethdiscordbot.data.server.squiz.SquizQuestions;
import space.alphaserpentis.squeethdiscordbot.data.server.squiz.SquizTracking;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.SquizLeaderboardDeserializer;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.SquizQuestionsDeserializer;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class SquizHandler {

    public static Path squizLeaderboardJson;
    public static Path squizQuestionsJson;
    public static Path squizTrackingJson;
    public static String pastebinApiKey;
    /**
     * Key: serverId
     * Value: SquizLeaderboard
     */
    public static Map<Long, SquizLeaderboard> squizLeaderboardHashMap = new HashMap<>();
    public static SquizTracking squizTracking = new SquizTracking();
    public static volatile HashMap<Long, ScheduledFuture<?>> runningRandomSquiz = new HashMap<>();
    public static ArrayList<SquizQuestions> squizQuestions = new ArrayList<>();
    private static final ScheduledThreadPoolExecutor scheduledExecutor;

    static {
        // count how many servers want to use squiz
        int eligibleServers = 0;
        for(long serverId: ServerDataHandler.serverDataHashMap.keySet()) {
            if(ServerDataHandler.serverDataHashMap.get(serverId).doRandomSquizQuestions()) {
                eligibleServers++;
            }
        }

        scheduledExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(
                eligibleServers + 1, Executors.defaultThreadFactory()
        );
    }

    public static void init(@Nonnull Path squizLeaderboardJson, @Nonnull Path squizQuestionsJson, @Nonnull Path squizTrackingJson) throws IOException {
        SquizHandler.squizLeaderboardJson = squizLeaderboardJson;
        SquizHandler.squizQuestionsJson = squizQuestionsJson;
        SquizHandler.squizTrackingJson = squizTrackingJson;

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
                if(isServerValidForRandomSquiz(serverId))
                    scheduleServer(serverId);
            }
        }

        squizTracking = new Gson().fromJson(Files.newBufferedReader(squizTrackingJson), new TypeToken<SquizTracking>(){}.getType());

        if(squizTracking == null) squizTracking = new SquizTracking();
    }

    public static void init(@Nonnull Path squizLeaderboardJson, @Nonnull Path squizQuestionsJson) throws IOException {
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
                if(isServerValidForRandomSquiz(serverId))
                    scheduleServer(serverId);
            }
        }
    }

    /**
     * Schedules the server for a new random Squiz to appear
     * @param id The server ID
     */
    public static void scheduleServer(long id) {
        ServerData sd = ServerDataHandler.serverDataHashMap.get(id);
        long time = (long) (sd.getRandomSquizBaseIntervals() + (sd.getRandomSquizBaseIntervals() * Math.random()));

        runningRandomSquiz.put(id, scheduledExecutor.schedule(() -> pushRandomSquiz(id), time, TimeUnit.SECONDS));
    }

    /**
     * Attempts to cancel the scheduled future. If successful, it will remove it from {@link #runningRandomSquiz runningRandomSquiz}
     * @param id The server ID
     * @return true if successfully cancelled (or if it wasn't running in the first place), otherwise false
     */
    public static boolean stopServerFromIssuingNewSquiz(long id) {
        if(!runningRandomSquiz.containsKey(id))
            return true;
        if(runningRandomSquiz.get(id).cancel(false)) {
            runningRandomSquiz.remove(id);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Attempts to restart the Squiz for a server. If unsuccessful, nothing happens
     * @param id The server ID
     */
    public static void restartServerSquiz(long id) {
        if(stopServerFromIssuingNewSquiz(id))
            scheduleServer(id);
    }

    /**
     * Checks that the server is eligible for random Squiz
     * @param serverId The server ID
     * @return true if eligible, otherwise false
     */
    public static boolean isServerValidForRandomSquiz(long serverId) {
        ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);

        if(sd.getLeaderboardChannelId() == 0) {
            return false;
        } else {
            TextChannel channel = Launcher.api.getTextChannelById(sd.getLeaderboardChannelId());

            if(channel == null || !channel.canTalk()) return false;
        }

        // Check if the random Squiz channels can messages be sent
        for(long channeId: sd.getRandomSquizQuestionsChannels()) {
            TextChannel channel = Launcher.api.getTextChannelById(channeId);

            if(channel == null || !channel.canTalk()) return false;
        }
        return true;
    }

    /**
     * Pushes a new random Squiz provided that it passes the invalid state check (e.g., leaderboard channel id or eligible channel size is 0)
     * @param id The server ID
     */
    private static void pushRandomSquiz(long id) {
        ServerData sd = ServerDataHandler.serverDataHashMap.get(id);

        if(sd.doRandomSquizQuestions()) {
            if(sd.getRandomSquizQuestionsChannels().size() == 0 || sd.getLeaderboardChannelId() == 0) { // invalid state to run random squiz questions
                runningRandomSquiz.remove(id);
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
        }
    }

    public static void updateSquizLeaderboard() throws IOException {
        Gson gson = new Gson();

        writeToJSON(gson, squizLeaderboardHashMap);
    }

    public static void updateSquizTracking() throws IOException {
        Gson gson = new Gson();

        writeToJSON(squizTrackingJson, gson, squizTracking);
    }

    public static void writeToJSON(@Nonnull Gson gson, @Nonnull Object data) throws IOException {
        Path path = Paths.get(squizLeaderboardJson.toString());
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(data, writer);
        }
    }

    public static void writeToJSON(@Nonnull Path path, @Nonnull Gson gson, @Nonnull Object data) throws IOException {
        try(Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(data, writer);
        }
    }

}
