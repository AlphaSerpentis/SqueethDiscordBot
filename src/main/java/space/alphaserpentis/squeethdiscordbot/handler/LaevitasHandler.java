package space.alphaserpentis.squeethdiscordbot.handler;

import com.google.gson.Gson;
import space.alphaserpentis.squeethdiscordbot.data.SqueethData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LaevitasHandler {

    public static URL API_URL;
    public static String KEY;
    public static SqueethData latestSqueethData = new SqueethData();

    public static void timedPoller() {
        new Thread(() -> {
            while(true) {
                try {
                    pollForData("analytics/defi/squeeth");
                    Thread.sleep(300000);
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public static void pollForData(String append) throws IOException {
        URL fullURL = new URL(API_URL.toString() + append);

        HttpURLConnection connection = (HttpURLConnection) fullURL.openConnection();
        connection.setRequestProperty("apiKey", KEY);
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();

        if(responseCode == HttpURLConnection.HTTP_ACCEPTED || responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();

            parseData(response.toString());

        } else {
            System.out.println("[LaevitasData] Connection failed, response code is " + responseCode);
        }
    }

    private static void parseData(String data) {
        Gson gson = new Gson();

        latestSqueethData = gson.fromJson(data, SqueethData.class);
    }

}
