// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler.api.ethereum;

import com.google.gson.Gson;
import io.reactivex.annotations.NonNull;
import space.alphaserpentis.squeethdiscordbot.data.api.SqueethData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LaevitasHandler {

    public static URL API_URL;
    public static String KEY;
    public static SqueethData latestSqueethData = new SqueethData();
    public static long lastSuccessfulPoll = 0;
    public static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public static void timedPoller() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                if(pollForData("analytics/defi/squeeth"))
                    lastSuccessfulPoll = Instant.now().getEpochSecond();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    public static boolean pollForData(@NonNull String append) throws IOException {
        URL fullURL = new URL(API_URL.toString() + append);

        HttpURLConnection connection = (HttpURLConnection) fullURL.openConnection();
        connection.setRequestProperty("apiKey", KEY);
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();

        if(responseCode == HttpURLConnection.HTTP_ACCEPTED || responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();

            parseData(response.toString());

        } else {
            System.out.println("[LaevitasData] Connection failed, response code is " + responseCode);
            return false;
        }

        return true;
    }

    public static boolean isDataStale() {
        return Instant.now().getEpochSecond() - lastSuccessfulPoll > 600;
    }

    private static void parseData(@NonNull String data) {
        Gson gson = new Gson();

        latestSqueethData = gson.fromJson(data, SqueethData.class);
    }

}
