package space.alphaserpentis.squeethdiscordbot.handler;

import space.alphaserpentis.squeethdiscordbot.data.SimpleTokenTransferResponse;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

public class PositionsDataHandler {

    public static HashMap<Long, ArrayList<SimpleTokenTransferResponse>> cachedTransfers = new HashMap<>();
    public static Path cachedTransfersPath;

    public static void init() {

    }

    public static void addNewData(SimpleTokenTransferResponse data) {

    }

    public static void writeDataToFile() {

    }

}
