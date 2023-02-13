package space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.squeeth;

import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.data.api.squeethportal.Auction;
import space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses;
import space.alphaserpentis.squeethdiscordbot.data.ethereum.CommonFunctions;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.PositionsDataHandler;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Squeeth.crabNetting;
import static space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.squeeth.JumboHandler.JumboCrabTopics.*;

public class JumboHandler {

    public interface JumboCrabTopics {
        String CRAB_WITHDRAWN_TOPIC = "0x460140ba175e3953a9d581c92fd2fc50c428691b4220431ff4a3f08c62aa3906";
        String USDC_DEPOSITED_TOPIC = "0xf407c5a0c99a9a3d15fe5a46a4aba5ce747e855b58bc1aec4a589fd53bda599e";
        String BID_TRADED_TOPIC = "0xd1d072f838d64d5c63545bc6cf88a03b39fd711611b7df5b0c0822f745044fcc";
    }

    public record JumboCrabStatistics(
            double pendingUsdc,
            double pendingCrabTokens,
            @NonNull JumboCrabNetResults lastNet,
            @NonNull JumboCrabAuctionResults lastAuction
    ) {}

    public record JumboCrabNetResults(
            double usdcAmountNetted,
            double crabAmountNetted,
            double remainingUsdc,
            double remainingCrab,
            double crabUsd,
            long time
    ) {}

    public record JumboCrabAuctionResults(
            double amountCleared,
            double crabPrice,
            double squeethVol,
            int auctionId,
            @NonNull Auction auction,
            boolean isBuying,
            long time
    ) {}

