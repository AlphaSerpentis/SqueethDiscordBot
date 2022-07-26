// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.data.api.alchemy.SimpleTokenTransferResponse;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.PositionsDataDeserializer;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.PriceDataDeserializer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PositionsDataHandler {

    public static Map<String, ArrayList<SimpleTokenTransferResponse>> cachedTransfers = new HashMap<>();
    public static Map<Long, PriceData> cachedPrices = new HashMap<>();
    public static Path cachedTransfersPath;
    public static Path cachedPricesPath;

    public static void init(@Nonnull Path transfersJson, @Nonnull Path pricesJson) throws IOException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(cachedTransfers.getClass(), new PositionsDataDeserializer())
                .create();
        Gson priceGson = new GsonBuilder()
                .registerTypeAdapter(cachedPrices.getClass(), new PriceDataDeserializer())
                .create();
        cachedTransfersPath = transfersJson;
        cachedPricesPath = pricesJson;

        Reader reader = Files.newBufferedReader(cachedTransfersPath);
        Reader priceReader = Files.newBufferedReader(cachedPricesPath);
        cachedTransfers = gson.fromJson(reader, new TypeToken<Map<String, ArrayList<SimpleTokenTransferResponse>>>(){}.getType());
        cachedPrices = priceGson.fromJson(priceReader, new TypeToken<Map<Long, PriceData>>(){}.getType());
        if(cachedTransfers == null)
            cachedTransfers = new HashMap<>();
        if(cachedPrices == null)
            cachedPrices = new HashMap<>();
    }

    public static void addNewData(@Nonnull String address, @Nonnull ArrayList<SimpleTokenTransferResponse> data) {
        if(cachedTransfers.containsKey(address)) {
            for(SimpleTokenTransferResponse transfer: data) {
                if(!cachedTransfers.get(address).contains(transfer)) {
                    cachedTransfers.get(address).add(transfer);
                }
            }
        } else {
            ArrayList<SimpleTokenTransferResponse> newList = new ArrayList<>(data);
            cachedTransfers.put(address, newList);
        }

        if(data.isEmpty()) {
            cachedTransfers.remove(address);
        }

        try {
            writeDataToFile(cachedTransfers, cachedTransfersPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void addNewData(@Nonnull Long block, @Nonnull PriceData data) {
        cachedPrices.put(block, data);

        try {
            writeDataToFile(cachedPrices, cachedPricesPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeDataToFile(@Nonnull Object data, @Nonnull Path path) throws IOException {
        Writer writer = Files.newBufferedWriter(path);

        new Gson().toJson(data, writer);

        writer.close();
    }

}
