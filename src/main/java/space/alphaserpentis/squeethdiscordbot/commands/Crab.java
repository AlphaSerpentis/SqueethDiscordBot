// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import space.alphaserpentis.squeethdiscordbot.data.api.PriceData;
import space.alphaserpentis.squeethdiscordbot.data.api.squeethportal.Auction;
import space.alphaserpentis.squeethdiscordbot.data.api.squeethportal.LatestCrabAuctionResponse;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses;
import space.alphaserpentis.squeethdiscordbot.data.ethereum.CommonFunctions;
import space.alphaserpentis.squeethdiscordbot.data.server.ServerData;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.servers.ServerDataHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.PositionsDataHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.squeeth.AuctionHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.squeeth.JumboHandler;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.squeeth.LaevitasHandler;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import java.awt.*;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Squeeth.crabv1;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.Addresses.Squeeth.crabv2;
import static space.alphaserpentis.squeethdiscordbot.data.ethereum.CommonFunctions.*;

public class Crab extends ButtonCommand<MessageEmbed> {

    public static abstract class CrabVault {
        public static final Function callTimeAtLastHedge = new Function("timeAtLastHedge",
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
            @NonNull String address
        ) {
            this.address = address;
        }

        public abstract void updateLastHedge() throws IOException;
        public double calculateCollateralRatio() {
            BigInteger debt = shortOsqth.multiply(priceOfEthInUsd).multiply(normFactor).divide(BigInteger.valueOf(10000));
            // Divide by 10^36 of debt to get the correctly scaled debt
            return ethCollateral.doubleValue() / (debt.doubleValue() / Math.pow(10,36)) * 100;
        }
    }
    public static class v1 extends CrabVault {
        public v1() {
            super(crabv1);
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
                long block = lastHedgeBlock - 1;
                List<Type> vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(address, getVaultDetails, block);
                PriceData priceData = PositionsDataHandler.getPriceData(block, new PriceData.Prices[]{PriceData.Prices.OSQTHETH, PriceData.Prices.ETHUSD, PriceData.Prices.NORMFACTOR});
                BigInteger ethCollateral, shortoSQTH, priceOfoSQTH, priceOfETHinUSD;
                double usdPerOsqth, usdPerEth, impliedVolInPercent;

                ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
                shortoSQTH = (BigInteger) vaultDetails.get(3).getValue();
                priceOfoSQTH = priceData.osqthEth;
                priceOfETHinUSD = priceData.ethUsdc;
                normFactor = priceData.normFactor;

                usdPerOsqth = priceOfoSQTH.multiply(priceOfETHinUSD).divide(BigInteger.valueOf((long) Math.pow(10,18))).doubleValue() / Math.pow(10,18);
                usdPerEth = priceOfETHinUSD.doubleValue() / Math.pow(10,18);

                impliedVolInPercent = Math.sqrt(Math.log(priceOfoSQTH.doubleValue() / Math.pow(10,18) * 10000/(priceData.normFactor.doubleValue() / Math.pow(10,18) * usdPerEth))/(17.5/365));

                preVaultGreeksAtHedge = new Vault.VaultGreeks(
                        priceOfETHinUSD.doubleValue() / Math.pow(10,18),
                        usdPerOsqth,
                        priceData.normFactor.doubleValue() / Math.pow(10,18),
                        impliedVolInPercent,
                        -(shortoSQTH.doubleValue() / Math.pow(10,18)),
                        ethCollateral.doubleValue() / Math.pow(10,18)
                );

                // get post data
                block = lastHedgeBlock;
                priceData = PositionsDataHandler.getPriceData(block, new PriceData.Prices[]{PriceData.Prices.OSQTHETH, PriceData.Prices.ETHUSD, PriceData.Prices.NORMFACTOR});
                vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(address, getVaultDetails, block);

                ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
                shortoSQTH = (BigInteger) vaultDetails.get(3).getValue();
                priceOfoSQTH = priceData.osqthEth;
                priceOfETHinUSD = priceData.ethUsdc;
                normFactor = priceData.normFactor;

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

        public static class FeedingTime {

            enum NotificationPhase {
                AUCTION_NOT_ACTIVE,
                SIXTY_MINUTES,
                THIRTY_MINUTES,
                TEN_MINUTES,
                AUCTION_ACTIVE
            }

            enum AuctionType {
                CRAB ("Crab"),
                JUMBO_CRAB ("Jumbo Crab"),
                ZEN_BULL ("Zen Bull");

                private final String properName;

                AuctionType(String properName) {
                    this.properName = properName;
                }

                @Override
                public String toString() {
                    return properName;
                }
            }

            public record EstimateSizeOfAuction(
                    double ethSize,
                    double osqthSize,
                    double gamma,
                    double vega,
                    boolean isBuyingOsqth
            ) {}

            public static final ArrayList<Long> serversListening = new ArrayList<>();
            private static ScheduledExecutorService scheduledExecutor;
//            private static ScheduledFuture<?> notificationFuture = null;
            private static ScheduledFuture<?> updateBidsFuture = null;
//            private static ScheduledFuture<?> settlementFuture = null;
//            private static EmbedBuilder settlementMessage = null;
//            private static Auction currentAuction = null;
            public static long auctionTime;
            public static NotificationPhase notificationPhase;
            public static AuctionType auctionType;

            public FeedingTime() {
                scheduledExecutor = Executors.newScheduledThreadPool(3);

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

                TimeZone timeZone = TimeZone.getTimeZone("UTC");
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
                        timeThen = auctionDay.atTime(16,30).atZone(timeZone.toZoneId()).toEpochSecond();
                        auctionType = AuctionType.CRAB;
                    }
                    case Calendar.MONDAY -> {
                        timeThen = today.atTime(16,30).atZone(timeZone.toZoneId()).toEpochSecond();
                        auctionType = AuctionType.CRAB;

                        if(timeNow >= timeThen) {
                            ta = TemporalAdjusters.next(DayOfWeek.TUESDAY);
                            auctionDay = today.with(ta);
                            timeThen = auctionDay.atTime(16,30).atZone(timeZone.toZoneId()).toEpochSecond();
                            auctionType = AuctionType.JUMBO_CRAB;
                        }
                    }
                    case Calendar.TUESDAY -> {
                        timeThen = today.atTime(16,30).atZone(timeZone.toZoneId()).toEpochSecond();
                        auctionType = AuctionType.JUMBO_CRAB;

                        if(timeNow >= timeThen) {
                            ta = TemporalAdjusters.next(DayOfWeek.WEDNESDAY);
                            auctionDay = today.with(ta);
                            timeThen = auctionDay.atTime(16,30).atZone(timeZone.toZoneId()).toEpochSecond();
                            auctionType = AuctionType.CRAB;
                        }
                    }
                    case Calendar.WEDNESDAY -> {
                        timeThen = today.atTime(16,30).atZone(timeZone.toZoneId()).toEpochSecond();
                        auctionType = AuctionType.CRAB;

                        if(timeNow >= timeThen) {
                            ta = TemporalAdjusters.next(DayOfWeek.FRIDAY);
                            auctionDay = today.with(ta);
                            timeThen = auctionDay.atTime(16,30).atZone(timeZone.toZoneId()).toEpochSecond();
                        }
                    }
                    case Calendar.THURSDAY -> {
                        ta = TemporalAdjusters.next(DayOfWeek.FRIDAY);
                        auctionDay = today.with(ta);
                        timeThen = auctionDay.atTime(16,30).atZone(timeZone.toZoneId()).toEpochSecond();
                        auctionType = AuctionType.CRAB;
                    }
                    case Calendar.FRIDAY -> {
                        timeThen = today.atTime(16,30).atZone(timeZone.toZoneId()).toEpochSecond();
                        auctionType = AuctionType.CRAB;

                        if(timeNow >= timeThen) {
                            ta = TemporalAdjusters.next(DayOfWeek.MONDAY);
                            auctionDay = today.with(ta);
                            timeThen = auctionDay.atTime(16,30).atZone(timeZone.toZoneId()).toEpochSecond();
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
                    } else {
                        scheduledExecutor.schedule(FeedingTime::prepareNotification, timeDiff, TimeUnit.SECONDS);
                        notificationPhase = NotificationPhase.TEN_MINUTES;
                    }
                } else if(timeDiff <= 0 && notificationPhase == NotificationPhase.AUCTION_NOT_ACTIVE) { // out-of-date auction time
                    timeUntilNextAuction();
                    prepareNotification(); // check again
                    return;
                } else if(timeDiff <= 0 && timeDiff > -600) {
                    scheduledExecutor.schedule(FeedingTime::prepareNotification, timeDiff + 600, TimeUnit.SECONDS);
                    notificationPhase = NotificationPhase.AUCTION_ACTIVE;
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
                NumberFormat cf = NumberFormat.getCurrencyInstance(Locale.US);
                EstimateSizeOfAuction sizeOfAuction;
                String auctionTypeTitle;
                String approxSizeOfAuction = null;
                String greeksTradedSection;

                try {
                    sizeOfAuction = estimateSizeOfAuction(auctionType);
                } catch(IllegalStateException e) {
                    throw new RuntimeException();
                }

                auctionTypeTitle = auctionType + " Auction";

                if(auctionType == AuctionType.CRAB || auctionType == AuctionType.ZEN_BULL)
                    eb.addField("Notice", "The strategy may or may not rebalance in-between the scheduled auctions or rebalance at all", false);

                switch(notificationPhase) {
                    case SIXTY_MINUTES -> eb.setTitle(auctionTypeTitle + " (One Hour Notice)");
                    case THIRTY_MINUTES -> eb.setTitle(auctionTypeTitle + " (Thirty Minute Notice)");
                    case TEN_MINUTES -> eb.setTitle(auctionTypeTitle + " (Ten Minute Notice)");
                    case AUCTION_ACTIVE -> eb.setTitle(auctionTypeTitle + " is Live!");
                    case AUCTION_NOT_ACTIVE -> eb.setTitle("Next Auction Date");
                }

                if(notificationPhase != NotificationPhase.AUCTION_ACTIVE && notificationPhase != NotificationPhase.AUCTION_NOT_ACTIVE) {
                    PriceData priceData;
                    double deltaTraded, gammaTraded, vegaTraded, ethPrice;
                    short multiplier;

                    try {
                        priceData = PositionsDataHandler.getPriceData(new PriceData.Prices[]{PriceData.Prices.ETHUSD, PriceData.Prices.OSQTHETH, PriceData.Prices.NORMFACTOR, PriceData.Prices.SQUEETHVOL});
                        ethPrice = priceData.ethUsdc.doubleValue() / Math.pow(10,18);
                    } catch (ExecutionException | InterruptedException | IOException e) {
                        throw new RuntimeException(e);
                    }

                    deltaTraded = Math.abs(2 * sizeOfAuction.ethSize * ethPrice);
                    gammaTraded = Math.abs(sizeOfAuction.gamma * Math.pow(ethPrice, 2) / 100);
                    vegaTraded = Math.abs(sizeOfAuction.vega);

                    greeksTradedSection = "Delta Traded: " + cf.format(deltaTraded) +
                            "\nGamma Traded: " + cf.format(gammaTraded) +
                            "\nVega Traded: " + cf.format(vegaTraded);

                    if(auctionType == AuctionType.CRAB) {
                        multiplier = -1;
                    } else if(auctionType == AuctionType.JUMBO_CRAB) {
                        multiplier = 1;
                    } else {
                        multiplier = 0;
                    }

                    if(sizeOfAuction.isBuyingOsqth) { // Buying oSQTH, Selling ETH
                        approxSizeOfAuction = "Approximately, " + auctionType + " is buying " + format.format(sizeOfAuction.osqthSize) + " oSQTH for " + format.format(multiplier * sizeOfAuction.ethSize) + " ETH\n\n" + greeksTradedSection;
                    } else { // Selling oSQTH, Buying ETH
                        approxSizeOfAuction = "Approximately, " + auctionType + " is selling " + format.format(multiplier * sizeOfAuction.osqthSize) + " oSQTH for " + format.format(sizeOfAuction.ethSize) + " ETH\n\n" + greeksTradedSection;
                    }
                }

                if(notificationPhase == NotificationPhase.AUCTION_ACTIVE) {
                    eb.setDescription(auctionTypeTitle + " is currently active! Users can place bids at https://squeethportal.xyz/auction");
                    updateMessageForBids();
                } else if(notificationPhase != NotificationPhase.AUCTION_NOT_ACTIVE) {
                    eb.setDescription("In <t:" + auctionTime + ":R>, " + auctionType + " will start an auction! Users can check out the current stats at https://squeethportal.xyz/auction\n\n" + approxSizeOfAuction);
                } else {
                    eb.setDescription("At <t:" + auctionTime + ">, " + auctionType + " will prepare an auction!");
                }

                for(Long serverId: serversListening) {
                    ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);
                    Guild guild = Launcher.api.getGuildById(serverId);
                    final long previousNotificationId = sd.getLastCrabAuctionNotificationId();
                    if(sd.getCrabAuctionChannelId() == 0 || !sd.getListenToCrabAuctions() || guild == null) { // ineligible to send
                        break;
                    }

                    guild.getTextChannelById(sd.getCrabAuctionChannelId()).sendMessageEmbeds(eb.build()).queue(
                            (response) -> {
                                if(previousNotificationId != 0)
                                    response.getChannel().asTextChannel().deleteMessageById(previousNotificationId).queue();
                                sd.setLastCrabAuctionNotificationId(response.getIdLong());
                                try {
                                    ServerDataHandler.updateServerData();
                                } catch (IOException ignored) {

                                }
                            }
                    );
                }
            }

            public static void updateMessageForBids() {
                AtomicInteger runs = new AtomicInteger();

                try {
//                    if(getLatestActiveAuctionId() != -1) {
//                        currentAuction = getLatestAuction().auction;
//                    } else {
//                        scheduledExecutor.schedule(FeedingTime::updateMessageForBids, 10, TimeUnit.SECONDS);
//                    }

                    if(AuctionHandler.getLatestActiveAuctionId() == -1) {
                        scheduledExecutor.schedule(FeedingTime::updateMessageForBids, 10, TimeUnit.SECONDS);
                        return;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                updateBidsFuture = scheduledExecutor.scheduleAtFixedRate(() -> {
                    EmbedBuilder eb = new EmbedBuilder();

                    for(Long serverId: serversListening) {
                        ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);

                        if(notificationPhase != NotificationPhase.AUCTION_ACTIVE) { // check if the auction is running or not
                            cleanBidMessages(sd);
//                            if(notificationPhase == NotificationPhase.AUCTION_SETTLING && settlementFuture == null) { // double check if it's actually settling
//                                listenForSettlement(sd);
//                            }
                            return;
                        }

                        TextChannel channel = Launcher.api.getTextChannelById(ServerDataHandler.serverDataHashMap.get(serverId).getCrabAuctionChannelId());
                        if(runs.getAndIncrement() == 8) { // allow up to 9 runs
                            cleanBidMessages(sd);
                            return;
                        } else {
                            final long previousBidMessageId = sd.getLastCrabAuctionBidMessageId();
                            bidsPage(eb, -1);

                            if(previousBidMessageId == 0) { // make new bid message
                                channel.sendMessageEmbeds(eb.build()).queue(
                                        (response) -> sd.setLastCrabAuctionBidMessageId(response.getIdLong())
                                );
                            } else { // edit bid message
                                channel.editMessageEmbedsById(previousBidMessageId, eb.build()).queue(
                                        (ignored) -> {},
                                        (error) -> sd.setLastCrabAuctionBidMessageId(0)
                                );
                            }
                        }
                    }
                },0,1,TimeUnit.MINUTES);
            }

            private static void cleanBidMessages(@NonNull ServerData sd) {
                for(Long serverId: serversListening) {
                    TextChannel channel = Launcher.api.getTextChannelById(ServerDataHandler.serverDataHashMap.get(serverId).getCrabAuctionChannelId());

                    channel.deleteMessageById(sd.getLastCrabAuctionBidMessageId()).queue();
                    sd.setLastCrabAuctionBidMessageId(0);
                }

                try {
                    ServerDataHandler.updateServerData();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                updateBidsFuture.cancel(false);
            }

//            private static void listenForSettlement(@Nonnull ServerData sd) {
//                long currentBlock;
//                try {
//                    currentBlock = EthereumRPCHandler.web3.ethBlockNumber().send().getBlockNumber().longValue();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//
//                settlementFuture = scheduledExecutor.scheduleWithFixedDelay(() -> {
//                    EthFilter filter;
//                    try {
//                        filter = new EthFilter(new DefaultBlockParameterNumber(currentBlock), new DefaultBlockParameterNumber(EthereumRPCHandler.web3.ethBlockNumber().send().getBlockNumber()), crabV2.address)
//                                .addOptionalTopics("0xbbc3ba742efe346cfdf333000069964e0ee3087c68da267dac977d299f2366fb");
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//
//                    Flowable<Log> logFlowable = EthereumRPCHandler.web3.ethLogFlowable(filter);
//                    AtomicReference<Log> latestLog = new AtomicReference<>();
//
//                    Disposable disposable = logFlowable.subscribe(
//                            latestLog::set
//                    );
//
//                    disposable.dispose();
//
//                    if(latestLog.get() != null) { // check if last log is valid
//                        if(notificationFuture.cancel(false)) {
//                            EmbedBuilder eb = new EmbedBuilder();
//                            bidsPage(eb, currentAuction.currentAuctionId);
//
//                            for(Long serverId: serversListening) {
//                                TextChannel channel = Launcher.api.getTextChannelById(ServerDataHandler.serverDataHashMap.get(serverId).getCrabAuctionChannelId());
//
//                                channel.sendMessageEmbeds(eb.build()).queue(
//                                        (response) -> {
//                                            if(sd.getLastCrabAuctionNotificationId() != 0)
//                                                response.getChannel().asTextChannel().deleteMessageById(sd.getLastCrabAuctionNotificationId()).queue(
//                                                        (ignored) -> {},
//                                                        Throwable::printStackTrace
//                                                );
//                                            sd.setLastCrabAuctionNotificationId(response.getIdLong());
//                                        },
//                                        Throwable::printStackTrace
//                                );
//                                notificationPhase = NotificationPhase.AUCTION_NOT_ACTIVE;
//                                scheduledExecutor.schedule(FeedingTime::prepareNotification, timeUntilNextAuction() - 3600, TimeUnit.SECONDS);
//                                notifyAboutAuction();
//                            }
//                        }
//
//                        throw new RuntimeException();
//                    }
//                }, 0, 30, TimeUnit.SECONDS);
//
//                scheduledExecutor.schedule(() -> settlementFuture.cancel(false), 10, TimeUnit.MINUTES);
//            }

            @SuppressWarnings("rawtypes")
            public static EstimateSizeOfAuction estimateSizeOfAuction(@NonNull AuctionType auctionType) {
                double[] sizes = new double[4];
                long currentBlock;
                double impliedVol, ethUsd, osqthEth, osqthUsd, normFactor;
                boolean isBuyingOsqth;
                PriceData priceData;

                try {
                    currentBlock = EthereumRPCHandler.getLatestBlockNumber().longValue();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if(auctionType == AuctionType.CRAB) {
                    // Get info
                    BigInteger osqthHoldings, ethVaultCollateral;

                    try {
                        priceData = PositionsDataHandler.getPriceData(currentBlock, new PriceData.Prices[]{PriceData.Prices.OSQTHETH, PriceData.Prices.ETHUSD, PriceData.Prices.NORMFACTOR});
                        List<Type> vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(crabV2.address, getVaultDetails, currentBlock);

                        ethVaultCollateral = (BigInteger) vaultDetails.get(2).getValue();
                        osqthHoldings = (BigInteger) vaultDetails.get(3).getValue();
                        osqthEth = priceData.osqthEth.doubleValue() / Math.pow(10,18);
                        ethUsd = priceData.ethUsdc.doubleValue() / Math.pow(10,18);
                        normFactor = priceData.normFactor.doubleValue() / Math.pow(10,18);
                        osqthUsd = osqthEth * ethUsd;
                        impliedVol = Math.sqrt(Math.log(osqthEth * SCALING_FACTOR/(normFactor * (ethUsd)))/FUNDING_PERIOD);

                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    // Calculate delta
                    Vault.VaultGreeks greeks = new Vault.VaultGreeks(
                            ethUsd,
                            osqthUsd,
                            normFactor,
                            impliedVol,
                            -osqthHoldings.doubleValue() / Math.pow(10,18),
                            ethVaultCollateral.doubleValue() / Math.pow(10,18)
                    );

                    double delta = greeks.delta;

                    sizes[0] = delta;
                    sizes[1] = -delta/(osqthEth);

                    double v = sizes[1] / (osqthHoldings.doubleValue() / Math.pow(10, 18));

                    sizes[2] = greeks.gamma * v;
                    sizes[3] = greeks.vega * v / 100;

                    isBuyingOsqth = sizes[0] < 0;
                } else if(auctionType == AuctionType.JUMBO_CRAB) {
                    try {
                        priceData = PositionsDataHandler.getPriceData(currentBlock, new PriceData.Prices[]{PriceData.Prices.OSQTHETH, PriceData.Prices.CRABV2ETH, PriceData.Prices.ETHUSD, PriceData.Prices.NORMFACTOR});
                        double remainingUsdc, remainingCrab, crabUsd, osqthHoldings, crabTotalSupply;
                        JumboHandler.JumboCrabStatistics jumboCrabStatistics = JumboHandler.getCurrentJumboCrabStatistics();
                        JumboHandler.JumboCrabNettingEstimates nettingEstimates = JumboHandler.getNettingEstimates(
                                jumboCrabStatistics,
                                priceData.crabV2Eth.multiply(priceData.ethUsdc).doubleValue() / Math.pow(10,36)
                        );
                        List<Type> vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(crabV2.address, getVaultDetails, currentBlock);

                        remainingUsdc = Math.round(jumboCrabStatistics.pendingUsdc() - nettingEstimates.usdcAmountNetted());
                        remainingCrab = Math.round(jumboCrabStatistics.pendingCrabTokens() - nettingEstimates.crabAmountNetted());
                        osqthHoldings = ((BigInteger) vaultDetails.get(3).getValue()).doubleValue() / Math.pow(10,18);
                        crabTotalSupply = ((BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(crabV2.address, callTotalSupply, currentBlock).get(0).getValue()).doubleValue() / Math.pow(10,18);
                        osqthEth = priceData.osqthEth.doubleValue() / Math.pow(10,18);
                        ethUsd = priceData.ethUsdc.doubleValue() / Math.pow(10,18);
                        normFactor = priceData.normFactor.doubleValue() / Math.pow(10,18);
                        osqthUsd = osqthEth * ethUsd;
                        crabUsd = priceData.crabV2Eth.doubleValue() / Math.pow(10,18) * ethUsd;
                        impliedVol = Math.sqrt(Math.log(osqthEth * SCALING_FACTOR/(normFactor * (ethUsd)))/FUNDING_PERIOD);

                        if(remainingUsdc > 0) {
                            sizes[1] = osqthHoldings * ((remainingUsdc / crabUsd) / crabTotalSupply);
                            isBuyingOsqth = false;
                        } else if(remainingCrab > 0) {
                            sizes[1] = osqthHoldings * remainingCrab / crabTotalSupply;
                            isBuyingOsqth = true;
                        } else {
                            throw new IllegalStateException("No remaining USDC or Crab");
                        }

                        sizes[0] = sizes[1] * osqthEth;

                        Vault.VaultGreeks greeks = new Vault.VaultGreeks(
                                ethUsd,
                                osqthUsd,
                                normFactor,
                                impliedVol,
                                sizes[1],
                                sizes[1] * osqthEth
                        );

                        sizes[2] = greeks.gamma;
                        sizes[3] = greeks.vega / 100;
                    } catch(ExecutionException | InterruptedException | IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    throw new IllegalStateException("No remaining");
                }
                return new EstimateSizeOfAuction(
                        sizes[0],
                        sizes[1],
                        sizes[2],
                        sizes[3],
                        isBuyingOsqth
                );
            }

            public static long getAuctionTime() {
                return auctionTime;
            }
        }

        public v2() {
            super(crabv2);
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
                long block = lastHedgeBlock - 1;
                PriceData priceData = PositionsDataHandler.getPriceData(block, new PriceData.Prices[]{PriceData.Prices.OSQTHETH, PriceData.Prices.ETHUSD, PriceData.Prices.NORMFACTOR});
                List<Type> vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(address, getVaultDetails, block);
                BigInteger ethCollateral, shortoSQTH, priceOfoSQTH, priceOfETHinUSD, normFactor;
                double usdPerOsqth, usdPerEth, impliedVolInPercent;

                ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
                shortoSQTH = (BigInteger) vaultDetails.get(3).getValue();
                priceOfoSQTH = priceData.osqthEth;
                priceOfETHinUSD = priceData.ethUsdc;
                normFactor = priceData.normFactor;

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
                block = lastHedgeBlock;
                priceData = PositionsDataHandler.getPriceData(block, new PriceData.Prices[]{PriceData.Prices.OSQTHETH, PriceData.Prices.ETHUSD, PriceData.Prices.NORMFACTOR});
                vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(address, getVaultDetails, block);

                ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
                shortoSQTH = (BigInteger) vaultDetails.get(3).getValue();
                priceOfoSQTH = priceData.osqthEth;
                priceOfETHinUSD = priceData.ethUsdc;
                normFactor = priceData.normFactor;

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

    public static CrabVault crabV1, crabV2;

    public Crab() {
        super(new BotCommandOptions(
           "crab",
           "Get current statistics on the Crab strategy!",
           0,
           0,
           true,
           false,
           TypeOfEphemeral.DYNAMIC,
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
    public void runButtonInteraction(@NonNull ButtonInteractionEvent event) {
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

    @NonNull
    @Override
    public Collection<ItemComponent> addButtons(@NonNull GenericCommandInteractionEvent event) {
        if(event.getSubcommandName().equalsIgnoreCase("bids") && event.getOptions().size() == 0) {
            return Arrays.asList(new ItemComponent[]{getButton("Refresh")});
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        CrabVault crab;

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
                    bidsPage(eb, event.getOptions().get(0).getAsInt());
                }
            }
        }

        return new CommandResponse<>(eb.build(), false);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
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
    @NonNull
    public CommandResponse<MessageEmbed> beforeRunCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        if(event.getSubcommandName().equalsIgnoreCase("bids")) {
            return new CommandResponse<>(null, true);
        }

        return new CommandResponse<>(null, false);
    }

    private void statsPage(@NonNull EmbedBuilder eb, @NonNull CrabVault crab) {
        if(crab.lastRun + 60 < Instant.now().getEpochSecond()) {
            try {
                update(crab);
            } catch (ExecutionException | InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        Vault.VaultGreeks vaultGreeks = crab.lastRunVaultGreeks;
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

    private void rebalancePage(@NonNull EmbedBuilder eb, @NonNull CrabVault crab) {
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
            double ethPriceAtRebalance = ((BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock(Addresses.Uniswap.oracle, CommonFunctions.getTwap_ethUsd, crab.lastHedgeBlock).get(0).getValue()).doubleValue() / Math.pow(10,18);
            eb.addField("Last Rebalance", "<t:" + df.format(timestamp) + ">", false);
            if(crab.rebalanceSoldOsqth) {
                eb.addField("Sold", instance.format(crab.rebalancedOsqth) + " oSQTH", false);
                eb.addField("Received", instance.format(crab.rebalancedEth) + " Ξ", false);
            } else {
                eb.addField("Bought", instance.format(crab.rebalancedOsqth) + " oSQTH", false);
                eb.addField("Paid", instance.format(crab.rebalancedEth) + " Ξ", false);
            }
            eb.addField("Clearing Price", instance.format(crab.rebalancedEth/crab.rebalancedOsqth) + " Ξ", false);
            if(crab instanceof v2) {
                eb.addField("Upcoming Auction", "<t:" + v2.FeedingTime.getAuctionTime() + ">", false);
            }
            eb.addField("ETH Price at Rebalance", "$" + instance.format(ethPriceAtRebalance), false);
            eb.addField("Price Hedge Threshold", "$" + instance.format(ethPriceAtRebalance * 0.8) + "/" + instance.format(ethPriceAtRebalance * 1.2), false);
            eb.addField("Δ Delta", "$" + instance.format(crab.preVaultGreeksAtHedge.delta) + " → $" + instance.format(crab.postVaultGreeksAtHedge.delta), true);
            eb.addField("Γ Gamma", "$" + instance.format(crab.preVaultGreeksAtHedge.gamma) + " → $" + instance.format(crab.postVaultGreeksAtHedge.gamma), true);
            eb.addBlankField(true);
            eb.addField("ν Vega", "$" + instance.format(crab.preVaultGreeksAtHedge.vega) + " → $" + instance.format(crab.postVaultGreeksAtHedge.vega), true);
            eb.addField("Θ Theta", "$" + instance.format(crab.preVaultGreeksAtHedge.theta) + " → $" + instance.format(crab.postVaultGreeksAtHedge.theta), true);
            eb.addBlankField(true);
            eb.addField("Greeks Notice", "Greeks shown here go from pre-rebalance → post-rebalance", false);
            eb.addField("Price Hedge Notice", "Price hedges will only be initiated if the price of Ethereum breaches the threshold and stays at or past it for a set amount of time.", false);
            eb.setColor(Color.RED);
        } catch (IOException e) {
            eb.setDescription("An unexpected error has occurred. Please try again later.");
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void bidsPage(@NonNull EmbedBuilder eb, long id) {
        eb.setTitle("Crab v2 Auction");
        eb.setThumbnail("https://c.tenor.com/e7FR3EW1CUYAAAAC/trading-places-buy.gif");
        try {
            if (id == -1) {
                LatestCrabAuctionResponse auctionResponse = AuctionHandler.getLatestAuction();
                Auction auction;
                ArrayList<Auction.Bid> bids;
                double clearingPrice = -1;

                if (auctionResponse == null) {
                    eb.setDescription("Something went wrong trying to get the latest auction. Report this issue!");
                    eb.setColor(Color.RED);
                    return;
                } else if (!auctionResponse.isLive) {
                    auction = AuctionHandler.getAuction(auctionResponse.auction.currentAuctionId - 1);
                    if (auction == null) {
                        eb.setDescription("Something went wrong trying to get the latest auction. Report this issue!");
                        eb.setColor(Color.RED);
                        return;
                    }
                } else {
                    auction = auctionResponse.auction;
                }

                bids = Auction.sortedBids(auction);
                eb.setFooter("Latest");

                for (int i = 0; i < bids.size(); i++) {
                    StringBuilder title = new StringBuilder("Trader " + (i + 1));
                    for (String winningBidKey : auction.winningBids) {
                        if (auction.bids.get(winningBidKey).equals(bids.get(i))) {
                            title.insert(0, "[INCLUDED] ");
                            clearingPrice = Double.parseDouble(String.format("%.5f", bids.get(i).order.price.doubleValue() / Math.pow(10, 18)));
                        }
                    }

                    eb.addField(title.toString(), bids.get(i).toString(), false);
                }

                if (auction.auctionEnd.longValue() / 1000 > Instant.now().getEpochSecond()) {
                    eb.setDescription("View detailed info of the current auction\n\n**Direction**: " +
                            (auction.isSelling ? "Selling oSQTH\n" : "Buying oSQTH\n") +
                            "**Size**: " + NumberFormat.getInstance().format(auction.oSqthAmount.doubleValue() / Math.pow(10, 18)) + " oSQTH\n" +
                            "**Min. Size**: " + auction.minSize + " oSQTH\n" +
                            "**Clearing Price**: " + (clearingPrice == -1 ? "Undetermined" : clearingPrice + " ETH")
                    );
                } else {
                    eb.setDescription("View detailed info of the previous auction\n\n**Direction**: " +
                            (auction.isSelling ? "Selling oSQTH\n" : "Buying oSQTH\n") +
                            "**Size**: " + NumberFormat.getInstance().format(auction.oSqthAmount.doubleValue() / Math.pow(10, 18)) + " oSQTH\n" +
                            "**Min. Size**: " + auction.minSize + " oSQTH\n" +
                            "**Clearing Price**: " + (clearingPrice == -1 ? "Undetermined" : clearingPrice + " ETH")
                    );
                }
            } else {
                Auction specificAuction = AuctionHandler.getAuction(id);
                ArrayList<Auction.Bid> bids;

                if (specificAuction == null) {
                    eb.setDescription("Something went wrong trying to get this specific auction! It might not exist, but if you're confident it exists report this issue!");
                    eb.addField("Auction ID Tried To Use", String.valueOf(id), false);
                    eb.setColor(Color.RED);
                    return;
                }

                bids = Auction.sortedBids(specificAuction);
                double clearingPrice = 0;

                for (int i = 0; i < bids.size(); i++) {
                    StringBuilder title = new StringBuilder("Trader " + (i + 1));
                    for (String winningBidKey : specificAuction.winningBids) {
                        if (specificAuction.bids.get(winningBidKey).equals(bids.get(i))) {
                            title.insert(0, "[INCLUDED] ");
                            clearingPrice = Double.parseDouble(String.format("%.5f", bids.get(i).order.price.doubleValue() / Math.pow(10, 18)));
                        }
                    }

                    eb.addField(title.toString(), bids.get(i).toString(), false);
                }

                eb.setDescription("View detailed info of the auction #" + id + "\n\n**Direction**: " +
                        (specificAuction.isSelling ? "Selling oSQTH\n" : "Buying oSQTH\n") +
                        "**Size**: " + NumberFormat.getInstance().format(specificAuction.oSqthAmount.doubleValue() / Math.pow(10, 18)) + " oSQTH\n" +
                        "**Min. Size**: " + specificAuction.minSize + " oSQTH\n" +
                        "**Clearing Price**: " + clearingPrice + " ETH"
                );

                eb.setFooter("Vault " + id);
            }
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("rawtypes")
    public static void update(@NonNull CrabVault crab) throws IOException, ExecutionException, InterruptedException {
        PriceData priceData = PositionsDataHandler.getPriceData(new PriceData.Prices[]{PriceData.Prices.OSQTHETH, PriceData.Prices.ETHUSD, PriceData.Prices.NORMFACTOR});
        List<Type> vaultDetails = EthereumRPCHandler.ethCallAtLatestBlock(crab.address, getVaultDetails);

        crab.ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
        crab.shortOsqth = (BigInteger) vaultDetails.get(3).getValue();
        BigInteger priceOfoSQTH = priceData.osqthEth;
        crab.priceOfEthInUsd = priceData.ethUsdc;
        crab.tokenSupply = (BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(crab.address, callTotalSupply).get(0).getValue();
        crab.lastHedgeTime = ((BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(crab.address, CrabVault.callTimeAtLastHedge).get(0).getValue()).longValue();

        crab.normFactor = priceData.normFactor;

        BigInteger netEth = crab.ethCollateral.subtract(crab.shortOsqth.multiply(priceOfoSQTH).divide(BigInteger.valueOf((long) Math.pow(10,18))));

        crab.ethPerToken = netEth.multiply(BigInteger.valueOf((long) Math.pow(10,18))).divide(crab.tokenSupply).doubleValue() / Math.pow(10, 18);
        crab.usdPerToken = netEth.multiply(crab.priceOfEthInUsd).divide(crab.tokenSupply).doubleValue() / Math.pow(10, 18);

        crab.lastRun = Instant.now().getEpochSecond();

        crab.lastRunVaultGreeks = new Vault.VaultGreeks(
                crab.priceOfEthInUsd.doubleValue() / Math.pow(10, 18),
                LaevitasHandler.latestSqueethData.data.getoSQTHPrice(),
                crab.normFactor.doubleValue() / Math.pow(10, 18),
                LaevitasHandler.latestSqueethData.data.getCurrentImpliedVolatility() / 100,
                -(crab.shortOsqth.doubleValue() / Math.pow(10, 18)),
                crab.ethCollateral.doubleValue() / Math.pow(10, 18)
        );
    }
}
