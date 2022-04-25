package space.alphaserpentis.handler;

import com.google.gson.Gson;
import space.alphaserpentis.data.SqueethData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class LaevitasHandler {

    public static URL API_URL;
    public static String KEY;
    public static SqueethData latestSqueethData = new SqueethData();

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

        System.out.println(data);

//        List<SqueethData> squeethData = Arrays.asList(gson.fromJson(data, SqueethData[].class));
        SqueethData squeethData = gson.fromJson(data, SqueethData.class);
        latestSqueethData = squeethData;

        // Safety net
//        if(squeethData.isEmpty())
//            return;

        // Check if this is the latest data
//        if(squeethData.get(squeethData.size() - 1).getDate() > latestSqueethData.getDate())
//            latestSqueethData = squeethData.get(squeethData.size() - 1);

//        System.out.println(squeethData.get(squeethData.size() - 1).getDate());
//        System.out.println(new Date(latestSqueethData.getDate()));
    }

}
