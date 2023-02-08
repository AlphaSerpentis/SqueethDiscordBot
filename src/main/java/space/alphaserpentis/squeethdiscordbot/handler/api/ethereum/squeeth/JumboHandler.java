package space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.squeeth;

import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
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
            long lastAuctionTime
    ) {}

    public record JumboCrabNetResults(
            double usdcAmountNetted,
            double crabAmountNetted,
            double remainingUsdc,
            double remainingCrab,
            double crabUsd,
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
                usdcBalance.doubleValue() / Math.pow(10,18),
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
        ).addOptionalTopics(USDC_DEPOSITED_TOPIC, CRAB_WITHDRAWN_TOPIC);

        Flowable<Log> logFlowable = EthereumRPCHandler.web3.ethLogFlowable(filter);
        AtomicReference<Log> latestLog = new AtomicReference<>();
//        AtomicReference<ArrayList<Log>> logsInSameBlock = new AtomicReference<>(new ArrayList<>());
//
//        Disposable disposable = logFlowable.subscribe(
//                (log) -> {
//                    ArrayList<Log> currentArrayList = logsInSameBlock.getAcquire();
//
//                    if(!currentArrayList.isEmpty()) {
//                        if(!currentArrayList.get(0).getBlockNumber().equals(log.getBlockNumber())) {
//                            currentArrayList.clear();
//                        }
//                    }
//
//                    currentArrayList.add(log);
//                    logsInSameBlock.set(currentArrayList);
//                }
//        );

        Disposable disposable = logFlowable.subscribe(
                latestLog::set
        );

        disposable.dispose();

        // Calculate the netted amount
        BigInteger block = latestLog.get().getBlockNumber();
        long timestamp = EthereumRPCHandler.web3.ethGetBlockByNumber(new DefaultBlockParameterNumber(block.longValue()), false).send().getBlock().getTimestamp().longValue();
        PriceData priceData;
        BigInteger usdcAmount0, usdcAmount1, crabAmount0, crabAmount1, crabUsd;

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

//        for(Log log: logsInSameBlock.get()) {
//            String[] data = new String[2];
//            for(int i = 0; i < 2; i++) {
//
//            }
//        }
//
//        System.out.println(logsInSameBlock);

        return new JumboCrabNetResults(
                usdcAmount0.subtract(usdcAmount1).doubleValue() / Math.pow(10,6),
                crabAmount0.subtract(crabAmount1).doubleValue() / Math.pow(10,18),
                usdcAmount1.doubleValue() / Math.pow(10,6),
                crabAmount1.doubleValue() / Math.pow(10,18),
                priceData.crabV2Eth.multiply(priceData.ethUsdc).divide(BigInteger.TEN.pow(18)).doubleValue() / Math.pow(10,18),
                timestamp
        );
    }

    public static long getLastJumboCrabAuction() throws IOException {
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

        BigInteger block = latestLog.get().getBlockNumber();

        return EthereumRPCHandler.web3.ethGetBlockByNumber(new DefaultBlockParameterNumber(block.longValue()), false).send().getBlock().getTimestamp().longValue();
    }
}
