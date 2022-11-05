// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler.api.ethereum;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.web3j.abi.datatypes.Type;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.data.api.alchemy.SimpleTokenTransferResponse;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.PositionsDataDeserializer;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.PriceDataDeserializer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Squeeth.*;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Uniswap.oracle;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.CommonFunctions.*;

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

    /**
     * Gets the prices for ALL assets at a particular block
     * @param block A nonnull and non-negative block number to obtain the prices of all the assets
     * @return A PriceData object for a particular block
     */
    @Nonnull
    public static PriceData getPriceData(@Nonnull Long block) throws ExecutionException, InterruptedException {
        PriceData data = cachedPrices.get(block);

        if(data == null) {
            data = new PriceData();

            // Get ETH/USD price
            data.ethUsdc = getEthUsd(block);
            // Get oSQTH/ETH price
            data.osqthEth = getOsqthEth(block);
            // Get Crabv1/ETH price
            data.crabEth = getCrabv1Eth(block, data.osqthEth);
            // Get Crabv2/ETH price
            data.crabV2Eth = getCrabv2Eth(block, data.osqthEth);
            // Get norm factor
            data.normFactor = getNormFactor(block);
        } else {
            if(data.ethUsdc.equals(BigInteger.ZERO))
                data.ethUsdc = getEthUsd(block);
            if(data.osqthEth.equals(BigInteger.ZERO))
                data.osqthEth = getOsqthEth(block);
            if(data.crabEth.equals(BigInteger.ZERO))
                data.crabEth = getCrabv1Eth(block, data.osqthEth);
            if(data.crabV2Eth.equals(BigInteger.ZERO))
                data.crabV2Eth = getCrabv2Eth(block, data.osqthEth);
            if(data.normFactor.equals(BigInteger.ZERO))
                data.normFactor = getNormFactor(block);
        }

        return data;
    }

    /**
     * Gets the prices for the SPECIFIED array of PriceData.Prices to update for a specific block
     * @param block A nonnull and non-negative block number to obtain the prices of all the assets
     * @param pricesToUpdate A nonnull and non-empty array to obtain the prices for
     * @return A PriceData object for a particular block
     */
    @Nonnull
    public static PriceData getPriceData(@Nonnull Long block, @Nonnull PriceData.Prices[] pricesToUpdate) throws ExecutionException, InterruptedException {
        PriceData data = cachedPrices.get(block);

        if(data == null) {
            data = new PriceData();
        }
        if(pricesToUpdate.length == 0) {
            throw new IllegalArgumentException("Requires a non-empty array of PriceData.Prices to update");
        }

        for(PriceData.Prices prices: pricesToUpdate) {
            switch(prices) {
                case ETHUSD -> data.ethUsdc = getEthUsd(block);
                case OSQTHETH -> data.osqthEth = getOsqthEth(block);
                case CRABV1ETH -> {
                    if(data.osqthEth.equals(BigInteger.ZERO))
                        data.osqthEth = getOsqthEth(block);
                    data.crabEth = getCrabv1Eth(block, data.osqthEth);
                }
                case CRABV2ETH -> {
                    if(data.osqthEth.equals(BigInteger.ZERO))
                        data.osqthEth = getOsqthEth(block);
                    data.crabV2Eth = getCrabv2Eth(block, data.osqthEth);
                }
                case NORMFACTOR -> data.normFactor = getNormFactor(block);
            }
        }

        return data;
    }

    /**
     * Gets the prices for an optional array of PriceData.Prices to update for the LATEST block
     * @param pricesToUpdate A nullable array to obtain the prices for. If not null, the array mustn't be empty.
     * @return A PriceData object for the specified prices to obtain for at the latest block
     */
    @Nonnull
    public static PriceData getPriceData(@Nullable PriceData.Prices[] pricesToUpdate) throws ExecutionException, InterruptedException, IOException {
        long latestBlock = EthereumRPCHandler.web3.ethBlockNumber().send().getBlockNumber().longValue();

        if(pricesToUpdate == null)
            return getPriceData(latestBlock);
        else
            return getPriceData(latestBlock, pricesToUpdate);
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

    public static void removeData(@Nonnull String address, @Nonnull String tokenAddress) {
        ArrayList<SimpleTokenTransferResponse> originalList = cachedTransfers.get(address);
        if(originalList == null) return;
        ArrayList<SimpleTokenTransferResponse> filteredList = (ArrayList<SimpleTokenTransferResponse>) originalList.stream().filter(t -> !t.token.equalsIgnoreCase(tokenAddress)).collect(Collectors.toList());

        if(!originalList.equals(filteredList)) {
            cachedTransfers.put(address, filteredList);

            try {
                writeDataToFile(cachedTransfers, cachedTransfersPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void removeData(long timestamp) {
        cachedPrices.remove(timestamp);

        try {
            writeDataToFile(cachedPrices, cachedPricesPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearTransfers() throws IOException {
        cachedTransfers.clear();

        Writer writer = Files.newBufferedWriter(cachedTransfersPath);

        writer.write("");

        writer.close();
    }

    public static void clearPrices() throws IOException {
        cachedPrices.clear();

        Writer writer = Files.newBufferedWriter(cachedPricesPath);

        writer.write("");

        writer.close();
    }

    public static void writeDataToFile(@Nonnull Object data, @Nonnull Path path) throws IOException {
        Writer writer = Files.newBufferedWriter(path);

        new Gson().toJson(data, writer);

        writer.close();
    }

    private static BigInteger getEthUsd(long block) throws ExecutionException, InterruptedException {
        return (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                oracle,
                getTwap_ethUsd,
                block
        ).get(0).getValue();
    }

    private static BigInteger getOsqthEth(long block) throws ExecutionException, InterruptedException {
        return (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                oracle,
                getTwap_osqth,
                block
        ).get(0).getValue();
    }

    @SuppressWarnings("rawtypes")
    private static BigInteger getCrabv1Eth(long block, @Nonnull BigInteger osqthEth) throws ExecutionException, InterruptedException {
        if(osqthEth.equals(BigInteger.ZERO))
            throw new IllegalArgumentException("osqthEth cannot be zero!");

        List<Type> vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(
                crabv1,
                getVaultDetails,
                block
        );
        BigInteger ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
        BigInteger shortOsqth = (BigInteger) vaultDetails.get(3).getValue();
        BigInteger netEth = ethCollateral.subtract(shortOsqth.multiply(osqthEth).divide(BigInteger.valueOf((long) Math.pow(10,18))));
        BigInteger crabTotalSupply = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                crabv1,
                callTotalSupply,
                block
        ).get(0).getValue();

        return netEth.multiply(BigInteger.valueOf((long) Math.pow(10,18))).divide(crabTotalSupply);
    }

    @SuppressWarnings("rawtypes")
    private static BigInteger getCrabv2Eth(long block, @Nonnull BigInteger osqthEth) throws ExecutionException, InterruptedException {
        if(osqthEth.equals(BigInteger.ZERO))
            throw new IllegalArgumentException("osqthEth cannot be zero!");

        List<Type> vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(
                crabv2,
                getVaultDetails,
                block
        );
        BigInteger ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
        BigInteger shortOsqth = (BigInteger) vaultDetails.get(3).getValue();
        BigInteger netEth = ethCollateral.subtract(shortOsqth.multiply(osqthEth).divide(BigInteger.valueOf((long) Math.pow(10,18))));
        BigInteger crabTotalSupply = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                crabv2,
                callTotalSupply,
                block
        ).get(0).getValue();

        return netEth.multiply(BigInteger.valueOf((long) Math.pow(10,18))).divide(crabTotalSupply);
    }

    private static BigInteger getNormFactor(long block) throws ExecutionException, InterruptedException {
        return (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                controller,
                getExpectedNormFactor,
                block
        ).get(0).getValue();
    }
}
