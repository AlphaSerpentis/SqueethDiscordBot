// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import com.google.gson.Gson;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.jetbrains.annotations.NotNull;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint128;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint96;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import space.alphaserpentis.squeethdiscordbot.data.api.squeethportal.Auction;
import space.alphaserpentis.squeethdiscordbot.data.api.squeethportal.GetAuctionByIdResponse;
import space.alphaserpentis.squeethdiscordbot.data.api.squeethportal.LatestCrabAuctionResponse;
import space.alphaserpentis.squeethdiscordbot.data.server.ServerData;
import space.alphaserpentis.squeethdiscordbot.handler.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.LaevitasHandler;
import space.alphaserpentis.squeethdiscordbot.handler.ServerDataHandler;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.*;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Squeeth.*;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Uniswap.*;

public class Crab extends ButtonCommand<MessageEmbed> {

    public static abstract class CrabVault {
        public static final Function callVaultsFunc = new Function("getVaultDetails",
                Collections.emptyList(),
                Arrays.asList(
                        new TypeReference<Address>() { },
                        new TypeReference<Uint32>() { },
                        new TypeReference<Uint96>() { },
                        new TypeReference<Uint128>() { }
                )
        );
        public static final Function callUniswapv3TwapOsqth = new Function("getTwap",
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
        public static final Function callUniswapv3TwapEth = new Function("getTwap",
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
        public static final Function callTotalSupply = new Function("totalSupply",
                Collections.emptyList(),
                List.of(
                        new TypeReference<Uint256>() {
                        }
                )
        );
        public static final Function callTimeAtLastHedge = new Function("timeAtLastHedge",
                Collections.emptyList(),
                List.of(
                        new TypeReference<Uint256>() {
                        }
                )
        );
        public static final Function callNormFactor = new Function("getExpectedNormalizationFactor",
                Collections.emptyList(),
                List.of(
                        new TypeReference<Uint256>() {
                        }
                )
        );
        public final String address;
        public BigInteger ethCollateral;
        public BigInteger shortOsqth;
        public BigInteger priceOfEthInUsd;
        public BigInteger tokenSupply;
        public Vault.VaultGreeks lastRunVaultGreeks, preVaultGreeksAtHedge, postVaultGreeksAtHedge;
        public BigInteger normFactor;
        public double ethPerToken, usdPerToken, rebalancedEth, rebalancedOsqth;
        public long lastHedgeTime;
        public long lastHedgeBlock;
        public boolean rebalanceSoldOsqth;
        public long lastRun = 0, lastRebalanceRun = 0;
        public CrabVault(
            @Nonnull String address
        ) {
            this.address = address;
        }

        public abstract void updateLastHedge() throws IOException;
        private double calculateCollateralRatio() {
            BigInteger debt = shortOsqth.multiply(priceOfEthInUsd).multiply(normFactor).divide(BigInteger.valueOf(10000));
            // Divide by 10^36 of debt to get the correctly scaled debt
            return ethCollateral.doubleValue() / (debt.doubleValue() / Math.pow(10,36)) * 100;
        }
    }
    public static class v1 extends CrabVault {
        public v1() {
            super("0xf205ad80bb86ac92247638914265887a8baa437d");
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void updateLastHedge() throws IOException {
            EthFilter filter = new EthFilter(new DefaultBlockParameterNumber(15134805), new DefaultBlockParameterNumber(EthereumRPCHandler.web3.ethBlockNumber().send().getBlockNumber()), address)
                    .addOptionalTopics("0x4c1a959210172325f5c6678421c3834b04ae8ce57f7a7c0c0bbfbb62bca37e34", "0x878fd3ca52ad322c7535f559ee7c91afc67363073783360ef1b1420589dc6174");

            Flowable<Log> logFlowable = EthereumRPCHandler.web3.ethLogFlowable(filter);
            AtomicReference<Log> latestLog = new AtomicReference<>();

            Disposable disposable = logFlowable.subscribe(
                    latestLog::set
            );

            disposable.dispose();

            if(lastHedgeBlock == latestLog.get().getBlockNumber().doubleValue()) {
                return;
            } else {
                lastHedgeBlock = (long) latestLog.get().getBlockNumber().doubleValue();
            }

            if(latestLog.get().getTopics().get(0).equalsIgnoreCase("0x4c1a959210172325f5c6678421c3834b04ae8ce57f7a7c0c0bbfbb62bca37e34")) { // Hedge on Uniswap

                String[] data = new String[4];
                String trimmedData = latestLog.get().getData().substring(2);

                for(int i = 0; i < 4; i++) {
                    data[i] = trimmedData.substring(64*i, 64*(i+1));
                }

                rebalanceSoldOsqth = (boolean) FunctionReturnDecoder.decodeIndexedValue(data[0], new TypeReference<Bool>() {}).getValue();
                rebalancedEth = ((BigInteger) FunctionReturnDecoder.decodeIndexedValue(data[3], new TypeReference<Uint256>() {}).getValue()).doubleValue() / Math.pow(10,18);
                rebalancedOsqth = ((BigInteger) FunctionReturnDecoder.decodeIndexedValue(data[2], new TypeReference<Uint256>() {}).getValue()).doubleValue() / Math.pow(10,18);
            } else { // OTC Hedge

                String[] data = new String[5];
                String trimmedData = latestLog.get().getData().substring(2);

                for(int i = 0; i < 5; i++) {
                    data[i] = trimmedData.substring(64*i, 64*(i+1));
                }

                rebalanceSoldOsqth = (boolean) FunctionReturnDecoder.decodeIndexedValue(data[0], new TypeReference<Bool>() {}).getValue();
                rebalancedEth = ((BigInteger) FunctionReturnDecoder.decodeIndexedValue(data[4], new TypeReference<Uint256>() {}).getValue()).doubleValue() / Math.pow(10,18);
                rebalancedOsqth = ((BigInteger) FunctionReturnDecoder.decodeIndexedValue(data[3], new TypeReference<Uint256>() {}).getValue()).doubleValue() / Math.pow(10,18);
            }

            try {
                List<Type> vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(address, callVaultsFunc, lastHedgeBlock - 1);
                List<Type> osqthEthPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3TwapOsqth, lastHedgeBlock - 1);
                List<Type> ethUsdcPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3TwapEth, lastHedgeBlock - 1);
                BigInteger ethCollateral, shortoSQTH, priceOfoSQTH, priceOfETHinUSD, normFactor;
                double usdPerOsqth, usdPerEth, impliedVolInPercent;

                ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
                shortoSQTH = (BigInteger) vaultDetails.get(3).getValue();
                priceOfoSQTH = (BigInteger) osqthEthPrice.get(0).getValue();
                priceOfETHinUSD = (BigInteger) ethUsdcPrice.get(0).getValue();

                normFactor = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(controller, callNormFactor, lastHedgeBlock - 1).get(0).getValue();
                usdPerOsqth = priceOfoSQTH.multiply(priceOfETHinUSD).divide(BigInteger.valueOf((long) Math.pow(10,18))).doubleValue() / Math.pow(10,18);
                usdPerEth = priceOfETHinUSD.doubleValue() / Math.pow(10,18);

                impliedVolInPercent = Math.sqrt(Math.log(priceOfoSQTH.doubleValue() / Math.pow(10,18) * 10000/(normFactor.doubleValue() / Math.pow(10,18) * usdPerEth))/(17.5/365));

                preVaultGreeksAtHedge = new Vault.VaultGreeks(
                        priceOfETHinUSD.doubleValue() / Math.pow(10,18),
                        usdPerOsqth,
                        normFactor.doubleValue() / Math.pow(10,18),
                        impliedVolInPercent,
                        -(shortoSQTH.doubleValue() / Math.pow(10,18)),
                        ethCollateral.doubleValue() / Math.pow(10,18)
                );

                // get post data
                vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(address, callVaultsFunc, lastHedgeBlock);
                osqthEthPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3TwapOsqth, lastHedgeBlock);
                ethUsdcPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3TwapEth, lastHedgeBlock);

                ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
                shortoSQTH = (BigInteger) vaultDetails.get(3).getValue();
                priceOfoSQTH = (BigInteger) osqthEthPrice.get(0).getValue();
                priceOfETHinUSD = (BigInteger) ethUsdcPrice.get(0).getValue();

                normFactor = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(controller, callNormFactor, lastHedgeBlock).get(0).getValue();
                usdPerOsqth = priceOfoSQTH.multiply(priceOfETHinUSD).divide(BigInteger.valueOf((long) Math.pow(10,18))).doubleValue() / Math.pow(10,18);
                usdPerEth = priceOfETHinUSD.doubleValue() / Math.pow(10,18);

                impliedVolInPercent = Math.sqrt(Math.log(priceOfoSQTH.doubleValue() / Math.pow(10,18) * 10000/(normFactor.doubleValue() / Math.pow(10,18) * usdPerEth))/(17.5/365));

                postVaultGreeksAtHedge = new Vault.VaultGreeks(
                        priceOfETHinUSD.doubleValue() / Math.pow(10,18),
                        usdPerOsqth,
                        normFactor.doubleValue() / Math.pow(10,18),
                        impliedVolInPercent,
                        -(shortoSQTH.doubleValue() / Math.pow(10,18)),
                        ethCollateral.doubleValue() / Math.pow(10,18)
                );
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class v2 extends CrabVault {

        public static FeedingTime auction;
        public static long previousNotificationId;

        public static class FeedingTime {

            enum NotificationPhase {
                AUCTION_NOT_ACTIVE,
                SIXTY_MINUTES,
                THIRTY_MINUTES,
                TEN_MINUTES,
                ONE_MINUTE,
                AUCTION_ACTIVE,
                AUCTION_SETTLING
            }

            public static final ArrayList<Long> serversListening = new ArrayList<>();
            public static ScheduledExecutorService scheduledExecutor;
            public static long auctionTime;
            public static NotificationPhase notificationPhase;
            public static long lastBidMessageId;

            public FeedingTime() {
                scheduledExecutor = Executors.newScheduledThreadPool(1);

                scheduledExecutor.schedule(() -> {
                    // check how long until next auction
                    long timeUntilNextAuction = timeUntilNextAuction();

                    // grab servers
                    for(Long serverId: ServerDataHandler.serverDataHashMap.keySet()) {
                        ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);
                        if(sd.getListenToCrabAuctions()) {
                            serversListening.add(serverId);
                        }
                    }

                    if(timeUntilNextAuction - 3600 > 0) {
                        notificationPhase = NotificationPhase.AUCTION_NOT_ACTIVE;
                        scheduledExecutor.schedule(FeedingTime::prepareNotification, (timeUntilNextAuction - 3600), TimeUnit.SECONDS);
                    } else {
                        prepareNotification();
                    }
                }, 1, TimeUnit.SECONDS);
            }

            public static long timeUntilNextAuction() {
                if(notificationPhase != null) {
                    if(notificationPhase != NotificationPhase.AUCTION_NOT_ACTIVE) {
                        return -1;
                    }
                }

                TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
                Calendar calendar = Calendar.getInstance(timeZone);
                TemporalAdjuster ta;
                LocalDate today = LocalDate.now(timeZone.toZoneId());
                LocalDate auctionDay;
                int day = calendar.get(Calendar.DAY_OF_WEEK);
                long timeNow = Instant.now().getEpochSecond();
                long timeThen = 0;

                switch(day) {
                    case Calendar.SATURDAY, Calendar.SUNDAY -> {
                        ta = TemporalAdjusters.next(DayOfWeek.MONDAY);
                        auctionDay = today.with(ta);
                        timeThen = auctionDay.atTime(9,30).atZone(timeZone.toZoneId()).toEpochSecond();
                    }
                    case Calendar.MONDAY -> {
                        timeThen = today.atTime(9,30).atZone(timeZone.toZoneId()).toEpochSecond();

                        if(timeNow >= timeThen) {
                            ta = TemporalAdjusters.next(DayOfWeek.WEDNESDAY);
                            auctionDay = today.with(ta);
                            timeThen = auctionDay.atTime(9,30).atZone(timeZone.toZoneId()).toEpochSecond();
                        }
                    }
                    case Calendar.TUESDAY -> {
                        ta = TemporalAdjusters.next(DayOfWeek.WEDNESDAY);
                        auctionDay = today.with(ta);
                        timeThen = auctionDay.atTime(9,30).atZone(timeZone.toZoneId()).toEpochSecond();
                    }
                    case Calendar.WEDNESDAY -> {
                        timeThen = today.atTime(9,30).atZone(timeZone.toZoneId()).toEpochSecond();

                        if(timeNow >= timeThen) {
                            ta = TemporalAdjusters.next(DayOfWeek.FRIDAY);
                            auctionDay = today.with(ta);
                            timeThen = auctionDay.atTime(9,30).atZone(timeZone.toZoneId()).toEpochSecond();
                        }
                    }
                    case Calendar.THURSDAY -> {
                        ta = TemporalAdjusters.next(DayOfWeek.FRIDAY);
                        auctionDay = today.with(ta);
                        timeThen = auctionDay.atTime(9,30).atZone(timeZone.toZoneId()).toEpochSecond();
                    }
                    case Calendar.FRIDAY -> {
                        timeThen = today.atTime(9,30).atZone(timeZone.toZoneId()).toEpochSecond();

                        if(timeNow >= timeThen) {
                            ta = TemporalAdjusters.next(DayOfWeek.MONDAY);
                            auctionDay = today.with(ta);
                            timeThen = auctionDay.atTime(9,30).atZone(timeZone.toZoneId()).toEpochSecond();
                        }
                    }
                }

                auctionTime = timeThen;

                return timeThen - timeNow;
            }

            public static void prepareNotification() {
                long timeDiff = auctionTime - Instant.now().getEpochSecond();
                NotificationPhase originalPhase = notificationPhase;

                if(timeDiff <= 3600 && timeDiff > 0) {
                    if(timeDiff > 1800) {
                        scheduledExecutor.schedule(FeedingTime::prepareNotification, timeDiff - 1800, TimeUnit.SECONDS);
                        notificationPhase = NotificationPhase.SIXTY_MINUTES;
                    } else if(timeDiff > 600) {
                        scheduledExecutor.schedule(FeedingTime::prepareNotification, timeDiff - 600, TimeUnit.SECONDS);
                        notificationPhase = NotificationPhase.THIRTY_MINUTES;
                    } else if(timeDiff > 60) {
                        scheduledExecutor.schedule(FeedingTime::prepareNotification, timeDiff - 60, TimeUnit.SECONDS);
                        notificationPhase = NotificationPhase.TEN_MINUTES;
                    } else {
                        scheduledExecutor.schedule(FeedingTime::prepareNotification, timeDiff, TimeUnit.SECONDS);
                        notificationPhase = NotificationPhase.ONE_MINUTE;
                    }
                } else if(timeDiff <= 0 && notificationPhase == NotificationPhase.AUCTION_NOT_ACTIVE) { // out-of-date auction time
                    timeUntilNextAuction();
                    prepareNotification(); // check again
                    return;
                } else if(timeDiff <= 0 && timeDiff > -600) {
                    scheduledExecutor.schedule(FeedingTime::prepareNotification, timeDiff + 600, TimeUnit.SECONDS);
                    notificationPhase = NotificationPhase.AUCTION_ACTIVE;
                } else if(timeDiff <= -600 && timeDiff > -1200) {
                    scheduledExecutor.schedule(FeedingTime::prepareNotification, timeDiff + 1200, TimeUnit.SECONDS);
                    notificationPhase = NotificationPhase.AUCTION_SETTLING;
                } else {
                    notificationPhase = NotificationPhase.AUCTION_NOT_ACTIVE;
                    scheduledExecutor.schedule(FeedingTime::prepareNotification, timeUntilNextAuction() - 3600, TimeUnit.SECONDS);
                }

                if(originalPhase != null || notificationPhase == NotificationPhase.AUCTION_NOT_ACTIVE) { // Skips notifying if the bot started up during auction
                    notifyAboutAuction();
                }
            }

            public static void notifyAboutAuction() {
                EmbedBuilder eb = new EmbedBuilder();
                NumberFormat format = NumberFormat.getInstance();
                double[] sizeOfAuction = estimateSizeOfAuction();
                String defaultTitle = "Crab Feeding Time (Crab Auction) Upcoming!";
                String approxSizeOfAuction = null;

                eb.addField("Notice", "The strategy may or may not rebalance in-between the scheduled auctions or rebalance at all", false);

                switch(notificationPhase) {
                    case SIXTY_MINUTES -> eb.setTitle(defaultTitle + " (One Hour Notice)");
                    case THIRTY_MINUTES -> eb.setTitle(defaultTitle + " (Thirty Minute Notice)");
                    case TEN_MINUTES -> eb.setTitle(defaultTitle + " (Ten Minute Notice)");
                    case ONE_MINUTE -> eb.setTitle(defaultTitle + " (One Minute Notice)");
                    case AUCTION_ACTIVE -> eb.setTitle("Crab Feeding Time (Crab Auction) is Live!");
                    case AUCTION_SETTLING -> eb.setTitle("Crab Feeding Time (Crab Auction) is Settling!");
                    case AUCTION_NOT_ACTIVE -> eb.setTitle("Next Crab Feeding Time (Crab Auction) Date");
                }

                if(notificationPhase != NotificationPhase.AUCTION_ACTIVE && notificationPhase != NotificationPhase.AUCTION_SETTLING && notificationPhase != NotificationPhase.AUCTION_NOT_ACTIVE) {
                    if(sizeOfAuction[0] < 0) { // Buying oSQTH, Selling ETH
                        approxSizeOfAuction = "Approximately, Crab is buying " + format.format(sizeOfAuction[1]) + " oSQTH for " + format.format(-1 * sizeOfAuction[0]) + " ETH";
                    } else { // Selling oSQTH, Buying ETH
                        approxSizeOfAuction = "Approximately, Crab is selling " + format.format(-1 * sizeOfAuction[1]) + " oSQTH for " + format.format(sizeOfAuction[0]) + " ETH";
                    }
                }

                if(notificationPhase == NotificationPhase.AUCTION_ACTIVE) {
                    eb.setDescription("Crab Feeding Time is currently active! Users can place bids at https://squeethportal.xyz/auction");
                } else if(notificationPhase == NotificationPhase.AUCTION_SETTLING) {
                    eb.setDescription("Crab Feeding Time is currently in the process of settling; rebalance will occur soon!");
                } else if(notificationPhase != NotificationPhase.AUCTION_NOT_ACTIVE) {
                    eb.setDescription("In <t:" + auctionTime + ":R>, Crab v2 strategy will start an auction! Users can check out the current stats at https://squeethportal.xyz/auction\n\n" + approxSizeOfAuction);
                } else {
                    eb.setDescription("At <t:" + auctionTime + ">, Crab v2 strategy will prepare an auction!");
                }

                for(Long serverId: serversListening) {
                    ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);
                    Guild guild = Launcher.api.getGuildById(serverId);
                    if(sd.getCrabAuctionChannelId() == 0 || !sd.getListenToCrabAuctions() || guild == null) { // ineligible to send
                        break;
                    }

                    guild.getTextChannelById(sd.getCrabAuctionChannelId()).sendMessageEmbeds(eb.build()).queue(
                            (response) -> {
                                if(previousNotificationId != 0)
                                    response.getChannel().asTextChannel().deleteMessageById(previousNotificationId).queue(
                                            (ignored) -> {},
                                            Throwable::printStackTrace
                                    );
                                previousNotificationId = response.getIdLong();
                            },
                            Throwable::printStackTrace
                    );
                }
            }

            public static void updateMessageForBids() {
                EmbedBuilder eb = new EmbedBuilder();

                for(Long serverId: serversListening) {
                    if(lastBidMessageId == 0) { // make new bid message

                    } else { // edit bid message
                        TextChannel channel = Launcher.api.getTextChannelById(ServerDataHandler.serverDataHashMap.get(serverId).getCrabAuctionChannelId());

                    }
                }
            }

            @SuppressWarnings("rawtypes")
            public static double[] estimateSizeOfAuction() {
                double[] sizes = new double[2];

                // Get info
                BigInteger ethUsd, osqthEth, osqthUsd, normFactor, osqthHoldings, ethVaultCollateral;
                BigInteger currentBlock;
                try {
                    currentBlock = EthereumRPCHandler.web3.ethBlockNumber().send().getBlockNumber();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                double impliedVol;

                try {
                    List<Type> vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(crabV2.address, callVaultsFunc, currentBlock.longValue());
                    List<Type> osqthEthPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3TwapOsqth, currentBlock.longValue());
                    List<Type> ethUsdcPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3TwapEth, currentBlock.longValue());
                    List<Type> normFactorResult = EthereumRPCHandler.ethCallAtSpecificBlock(controller, callNormFactor, currentBlock.longValue());

                    ethVaultCollateral = (BigInteger) vaultDetails.get(2).getValue();
                    osqthHoldings = (BigInteger) vaultDetails.get(3).getValue();
                    osqthEth = (BigInteger) osqthEthPrice.get(0).getValue();
                    ethUsd = (BigInteger) ethUsdcPrice.get(0).getValue();
                    normFactor = (BigInteger) normFactorResult.get(0).getValue();

                    osqthUsd = osqthEth.multiply(ethUsd).divide(BigInteger.valueOf((long) Math.pow(10,18)));
                    impliedVol = Math.sqrt(Math.log(osqthEth.doubleValue() / Math.pow(10,18) * 10000/(normFactor.doubleValue() / Math.pow(10,18) * (ethUsd.doubleValue() / Math.pow(10,18))))/(17.5/365));

                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                // Calculate delta
                double delta = new Vault.VaultGreeks(
                        ethUsd.doubleValue() / Math.pow(10,18),
                        osqthUsd.doubleValue() / Math.pow(10,18),
                        normFactor.doubleValue() / Math.pow(10,18),
                        impliedVol,
                        -osqthHoldings.doubleValue() / Math.pow(10,18),
                        ethVaultCollateral.doubleValue() / Math.pow(10,18)
                ).delta;

                sizes[0] = delta;
                sizes[1] = -delta/(osqthEth.doubleValue() / Math.pow(10,18));

                return sizes;
            }

            public static long getLatestAuctionId() throws IOException {
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

                    if(!responseConverted.isLive) return responseConverted.auction.currentAuctionId - 1;

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

                        if(!responseConverted.isLive) return responseConverted.auction.currentAuctionId - 1;

                        return responseConverted.auction.currentAuctionId;
                    } else {
                        con.disconnect();
                        return -1;
                    }
                }
            }

//            @Nonnull
//            @CheckReturnValue
//            public static ArrayList<Auction.Bid> getCurrentBids() throws IOException {
//                HttpsURLConnection con = (HttpsURLConnection) new URL("https://squeethportal.xyz/api/auction/getLatestAuction").openConnection();
//                con.setRequestMethod("GET");
//                con.setInstanceFollowRedirects(true);
//                con.setDoOutput(true);
//
//                int responseCode = con.getResponseCode();
//
//                if(responseCode == HttpURLConnection.HTTP_OK) {
//                    Gson gson = new Gson();
//                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//                    String inputLine;
//                    StringBuilder response = new StringBuilder();
//                    LatestCrabAuctionResponse responseConverted;
//
//                    while((inputLine = in.readLine()) != null) {
//                        response.append(inputLine);
//                    }
//
//                    in.close();
//                    con.disconnect();
//
//                    responseConverted = gson.fromJson(String.valueOf(response), LatestCrabAuctionResponse.class);
//
//                    if(!responseConverted.isLive) return new ArrayList<>();
//
//                    return new ArrayList<>(responseConverted.auction.bids.values());
//                } else {
//                    if(responseCode == 308) {
//                        URL newUrl = new URL(con.getHeaderField("Location"));
//                        con = (HttpsURLConnection) newUrl.openConnection();
//                        con.setDoOutput(true);
//
//                        Gson gson = new Gson();
//                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
//                        String inputLine;
//                        StringBuilder response = new StringBuilder();
//                        LatestCrabAuctionResponse responseConverted;
//
//                        while((inputLine = in.readLine()) != null) {
//                            response.append(inputLine);
//                        }
//
//                        in.close();
//                        con.disconnect();
//
//                        responseConverted = gson.fromJson(String.valueOf(response), LatestCrabAuctionResponse.class);
//
//                        if(!responseConverted.isLive) return new ArrayList<>();
//
//                        return new ArrayList<>(responseConverted.auction.bids.values());
//                    } else {
//                        con.disconnect();
//                        return new ArrayList<>();
//                    }
//                }
//            }

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

