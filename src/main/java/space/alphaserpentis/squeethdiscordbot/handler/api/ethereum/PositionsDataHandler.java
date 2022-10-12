// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler.api.ethereum;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint128;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint96;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.data.api.alchemy.SimpleTokenTransferResponse;
import space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.PositionsDataDeserializer;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.PriceDataDeserializer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Squeeth.crabv2;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Squeeth.osqth;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Uniswap.*;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.usdc;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.weth;

public class PositionsDataHandler {

    public static Map<String, ArrayList<SimpleTokenTransferResponse>> cachedTransfers = new HashMap<>();
    public static Map<Long, PriceData> cachedPrices = new HashMap<>();
    public static Path cachedTransfersPath;
    public static Path cachedPricesPath;
    private static final Function getTwap_ethUsd = new Function(
            "getTwap",
            Arrays.asList(
                    new org.web3j.abi.datatypes.Address(ethUsdcPool),
                    new org.web3j.abi.datatypes.Address(weth),
                    new org.web3j.abi.datatypes.Address(usdc),
                    new Uint32(1),
                    new org.web3j.abi.datatypes.Bool(true)
            ),
            List.of(
                    new TypeReference<Uint256>() {
                    }
            )
    );
    private static final Function getTwap_osqth = new Function(
            "getTwap",
            Arrays.asList(
                    new org.web3j.abi.datatypes.Address(osqthEthPool),
                    new org.web3j.abi.datatypes.Address(osqth),
                    new org.web3j.abi.datatypes.Address(weth),
                    new Uint32(1),
                    new org.web3j.abi.datatypes.Bool(true)
            ),
            List.of(
                    new TypeReference<Uint256>() {
                    }
            )
    );
    private static final Function getVaultDetails = new Function("getVaultDetails",
            Collections.emptyList(),
            Arrays.asList(
                    new TypeReference<Address>() { },
                    new TypeReference<Uint32>() { },
                    new TypeReference<Uint96>() { },
                    new TypeReference<Uint128>() { }
            )
    );
    private static final Function callTotalSupply = new Function("totalSupply",
            Collections.emptyList(),
            List.of(
                    new TypeReference<Uint256>() {
                    }
            )
    );

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
    @SuppressWarnings("rawtypes")
    public static PriceData getPrice(@Nonnull Long block) throws ExecutionException, InterruptedException {
        PriceData data = cachedPrices.get(block);

        if(data == null) {
            data = new PriceData();

            // Get ETH/USD price
            data.ethUsdc = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(oracle, getTwap_ethUsd, block).get(0).getValue();
            // Get oSQTH/ETH price
            data.osqthEth = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(oracle, getTwap_osqth, block).get(0).getValue();
            // Get Crab/ETH price
            List<Type> vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(crabv2, getVaultDetails, block);
            BigInteger ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
            BigInteger shortOsqth = (BigInteger) vaultDetails.get(3).getValue();
            BigInteger netEth = ethCollateral.subtract(shortOsqth.multiply(data.osqthEth).divide(BigInteger.valueOf((long) Math.pow(10,18))));
            BigInteger crabTotalSupply = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(crabv2, callTotalSupply, block).get(0).getValue();

            data.crabV2Eth = netEth.multiply(BigInteger.valueOf((long) Math.pow(10,18))).divide(crabTotalSupply);
        }

        return data;
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

}
