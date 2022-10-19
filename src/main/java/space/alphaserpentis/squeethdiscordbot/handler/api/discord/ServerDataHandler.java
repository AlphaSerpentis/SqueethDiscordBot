// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler.api.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import space.alphaserpentis.squeethdiscordbot.data.server.ServerData;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.ServerDataDeserializer;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class ServerDataHandler extends ListenerAdapter {

    public static Path serverJson;
    public static HashMap<Long, ServerData> serverDataHashMap = new HashMap<>();

    public static void init(@Nonnull Path jsonFile) throws IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(serverDataHashMap.getClass(), new ServerDataDeserializer())
                .create();
        serverJson = jsonFile;

        Reader reader = Files.newBufferedReader(serverJson);
        serverDataHashMap = gson.fromJson(reader, new TypeToken<HashMap<Long, ServerData>>(){}.getType());

        // Check the current servers
        if(serverDataHashMap == null)
            serverDataHashMap = new HashMap<>();

        ArrayList<Long> serversActuallyJoined = new ArrayList<>();

        for(Guild g: Launcher.api.getGuilds()) {
            if(!serverDataHashMap.containsKey(g.getIdLong())) {
                serverDataHashMap.put(g.getIdLong(), new ServerData());
            }

            serversActuallyJoined.add(g.getIdLong());
        }

        // Check if the bot left a server but data wasn't cleared
        serverDataHashMap.keySet().removeIf(id -> !serversActuallyJoined.contains(id));

        updateServerData();
    }

    public static void updateServerData() throws IOException {
        Gson gson = new Gson();

        writeToJSON(gson, gson.toJson(serverDataHashMap));
    }

    private static void writeToJSON(@Nonnull Gson gson, @Nonnull String json) throws IOException {
        Writer writer = Files.newBufferedWriter(Paths.get(String.valueOf(serverJson)));

        gson.toJson(json, writer);

        writer.close();
    }

    @Override
    public void onGuildJoin(@Nonnull GuildJoinEvent event) {
        serverDataHashMap.put(event.getGuild().getIdLong(), new ServerData());
        try {
            updateServerData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[ServerDataHandler] Adding " + event.getGuild().getIdLong() + " to JSON");
    }

    @Override
    public void onGuildLeave(@Nonnull  GuildLeaveEvent event) {
        serverDataHashMap.remove(event.getGuild().getIdLong());
        try {
            updateServerData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[ServerDataHandler] Removing " + event.getGuild().getIdLong() + " from the JSON");
    }

}
