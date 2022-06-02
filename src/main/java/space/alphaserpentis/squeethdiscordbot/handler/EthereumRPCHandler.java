package space.alphaserpentis.squeethdiscordbot.handler;

import com.google.gson.Gson;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.Transaction;
import space.alphaserpentis.squeethdiscordbot.data.AlchemyRequest;
import space.alphaserpentis.squeethdiscordbot.data.SimpleTokenTransferResponse;
import space.alphaserpentis.squeethdiscordbot.data.TokenTransferResponse;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

public class EthereumRPCHandler {

    public static final String zeroAddress = "0x0000000000000000000000000000000000000000";
    public static Web3j web3;
    public static URL url;

    public static List<Type> ethCallAtSpecificBlock(String address, Function function, Long block) throws ExecutionException, InterruptedException {
        return FunctionReturnDecoder.decode(
                web3.ethCall(Transaction.createEthCallTransaction(
                        zeroAddress,
                        address,
                        FunctionEncoder.encode(function)
                ), new DefaultBlockParameterNumber(block)).sendAsync().get().getResult(),
                function.getOutputParameters()
        );
    }

    public static List<Type> ethCallAtLatestBlock(String address, Function function) throws ExecutionException, InterruptedException {
        return FunctionReturnDecoder.decode(
                web3.ethCall(Transaction.createEthCallTransaction(
                        zeroAddress,
                        address,
                        FunctionEncoder.encode(function)
                ), DefaultBlockParameterName.LATEST).sendAsync().get().getResult(),
                function.getOutputParameters()
        );
    }

    public static ArrayList<SimpleTokenTransferResponse> getAssetTransfersOfUser(String address, String token) {
        String[] responses = new String[2];
        ArrayList<SimpleTokenTransferResponse> listOfTransfers = new ArrayList<>();

        TokenTransferResponse inbound, outbound;

        try {
            responses[0] = alchemy_getAssetTransfers("", address, token, null);
            responses[1] = alchemy_getAssetTransfers(address, "", token, null);

            inbound = new Gson().fromJson(responses[0], TokenTransferResponse.class);
            outbound = new Gson().fromJson(responses[1], TokenTransferResponse.class);

            while(inbound.result.pageKey != null) {
                TokenTransferResponse temp = new Gson().fromJson(alchemy_getAssetTransfers("", address, token, inbound.result.pageKey), TokenTransferResponse.class);
                temp.result.transfers.forEach(transfer -> listOfTransfers.add(new SimpleTokenTransferResponse(transfer.getBlockNum(), transfer.from, transfer.value)));
                inbound = temp;
            }
            while(outbound.result.pageKey != null) {
                TokenTransferResponse temp = new Gson().fromJson(alchemy_getAssetTransfers(address, "", token, outbound.result.pageKey), TokenTransferResponse.class);
                temp.result.transfers.forEach(transfer -> listOfTransfers.add(new SimpleTokenTransferResponse(transfer.getBlockNum(), transfer.from, transfer.value)));
                outbound = temp;
            }

            listOfTransfers.sort(Comparator.comparing(SimpleTokenTransferResponse::getBlockNum));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return listOfTransfers;
    }

    public static String alchemy_getAssetTransfers(String fromAddress, String toAddress, String token, @Nullable String pageKey) throws IOException {
        StringBuffer response = null;
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
        req.params[0].fromBlock = "0xd55bca";
        req.params[0].category = new String[1];
        req.params[0].contractAddresses = new String[1];
        req.params[0].category[0] = "token";
        req.params[0].contractAddresses[0] = token;

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
            response = new StringBuffer();

            while((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();

        } else {
            System.out.println("[EthereumRPCHandler] Connection failed, response code is " + responseCode);
        }

        //System.out.println(response.toString());

        return response.toString();
    }

}
