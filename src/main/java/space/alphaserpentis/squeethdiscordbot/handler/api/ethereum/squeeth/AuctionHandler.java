package space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.squeeth;

import com.google.gson.Gson;
import io.reactivex.annotations.Nullable;
import space.alphaserpentis.squeethdiscordbot.data.api.squeethportal.Auction;
import space.alphaserpentis.squeethdiscordbot.data.api.squeethportal.GetAuctionByIdResponse;
import space.alphaserpentis.squeethdiscordbot.data.api.squeethportal.LatestCrabAuctionResponse;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class AuctionHandler {
    public static long getLatestActiveAuctionId() throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) new URL("https://squeethportal.xyz/api/auction/getLatestAuction").openConnection();
        con.setRequestMethod("GET");
        con.setInstanceFollowRedirects(true);
        con.setDoOutput(true);

        int responseCode = con.getResponseCode();

        if(responseCode == HttpURLConnection.HTTP_OK) {
            Gson gson = new Gson();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            LatestCrabAuctionResponse responseConverted;

            while((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();
            con.disconnect();

            responseConverted = gson.fromJson(String.valueOf(response), LatestCrabAuctionResponse.class);

            if(!responseConverted.isLive) return -1;

            return responseConverted.auction.currentAuctionId;
        } else {
            if(responseCode == 308) {
                URL newUrl = new URL(con.getHeaderField("Location"));
                con = (HttpsURLConnection) newUrl.openConnection();
                con.setDoOutput(true);

                Gson gson = new Gson();
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                LatestCrabAuctionResponse responseConverted;

                while((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();
                con.disconnect();

                responseConverted = gson.fromJson(String.valueOf(response), LatestCrabAuctionResponse.class);

                if(!responseConverted.isLive) return -1;

                return responseConverted.auction.currentAuctionId;
            } else {
                con.disconnect();
                return -1;
            }
        }
    }
    @Nullable
    public static LatestCrabAuctionResponse getLatestAuction() throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) new URL("https://squeethportal.xyz/api/auction/getLatestAuction").openConnection();
        con.setRequestMethod("GET");
        con.setInstanceFollowRedirects(true);
        con.setDoOutput(true);

        int responseCode = con.getResponseCode();

        if(responseCode == HttpURLConnection.HTTP_OK) {
            Gson gson = new Gson();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();
            con.disconnect();

            return gson.fromJson(String.valueOf(response), LatestCrabAuctionResponse.class);
        } else {
            if(responseCode == 308) {
                URL newUrl = new URL(con.getHeaderField("Location"));
                con = (HttpsURLConnection) newUrl.openConnection();
                con.setDoOutput(true);

                Gson gson = new Gson();
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();
                con.disconnect();

                return gson.fromJson(String.valueOf(response), LatestCrabAuctionResponse.class);
            } else {
                con.disconnect();
                return null;
            }
        }
    }

    @Nullable
    public static Auction getAuction(@Nullable Long id) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL("https://squeethportal.xyz/api/auction/getAuctionById?id=" + id).openConnection();
        con.setRequestMethod("GET");
        con.setInstanceFollowRedirects(true);
        con.setDoOutput(true);

        int responseCode = con.getResponseCode();

        if(responseCode == HttpURLConnection.HTTP_OK) {
            Gson gson = new Gson();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            GetAuctionByIdResponse responseConverted;

            while((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();
            con.disconnect();

            responseConverted = gson.fromJson(response.toString(), GetAuctionByIdResponse.class);

            return responseConverted.auction;
        } else {
            if(responseCode == 308) {
                URL newUrl = new URL(con.getHeaderField("Location"));
                con = (HttpsURLConnection) newUrl.openConnection();
                con.setDoOutput(true);

                Gson gson = new Gson();
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                GetAuctionByIdResponse responseConverted;

                while((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();
                con.disconnect();

                responseConverted = gson.fromJson(response.toString(), GetAuctionByIdResponse.class);

                return responseConverted.auction;
            } else {
                con.disconnect();
                return null;
            }
        }
    }
}