    @NonNull
    public static JumboCrabStatistics getCurrentJumboCrabStatistics() throws ExecutionException, InterruptedException, IOException {
        BigInteger usdcBalance, crabBalance;

        usdcBalance = (BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(
                Addresses.usdc,
                CommonFunctions.balanceOf(crabNetting)
        ).get(0).getValue();
        crabBalance = (BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(
                Addresses.Squeeth.crabv2,
                CommonFunctions.balanceOf(crabNetting)
        ).get(0).getValue();

        return new JumboCrabStatistics(
                usdcBalance.doubleValue() / Math.pow(10,6),
                crabBalance.doubleValue() / Math.pow(10,18),
                getLastJumboCrabNet(),
                getLastJumboCrabAuction()
        );
    }

    public static JumboCrabNetResults getLastJumboCrabNet() throws IOException, ExecutionException, InterruptedException {
        EthFilter filter = new EthFilter(
                new DefaultBlockParameterNumber(16185539),
                new DefaultBlockParameterNumber(EthereumRPCHandler.web3.ethBlockNumber().send().getBlockNumber()),
                crabNetting
        ).addOptionalTopics(USDC_DEPOSITED_TOPIC, CRAB_WITHDRAWN_TOPIC, BID_TRADED_TOPIC);

        Flowable<Log> logFlowable = EthereumRPCHandler.web3.ethLogFlowable(filter);
        AtomicReference<Log> latestBidLog = new AtomicReference<>();
        AtomicReference<Log> latestLog = new AtomicReference<>();
        Disposable disposable = logFlowable.subscribe(
                (log) -> {
                    if(log.getTopics().get(0).equalsIgnoreCase(BID_TRADED_TOPIC)) {
                        latestBidLog.set(log);
                    } else {
                        if(!latestBidLog.get().getBlockNumber().equals(log.getBlockNumber()))
                            latestLog.set(log);
                    }
                }
        );

        disposable.dispose();

        // Calculate the netted amount
        BigInteger block = latestLog.get().getBlockNumber();
        long timestamp = EthereumRPCHandler.web3.ethGetBlockByNumber(new DefaultBlockParameterNumber(block.longValue()), false).send().getBlock().getTimestamp().longValue();
        PriceData priceData;
        BigInteger usdcAmount0, usdcAmount1, crabAmount0, crabAmount1;

        priceData = PositionsDataHandler.getPriceData(
                block.longValue(),
                new PriceData.Prices[]{PriceData.Prices.ETHUSD, PriceData.Prices.CRABV2ETH}
        );
        usdcAmount0 = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                Addresses.usdc,
                CommonFunctions.balanceOf(crabNetting),
                block.longValue() - 1
        ).get(0).getValue();
        crabAmount0 = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                Addresses.Squeeth.crabv2,
                CommonFunctions.balanceOf(crabNetting),
                block.longValue() - 1
        ).get(0).getValue();
        usdcAmount1 = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                Addresses.usdc,
                CommonFunctions.balanceOf(crabNetting),
                block.longValue()
        ).get(0).getValue();
        crabAmount1 = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                Addresses.Squeeth.crabv2,
                CommonFunctions.balanceOf(crabNetting),
                block.longValue()
        ).get(0).getValue();

        return new JumboCrabNetResults(
                usdcAmount0.subtract(usdcAmount1).doubleValue() / Math.pow(10,6),
                crabAmount0.subtract(crabAmount1).doubleValue() / Math.pow(10,18),
                usdcAmount1.doubleValue() / Math.pow(10,6),
                crabAmount1.doubleValue() / Math.pow(10,18),
                priceData.crabV2Eth.multiply(priceData.ethUsdc).divide(BigInteger.TEN.pow(18)).doubleValue() / Math.pow(10,18),
                timestamp
        );
    }

    public static JumboCrabAuctionResults getLastJumboCrabAuction() throws IOException, ExecutionException, InterruptedException {
        EthFilter filter = new EthFilter(
                new DefaultBlockParameterNumber(16185539),
                new DefaultBlockParameterNumber(EthereumRPCHandler.web3.ethBlockNumber().send().getBlockNumber()),
                crabNetting
        ).addOptionalTopics(BID_TRADED_TOPIC);

        Flowable<Log> logFlowable = EthereumRPCHandler.web3.ethLogFlowable(filter);
        AtomicReference<Log> latestLog = new AtomicReference<>();

        Disposable disposable = logFlowable.subscribe(
                latestLog::set
        );

        disposable.dispose();

        PriceData priceData = PositionsDataHandler.getPriceData(new PriceData.Prices[]{PriceData.Prices.ETHUSD, PriceData.Prices.CRABV2ETH, PriceData.Prices.SQUEETHVOL});
        BigInteger block = latestLog.get().getBlockNumber();
        BigInteger usdcAmount0, usdcAmount1, crabAmount0, crabAmount1;

        // Decode the latest log data and get a boolean for whether it's a buy or sell
        String[] data = latestLog.get().getData().substring(2).split("(?<=\\G.{64})");
        int auctionId = ((BigInteger) FunctionReturnDecoder.decodeIndexedValue(latestLog.get().getTopics().get(1), new TypeReference<Uint256>() {}).getValue()).intValue();
        boolean isBuying = data[2].endsWith("01");

        if(isBuying) { // buying crab, this clears the USDC queue
            usdcAmount0 = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                    Addresses.usdc,
                    CommonFunctions.balanceOf(crabNetting),
                    block.longValue() - 1
            ).get(0).getValue();
            usdcAmount1 = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                    Addresses.usdc,
                    CommonFunctions.balanceOf(crabNetting),
                    block.longValue()
            ).get(0).getValue();

            return new JumboCrabAuctionResults(
                    usdcAmount0.subtract(usdcAmount1).doubleValue() / Math.pow(10,6),
                    priceData.crabV2Eth.multiply(priceData.ethUsdc).doubleValue() / Math.pow(10,36),
                    priceData.squeethVol,
                    auctionId,
                    AuctionHandler.getAuction((long) auctionId),
                    true,
                    EthereumRPCHandler.web3.ethGetBlockByNumber(new DefaultBlockParameterNumber(block.longValue()), false).send().getBlock().getTimestamp().longValue()
            );
        } else { // selling usdc, this clears the crab queue
            crabAmount0 = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                    Addresses.Squeeth.crabv2,
                    CommonFunctions.balanceOf(crabNetting),
                    block.longValue() - 1
            ).get(0).getValue();
            crabAmount1 = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(
                    Addresses.Squeeth.crabv2,
                    CommonFunctions.balanceOf(crabNetting),
                    block.longValue()
            ).get(0).getValue();
            return new JumboCrabAuctionResults(
                    crabAmount0.subtract(crabAmount1).doubleValue() / Math.pow(10,18),
                    priceData.crabV2Eth.multiply(priceData.ethUsdc).doubleValue() / Math.pow(10,36),
                    priceData.squeethVol,
                    auctionId,
                    AuctionHandler.getAuction((long) auctionId),
                    false,
                    EthereumRPCHandler.web3.ethGetBlockByNumber(new DefaultBlockParameterNumber(block.longValue()), false).send().getBlock().getTimestamp().longValue()
            );
        }
    }

    public static JumboCrabAuctionResults getLastJumboCrabAuction(int auctionId) {
        return null;
    }
}