            public static long getAuctionTime() {
                return auctionTime;
            }
        }

        public v2() {
            super("0x3B960E47784150F5a63777201ee2B15253D713e8");
            auction = new FeedingTime();
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void updateLastHedge() throws IOException {
            EthFilter filter = new EthFilter(new DefaultBlockParameterNumber(15134805), new DefaultBlockParameterNumber(EthereumRPCHandler.web3.ethBlockNumber().send().getBlockNumber()), address)
                    .addOptionalTopics("0xbbc3ba742efe346cfdf333000069964e0ee3087c68da267dac977d299f2366fb");

            Flowable<Log> logFlowable = EthereumRPCHandler.web3.ethLogFlowable(filter);
            AtomicReference<Log> latestLog = new AtomicReference<>();

            Disposable disposable = logFlowable.subscribe(
                    latestLog::set
            );

            disposable.dispose();

            if(lastHedgeBlock == latestLog.get().getBlockNumber().doubleValue()) {
                return;
            } else {
                lastHedgeBlock = (long) latestLog.get().getBlockNumber().doubleValue();
            }

            String[] data = new String[4];
            String trimmedData = latestLog.get().getData().substring(2);

            for(int i = 0; i < 4; i++) {
                data[i] = trimmedData.substring(64*i, 64*(i+1));
            }

            rebalancedOsqth = ((BigInteger) FunctionReturnDecoder.decodeIndexedValue(data[1], new TypeReference<Uint256>() {}).getValue()).doubleValue() / Math.pow(10,18);
            rebalanceSoldOsqth = !(boolean) FunctionReturnDecoder.decodeIndexedValue(data[2], new TypeReference<Bool>() {}).getValue();
            rebalancedEth = ((BigInteger) FunctionReturnDecoder.decodeIndexedValue(data[3], new TypeReference<Uint256>() {}).getValue()).doubleValue() / Math.pow(10,18) * rebalancedOsqth;

            try {
                List<Type> vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(address, callVaultsFunc, lastHedgeBlock - 1);
                List<Type> osqthEthPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3TwapOsqth, lastHedgeBlock - 1);
                List<Type> ethUsdcPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3TwapEth, lastHedgeBlock - 1);
                BigInteger ethCollateral, shortoSQTH, priceOfoSQTH, priceOfETHinUSD, normFactor;
                double usdPerOsqth, usdPerEth, impliedVolInPercent;

                ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
                shortoSQTH = (BigInteger) vaultDetails.get(3).getValue();
                priceOfoSQTH = (BigInteger) osqthEthPrice.get(0).getValue();
                priceOfETHinUSD = (BigInteger) ethUsdcPrice.get(0).getValue();

                normFactor = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(controller, callNormFactor, lastHedgeBlock - 1).get(0).getValue();
                usdPerOsqth = priceOfoSQTH.multiply(priceOfETHinUSD).divide(BigInteger.valueOf((long) Math.pow(10,18))).doubleValue() / Math.pow(10,18);
                usdPerEth = priceOfETHinUSD.doubleValue() / Math.pow(10,18);

                impliedVolInPercent = Math.sqrt(Math.log(priceOfoSQTH.doubleValue() / Math.pow(10,18) * 10000/(normFactor.doubleValue() / Math.pow(10,18) * usdPerEth))/(17.5/365));

                preVaultGreeksAtHedge = new Vault.VaultGreeks(
                        priceOfETHinUSD.doubleValue() / Math.pow(10,18),
                        usdPerOsqth,
                        normFactor.doubleValue() / Math.pow(10,18),
                        impliedVolInPercent,
                        -(shortoSQTH.doubleValue() / Math.pow(10,18)),
                        ethCollateral.doubleValue() / Math.pow(10,18)
                );

                // get post data
                vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(address, callVaultsFunc, lastHedgeBlock);
                osqthEthPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3TwapOsqth, lastHedgeBlock);
                ethUsdcPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3TwapEth, lastHedgeBlock);

                ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
                shortoSQTH = (BigInteger) vaultDetails.get(3).getValue();
                priceOfoSQTH = (BigInteger) osqthEthPrice.get(0).getValue();
                priceOfETHinUSD = (BigInteger) ethUsdcPrice.get(0).getValue();

                normFactor = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(controller, callNormFactor, lastHedgeBlock).get(0).getValue();
                usdPerOsqth = priceOfoSQTH.multiply(priceOfETHinUSD).divide(BigInteger.valueOf((long) Math.pow(10,18))).doubleValue() / Math.pow(10,18);
                usdPerEth = priceOfETHinUSD.doubleValue() / Math.pow(10,18);

                impliedVolInPercent = Math.sqrt(Math.log(priceOfoSQTH.doubleValue() / Math.pow(10,18) * 10000/(normFactor.doubleValue() / Math.pow(10,18) * usdPerEth))/(17.5/365));

                postVaultGreeksAtHedge = new Vault.VaultGreeks(
                        priceOfETHinUSD.doubleValue() / Math.pow(10,18),
                        usdPerOsqth,
                        normFactor.doubleValue() / Math.pow(10,18),
                        impliedVolInPercent,
                        -(shortoSQTH.doubleValue() / Math.pow(10,18)),
                        ethCollateral.doubleValue() / Math.pow(10,18)
                );
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static CrabVault crabV1, crabV2;

    public Crab() {
        super(new BotCommandOptions(
           "crab",
           "Get current statistics on the Crab strategy!",
           0,
           0,
           true,
           false,
           true,
           true,
           false,
           false
        ));

        try {
            crabV1 = new v1();
            crabV2 = new v2();
            crabV1.updateLastHedge();
            crabV2.updateLastHedge();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        buttonHashMap.put("Refresh", Button.danger("crab_refresh", "Refresh"));
    }

    @Override
    public void runButtonInteraction(@NotNull ButtonInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        InteractionHook pending = event.deferEdit().complete();

        if(event.getButton().getId().equalsIgnoreCase("crab_refresh")) {
            String parseFooter = event.getMessage().getEmbeds().get(0).getFooter().getText();

            if(parseFooter.equalsIgnoreCase("Latest")) {
                bidsPage(eb, -1);
            } else {
                bidsPage(eb, Integer.parseInt(parseFooter.substring(parseFooter.indexOf(" ") + 1)));
            }
            pending.editOriginalEmbeds(eb.build()).complete();
        }
    }

    @NotNull
    @Override
    public Collection<ItemComponent> addButtons(@NotNull GenericCommandInteractionEvent event) {
        if(event.getSubcommandName().equalsIgnoreCase("bids") && event.getOptions().size() == 0) {
            return Arrays.asList(new ItemComponent[]{getButton("Refresh")});
        } else {
            return Collections.emptyList();
        }
    }

    @Nonnull
    @Override
    public MessageEmbed runCommand(long userId, @Nonnull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        CrabVault crab;
        int bidsId = -1;

        if(event.getOptions().size() != 0 && !event.getSubcommandName().equalsIgnoreCase("bids")) {
            if(event.getOptions().get(0).getAsBoolean()) { // run v1
                crab = crabV1;
            } else { // run v2
                crab = crabV2;
            }
        } else {
            crab = crabV2;
        }

        switch(event.getSubcommandName()) {
            case "stats" -> statsPage(eb, crab);
            case "latest" -> rebalancePage(eb, crab);
            case "bids" -> {
                if(event.getOptions().isEmpty()) {
                    bidsPage(eb, -1);
                } else {
                    bidsPage(eb, bidsId);
                }
            }
        }

        return eb.build();
    }

    @Override
    public void addCommand(@Nonnull JDA jda) {
        SubcommandData stats = new SubcommandData("stats", "Regular statistics on Crab").addOption(OptionType.BOOLEAN, "v1", "True to toggle v1 stats", false);
        SubcommandGroupData rebalance = new SubcommandGroupData("rebalance", "Shows the rebalancing-related commands")
                .addSubcommands(
                        new SubcommandData("latest", "Shows the latest rebalancing stats").addOption(OptionType.BOOLEAN, "v1", "True to toggle v1 rebalance", false),
                        new SubcommandData("bids", "Shows the latest/specific auction bids").addOption(OptionType.INTEGER, "id", "Auction ID to specify", false)
                );

        Command cmd = jda.upsertCommand(name, description).addSubcommands(stats).addSubcommandGroups(rebalance).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@Nonnull JDA jda) {
        SubcommandData stats = new SubcommandData("stats", "Regular statistics on Crab").addOption(OptionType.BOOLEAN, "v1", "True to toggle v1 stats", false);
        SubcommandGroupData rebalance = new SubcommandGroupData("rebalance", "Shows the rebalancing-related commands")
                .addSubcommands(
                        new SubcommandData("latest", "Shows the latest rebalancing stats").addOption(OptionType.BOOLEAN, "v1", "True to toggle v1 rebalance", false),
                        new SubcommandData("bids", "Shows the latest/specific auction bids").addOption(OptionType.INTEGER, "id", "Auction ID to specify", false)
                );

        Command cmd = jda.upsertCommand(name, description).addSubcommands(stats).addSubcommandGroups(rebalance).complete();

        commandId = cmd.getIdLong();
    }

    @SuppressWarnings("rawtypes")
    private void statsPage(@Nonnull EmbedBuilder eb, @Nonnull CrabVault crab) {
        Vault.VaultGreeks vaultGreeks = crab.lastRunVaultGreeks;
        if(crab.lastRun + 60 < Instant.now().getEpochSecond()) {
            try {
                List<Type> vaultDetails = EthereumRPCHandler.ethCallAtLatestBlock(crab.address, CrabVault.callVaultsFunc);
                List<Type> osqthEthPrice = EthereumRPCHandler.ethCallAtLatestBlock(oracle, CrabVault.callUniswapv3TwapOsqth);
                List<Type> ethUsdcPrice = EthereumRPCHandler.ethCallAtLatestBlock(oracle, CrabVault.callUniswapv3TwapEth);

                crab.ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
                crab.shortOsqth = (BigInteger) vaultDetails.get(3).getValue();
                BigInteger priceOfoSQTH = (BigInteger) osqthEthPrice.get(0).getValue();
                crab.priceOfEthInUsd = (BigInteger) ethUsdcPrice.get(0).getValue();
                crab.tokenSupply = (BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(crab.address, CrabVault.callTotalSupply).get(0).getValue();
                crab.lastHedgeTime = ((BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(crab.address, CrabVault.callTimeAtLastHedge).get(0).getValue()).longValue();
                DecimalFormat df = new DecimalFormat("#");

                crab.normFactor = new BigInteger(String.valueOf(df.format(LaevitasHandler.latestSqueethData.getNormalizationFactor() * (long) Math.pow(10,18))));

                BigInteger netEth = crab.ethCollateral.subtract(crab.shortOsqth.multiply(priceOfoSQTH).divide(BigInteger.valueOf((long) Math.pow(10,18))));

                crab.ethPerToken = netEth.multiply(BigInteger.valueOf((long) Math.pow(10,18))).divide(crab.tokenSupply).doubleValue() / Math.pow(10, 18);
                crab.usdPerToken = netEth.multiply(crab.priceOfEthInUsd).divide(crab.tokenSupply).doubleValue() / Math.pow(10, 18);

                crab.lastRun = Instant.now().getEpochSecond();

                vaultGreeks = new Vault.VaultGreeks(
                        crab.priceOfEthInUsd.doubleValue() / Math.pow(10,18),
                        LaevitasHandler.latestSqueethData.getoSQTHPrice(),
                        crab.normFactor.doubleValue() / Math.pow(10,18),
                        LaevitasHandler.latestSqueethData.getCurrentImpliedVolatility()/100,
                        -(crab.shortOsqth.doubleValue() / Math.pow(10,18)),
                        crab.ethCollateral.doubleValue() / Math.pow(10,18)
                );
                crab.lastRunVaultGreeks = vaultGreeks;
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        NumberFormat instance = NumberFormat.getInstance();

        eb.setTitle("Crab " + (crab instanceof v1 ? "v1" : "v2") + " Statistics");
        eb.setThumbnail("https://c.tenor.com/3CIbJomibvYAAAAi/crab-rave.gif");
        eb.setDescription("Get all of your crabby stats here!\n\nhttps://squeeth.com/strategies" + (LaevitasHandler.isDataStale() ? "\n\n**(Data is stale! Calculations may be off!)**" : ""));
        eb.addField("ETH Collateral", instance.format(crab.ethCollateral.divide(BigInteger.valueOf((long) Math.pow(10,18))).doubleValue()) + " Ξ", false);
        eb.addField("Vault Debt", instance.format(crab.shortOsqth.divide(BigInteger.valueOf((long) Math.pow(10,18))).doubleValue()) + " oSQTH", false);
        eb.addField("Collateral Ratio", instance.format(crab.calculateCollateralRatio()) + "%", false);
        eb.addField("Price per Crab Token", "$" + instance.format(crab.usdPerToken) + " (" + instance.format(crab.ethPerToken) + " Ξ)", false);
        eb.addField("Total Supply of Crab", instance.format(crab.tokenSupply.divide(BigInteger.valueOf((long) Math.pow(10,18)))), false);
        eb.addField("Last Rebalance", "<t:" + crab.lastHedgeTime + ">", false);
        eb.addField("Δ Delta", "$" + instance.format(vaultGreeks.delta), true);
        eb.addField("Γ Gamma", "$" + instance.format(vaultGreeks.gamma), true);
        eb.addBlankField(true);
        eb.addField("ν Vega", "$" + instance.format(vaultGreeks.vega), true);
        eb.addField("Θ Theta", "$" + instance.format(vaultGreeks.theta), true);
        eb.addBlankField(true);
        eb.addField("Greeks Notice", "*Greeks use some Laevitas data which is polled every 5-minutes*", false);
        eb.setFooter("Last Updated at " + Instant.ofEpochSecond(crab.lastRun).atOffset(ZoneOffset.UTC).toOffsetTime());
        eb.setColor(Color.RED);
    }

    private void rebalancePage(@Nonnull EmbedBuilder eb, @Nonnull CrabVault crab) {
        NumberFormat instance = NumberFormat.getInstance();
        DecimalFormat df = new DecimalFormat("#");

        eb.setTitle("Crab " + (crab instanceof v1 ? "v1" : "v2") + " Rebalance Statistics");
        eb.setThumbnail("https://c.tenor.com/3CIbJomibvYAAAAi/crab-rave.gif");
        eb.setDescription("View detailed information that happened during the last rebalance!\n\nParticipate in the auctions: https://www.squeethportal.xyz/auction");

        if(crab.lastRebalanceRun + 60 < Instant.now().getEpochSecond()) {
            try {
                crab.updateLastHedge();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            crab.lastRebalanceRun = Instant.now().getEpochSecond();
        }

        try {
            double timestamp = EthereumRPCHandler.web3.ethGetBlockByNumber(new DefaultBlockParameterNumber(crab.lastHedgeBlock), true).send().getBlock().getTimestamp().doubleValue();
            eb.addField("Last Rebalance", "<t:" + df.format(timestamp) + ">", false);
            if(crab.rebalanceSoldOsqth) {
                eb.addField("Sold", instance.format(crab.rebalancedOsqth) + " oSQTH", false);
                eb.addField("Received", instance.format(crab.rebalancedEth) + " ETH", false);
            } else {
                eb.addField("Bought", instance.format(crab.rebalancedOsqth) + " oSQTH", false);
                eb.addField("Paid", instance.format(crab.rebalancedEth) + " ETH", false);
            }
            if(crab instanceof v2) {
                eb.addField("Upcoming Auction", "<t:" + v2.FeedingTime.getAuctionTime() + ">", false);
            }
            eb.addField("Δ Delta", "$" + instance.format(crab.preVaultGreeksAtHedge.delta) + " → $" + instance.format(crab.postVaultGreeksAtHedge.delta), true);
            eb.addField("Γ Gamma", "$" + instance.format(crab.preVaultGreeksAtHedge.gamma) + " → $" + instance.format(crab.postVaultGreeksAtHedge.gamma), true);
            eb.addBlankField(true);
            eb.addField("ν Vega", "$" + instance.format(crab.preVaultGreeksAtHedge.vega) + " → $" + instance.format(crab.postVaultGreeksAtHedge.vega), true);
            eb.addField("Θ Theta", "$" + instance.format(crab.preVaultGreeksAtHedge.theta) + " → $" + instance.format(crab.postVaultGreeksAtHedge.theta), true);
            eb.addBlankField(true);
            eb.addField("Greeks Notice", "Greeks shown here go from pre-rebalance → post-rebalance", false);
            eb.setColor(Color.RED);
        } catch (IOException e) {
            eb.setDescription("An unexpected error has occurred. Please try again later.");
        }
    }

    private void bidsPage(@Nonnull EmbedBuilder eb, int id) {
        eb.setTitle("Crab v2 Auction");
        eb.setImage("https://c.tenor.com/e7FR3EW1CUYAAAAC/trading-places-buy.gif");
        try {
            if(id == -1) {
                Auction latestAuction = v2.FeedingTime.getAuction(v2.FeedingTime.getLatestAuctionId());
                ArrayList<Auction.Bid> bids;

                if(latestAuction == null) {
                    latestAuction = v2.FeedingTime.getAuction(v2.FeedingTime.getLatestAuctionId() - 1);

                    if(latestAuction == null) {
                        eb.setDescription("Something went wrong trying to get the latest auction. Report this issue!");
                        eb.addField("Auction ID Tried To Use", String.valueOf(v2.FeedingTime.getLatestAuctionId()), false);
                        return;
                    }
                }

                bids = Auction.sortedBids(latestAuction);
                eb.setFooter("Latest");

                if(latestAuction.auctionEnd.longValue()/1000 > Instant.now().getEpochSecond()) {
                    eb.setDescription("View detailed info of the current auction\n\n**Direction**: " +
                            (latestAuction.isSelling ? "Selling oSQTH\n" : "Buying oSQTH\n") +
                            "**Size**: " + NumberFormat.getInstance().format(latestAuction.oSqthAmount.doubleValue() / Math.pow(10,18)) + " oSQTH\n" +
                            "**Min. Size**: " + latestAuction.minSize + " oSQTH"
                    );
                } else {
                    eb.setDescription("View detailed info of the previous auction\n\n**Direction**: " +
                            (latestAuction.isSelling ? "Selling oSQTH\n" : "Buying oSQTH\n") +
                            "**Size**: " + NumberFormat.getInstance().format(latestAuction.oSqthAmount.doubleValue() / Math.pow(10,18)) + " oSQTH\n" +
                            "**Min. Size**: " + latestAuction.minSize + " oSQTH"
                    );
                }

                for(int i = 0; i < bids.size(); i++) {
                    StringBuilder title = new StringBuilder("Trader " + (i + 1));
                    for(String winningBidKey: latestAuction.winningBids) {
                        if(latestAuction.bids.get(winningBidKey).equals(bids.get(i))) {
                            title.insert(0, "[INCLUDED] ");
                        }
                    }

                    eb.addField(title.toString(), bids.get(i).toString(),false);
                }
            } else {
                Auction specificAuction = v2.FeedingTime.getAuction((long) id);
                ArrayList<Auction.Bid> bids;

                if(specificAuction == null) {
                    eb.setDescription("Something went wrong trying to get this specific auction! It might not exist, but if you're confident it exists report this issue!");
                    eb.addField("Auction ID Tried To Use", String.valueOf(id), false);
                    return;
                }

                eb.setDescription("View detailed info of the current auction\n\n**Direction**: " +
                        (specificAuction.isSelling ? "Selling oSQTH\n" : "Buying oSQTH\n") +
                        "**Size**: " + NumberFormat.getInstance().format(specificAuction.oSqthAmount.doubleValue() / Math.pow(10,18)) + " oSQTH\n" +
                        "**Min. Size**: " + specificAuction.minSize + " oSQTH"
                );

                eb.setFooter("Vault " + id);
            }
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }
}
