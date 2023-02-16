// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler.api.ethereum;

import com.google.gson.Gson;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.ens.EnsResolver;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import space.alphaserpentis.squeethdiscordbot.data.api.alchemy.AlchemyRequest;
import space.alphaserpentis.squeethdiscordbot.data.api.alchemy.SimpleTokenTransferResponse;
import space.alphaserpentis.squeethdiscordbot.data.api.alchemy.TokenTransferResponse;
import space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class EthereumRPCHandler {

    public static Web3j web3;
    public static URL url;

    @SuppressWarnings("rawtypes")
    public static List<Type> ethCallAtSpecificBlock(@NonNull String address, @NonNull Function function, @NonNull Long block) throws ExecutionException, InterruptedException {
        Request<?, EthCall> ethCallRequest = web3.ethCall(Transaction.createEthCallTransaction(
                Addresses.zeroAddress,
                address,
                FunctionEncoder.encode(function)
        ), new DefaultBlockParameterNumber(block));

        for(int i = 0; i < 3; i++) {
            try {
                if(i != 0)
                    Thread.sleep(1500);

                return FunctionReturnDecoder.decode(
                        ethCallRequest.sendAsync().get().getResult(),
                        function.getOutputParameters()
                );
            } catch(ExecutionException ignored) {

            }
        }

        return FunctionReturnDecoder.decode( // last attempt
                ethCallRequest.sendAsync().get().getResult(),
                function.getOutputParameters()
        );
    }

    @SuppressWarnings("rawtypes")
    public static List<Type> ethCallAtLatestBlock(@NonNull String address, @NonNull Function function) throws ExecutionException, InterruptedException {
        Request<?, EthCall> ethCallRequest = web3.ethCall(Transaction.createEthCallTransaction(
                Addresses.zeroAddress,
                address,
                FunctionEncoder.encode(function)
        ), DefaultBlockParameterName.LATEST);

        for(int i = 0; i < 3; i++) {
            try {
                if(i != 0)
                    Thread.sleep(1500);

                return FunctionReturnDecoder.decode(
                        ethCallRequest.sendAsync().get().getResult(),
                        function.getOutputParameters()
                );
            } catch(ExecutionException ignored) {

            }
        }

        return FunctionReturnDecoder.decode( // last attempt
                ethCallRequest.sendAsync().get().getResult(),
                function.getOutputParameters()
        );
    }

    public static ArrayList<SimpleTokenTransferResponse> getAssetTransfersOfUser(@NonNull String address, @NonNull String token, long startingBlock, long endingBlock) {
        String[] responses = new String[2];
        ArrayList<SimpleTokenTransferResponse> listOfTransfers = new ArrayList<>();

        TokenTransferResponse inbound, outbound;

        try {
            responses[0] = alchemy_getAssetTransfers("", address, token, null, startingBlock, endingBlock);
            responses[1] = alchemy_getAssetTransfers(address, "", token, null, startingBlock, endingBlock);

            inbound = new Gson().fromJson(responses[0], TokenTransferResponse.class);
            outbound = new Gson().fromJson(responses[1], TokenTransferResponse.class);

            if(inbound.result.pageKey == null) {
                TokenTransferResponse temp = new Gson().fromJson(alchemy_getAssetTransfers("", address, token, inbound.result.pageKey, startingBlock, endingBlock), TokenTransferResponse.class);
                temp.result.transfers.forEach(transfer -> {
                    try {
                        listOfTransfers.add(new SimpleTokenTransferResponse(token, transfer.getBlockNum(), transfer.from, transfer.value, transfer.rawContract.value));
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                while(inbound.result.pageKey != null) {
                    TokenTransferResponse temp = new Gson().fromJson(alchemy_getAssetTransfers("", address, token, inbound.result.pageKey, startingBlock, endingBlock), TokenTransferResponse.class);
                    temp.result.transfers.forEach(transfer -> {
                        try {
                            listOfTransfers.add(new SimpleTokenTransferResponse(token, transfer.getBlockNum(), transfer.from, transfer.value, transfer.rawContract.value));
                        } catch (ExecutionException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    inbound = temp;
                }
            }
            if(outbound.result.pageKey == null) {
                TokenTransferResponse temp = new Gson().fromJson(alchemy_getAssetTransfers(address, "", token, outbound.result.pageKey, startingBlock, endingBlock), TokenTransferResponse.class);
                temp.result.transfers.forEach(transfer -> {
                    try {
                        listOfTransfers.add(new SimpleTokenTransferResponse(token, transfer.getBlockNum(), transfer.from, transfer.value, transfer.rawContract.value));
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                while(outbound.result.pageKey != null) {
                    TokenTransferResponse temp = new Gson().fromJson(alchemy_getAssetTransfers(address, "", token, outbound.result.pageKey, startingBlock, endingBlock), TokenTransferResponse.class);
                    temp.result.transfers.forEach(transfer -> {
                        try {
                            listOfTransfers.add(new SimpleTokenTransferResponse(token, transfer.getBlockNum(), transfer.from, transfer.value, transfer.rawContract.value));
                        } catch (ExecutionException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    outbound = temp;
                }
            }

            listOfTransfers.sort(Comparator.comparing(SimpleTokenTransferResponse::getBlockNum));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return listOfTransfers;
    }

    public static ArrayList<SimpleTokenTransferResponse> getAssetTransfersOfUser(@NonNull String address, @NonNull String token) {
        String[] responses = new String[2];
        ArrayList<SimpleTokenTransferResponse> listOfTransfers = new ArrayList<>();

        TokenTransferResponse inbound, outbound;

        try {
            responses[0] = alchemy_getAssetTransfers("", address, token, null, -1, -1);
            responses[1] = alchemy_getAssetTransfers(address, "", token, null, -1, -1);

            inbound = new Gson().fromJson(responses[0], TokenTransferResponse.class);
            outbound = new Gson().fromJson(responses[1], TokenTransferResponse.class);

            if(inbound.result.pageKey == null) {
                TokenTransferResponse temp = new Gson().fromJson(alchemy_getAssetTransfers("", address, token, inbound.result.pageKey, -1, -1), TokenTransferResponse.class);
                temp.result.transfers.forEach(transfer -> {
                    try {
                        listOfTransfers.add(new SimpleTokenTransferResponse(token, transfer.getBlockNum(), transfer.from, transfer.value, transfer.rawContract.value));
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                while(inbound.result.pageKey != null) {
                    TokenTransferResponse temp = new Gson().fromJson(alchemy_getAssetTransfers("", address, token, inbound.result.pageKey, -1, -1), TokenTransferResponse.class);
                    temp.result.transfers.forEach(transfer -> {
                        try {
                            listOfTransfers.add(new SimpleTokenTransferResponse(token, transfer.getBlockNum(), transfer.from, transfer.value, transfer.rawContract.value));
                        } catch (ExecutionException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    inbound = temp;
                }
            }
            if(outbound.result.pageKey == null) {
                TokenTransferResponse temp = new Gson().fromJson(alchemy_getAssetTransfers(address, "", token, outbound.result.pageKey, -1, -1), TokenTransferResponse.class);
                temp.result.transfers.forEach(transfer -> {
                    try {
                        listOfTransfers.add(new SimpleTokenTransferResponse(token, transfer.getBlockNum(), transfer.from, transfer.value, transfer.rawContract.value));
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                while(outbound.result.pageKey != null) {
                    TokenTransferResponse temp = new Gson().fromJson(alchemy_getAssetTransfers(address, "", token, outbound.result.pageKey, -1, -1), TokenTransferResponse.class);
                    temp.result.transfers.forEach(transfer -> {
                        try {
                            listOfTransfers.add(new SimpleTokenTransferResponse(token, transfer.getBlockNum(), transfer.from, transfer.value, transfer.rawContract.value));
                        } catch (ExecutionException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    outbound = temp;
                }
            }

            listOfTransfers.sort(Comparator.comparing(SimpleTokenTransferResponse::getBlockNum));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return listOfTransfers;
    }

    public static String alchemy_getAssetTransfers(@NonNull String fromAddress, @NonNull String toAddress, @NonNull String token, @Nullable String pageKey, long startingBlock, long endingBlock) throws IOException {
        StringBuilder response = null;
        AlchemyRequest req = new AlchemyRequest();
        Gson gson = new Gson();

        req.method = "alchemy_getAssetTransfers";
        if(Objects.equals(fromAddress, "")) {
            req.params[0].toAddress = toAddress;
        } else {
            req.params[0].fromAddress = fromAddress;
        }
        if(pageKey != null) {
            req.params[0].pageKey = pageKey;
        }
        if(endingBlock != -1 && endingBlock > startingBlock) {
            req.params[0].toBlock = "0x" + Long.toHexString(endingBlock);
        }
        req.params[0].fromBlock = startingBlock == -1 ? "0xd55bca" : "0x" + Long.toHexString(startingBlock);
        req.params[0].category = new String[]{"erc20"};
        req.params[0].contractAddresses = new String[]{token};

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");

        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(gson.toJson(req));

        int responseCode = con.getResponseCode();

        if(responseCode == HttpURLConnection.HTTP_ACCEPTED || responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            response = new StringBuilder();

            while((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();

        } else if(responseCode == 429) { // rate limit
            for(int i = 0; i < 3; i++) {
                try {
                    Thread.sleep(1500);
                    String retryResponse = alchemy_getAssetTransfers(fromAddress, toAddress, token, pageKey, startingBlock, endingBlock);
                    if(!retryResponse.equals("429")) {
                        return retryResponse;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            System.out.println("[EthereumRPCHandler] Connection failed, response code is " + responseCode);
        }

        //System.out.println(response.toString());

        if(response == null) {
            return String.valueOf(responseCode);
        } else {
            return response.toString();
        }
    }

    @NonNull
    public static String getENSName(@NonNull String address) {
        EnsResolver resolver = new EnsResolver(web3);

        try {
            return resolver.reverseResolve(address);
        } catch (Exception e) {
            return address;
        }
    }

    public static String getResolvedAddress(@NonNull String ens) throws Exception {
        EnsResolver resolver = new EnsResolver(web3);

        try {
            return resolver.resolve(ens);
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public static BigInteger getLatestBlockNumber() throws IOException {
        return web3.ethBlockNumber().send().getBlockNumber();
    }
}
