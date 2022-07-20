// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
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
import space.alphaserpentis.squeethdiscordbot.handler.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.LaevitasHandler;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class Crab extends BotCommand {

    private static final String crab = "0xf205ad80bb86ac92247638914265887a8baa437d", oSQTH = "0xf1b99e3e573a1a9c5e6b2ce818b617f0e664e86b", pool = "0x82c427adfdf2d245ec51d8046b41c4ee87f0d29c", ethusdcPool = "0x8ad599c3a0ff1de082011efddc58f1908eb6e6d8", oracle = "0x65d66c76447ccb45daf1e8044e918fa786a483a1", usdc = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", weth = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2";

    private static final Function callVaultsFunc = new Function("getVaultDetails",
            Collections.emptyList(),
            Arrays.asList(
                    new TypeReference<Address>() { },
                    new TypeReference<Uint32>() { },
                    new TypeReference<Uint96>() { },
                    new TypeReference<Uint128>() { }
            )
    );
    private static final Function callUniswapv3PriceCheck = new Function("getTwap",
            Arrays.asList(
                    new org.web3j.abi.datatypes.Address(pool),
                    new org.web3j.abi.datatypes.Address(oSQTH),
                    new org.web3j.abi.datatypes.Address(weth),
                    new Uint32(1),
                    new org.web3j.abi.datatypes.Bool(true)
            ),
            Arrays.asList(
                    new TypeReference<Uint256>() { }
            )
    );
    private static final Function callUniswapv3PriceCheck_USDC = new Function("getTwap",
            Arrays.asList(
                    new org.web3j.abi.datatypes.Address(ethusdcPool),
                    new org.web3j.abi.datatypes.Address(weth),
                    new org.web3j.abi.datatypes.Address(usdc),
                    new Uint32(1),
                    new org.web3j.abi.datatypes.Bool(true)
            ),
            Arrays.asList(
                    new TypeReference<Uint256>() { }
            )
    );
    private static final Function callTotalSupply = new Function("totalSupply",
            Collections.emptyList(),
            Arrays.asList(
                    new TypeReference<Uint256>() { }
            )
    );
    private static final Function callTimeAtLastHedge = new Function("timeAtLastHedge",
            Collections.emptyList(),
            Arrays.asList(
                    new TypeReference<Uint256>() { }
            )
    );

    private static long lastRun = 0, lastRebalanceRun = 0;
    private static BigInteger ethCollateral, shortoSQTH, priceOfoSQTH, priceOfETHinUSD, crabTotalSupply, normFactor;
    private static double ethPerCrab, usdPerCrab, rebalanceEth, rebalanceOsqth;
    private static long lastHedgeTime;
    private static long lastHedgeBlock;
    private static Vault.VaultGreeks lastRunVaultGreeks, preVaultGreeksAtHedge, postVaultGreeksAtHedge;
    private static boolean rebalanceWasAuction;

    public Crab() {
        name = "crab";
        description = "Get current statistics on the Crab strategy!";
        onlyEmbed = true;
        deferReplies = true;

        try {
            updateLastHedge();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    @Override
    public MessageEmbed runCommand(long userId, @Nonnull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        if(event.getSubcommandName() != null) {
            String crabV1Notice = "If you are a depositor in Crab v1, Crab v2 early access is open now!\n\nRead more about it [here](https://twitter.com/opyn_/status/1547331378320121857) and go to https://squeeth.opyn.co/strategies to reserve your spot now!";

            switch(event.getSubcommandName()) {
                case "stats" -> {
                    Vault.VaultGreeks vaultGreeks = lastRunVaultGreeks;
                    if(lastRun + 60 < Instant.now().getEpochSecond()) {
                        try {
                            List<Type> vaultDetails = EthereumRPCHandler.ethCallAtLatestBlock(crab, callVaultsFunc);
                            List<Type> osqthEthPrice = EthereumRPCHandler.ethCallAtLatestBlock(oracle, callUniswapv3PriceCheck);
                            List<Type> ethUsdcPrice = EthereumRPCHandler.ethCallAtLatestBlock(oracle, callUniswapv3PriceCheck_USDC);

                            ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
                            shortoSQTH = (BigInteger) vaultDetails.get(3).getValue();
                            priceOfoSQTH = (BigInteger) osqthEthPrice.get(0).getValue();
                            priceOfETHinUSD = (BigInteger) ethUsdcPrice.get(0).getValue();
                            crabTotalSupply = (BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(crab, callTotalSupply).get(0).getValue();
                            lastHedgeTime = ((BigInteger) EthereumRPCHandler.ethCallAtLatestBlock(crab, callTimeAtLastHedge).get(0).getValue()).longValue();
                            DecimalFormat df = new DecimalFormat("#");

                            normFactor = new BigInteger(String.valueOf(df.format(LaevitasHandler.latestSqueethData.getNormalizationFactor() * (long) Math.pow(10,18))));

                            BigInteger netEth = ethCollateral.subtract(shortoSQTH.multiply(priceOfoSQTH).divide(BigInteger.valueOf((long) Math.pow(10,18))));

                            ethPerCrab = netEth.multiply(BigInteger.valueOf((long) Math.pow(10,18))).divide(crabTotalSupply).doubleValue() / Math.pow(10, 18);
                            usdPerCrab = netEth.multiply(priceOfETHinUSD).divide(crabTotalSupply).doubleValue() / Math.pow(10, 18);

                            lastRun = Instant.now().getEpochSecond();

                            vaultGreeks = new Vault.VaultGreeks(
                                    priceOfETHinUSD.doubleValue() / Math.pow(10,18),
                                    LaevitasHandler.latestSqueethData.getoSQTHPrice(),
                                    normFactor.doubleValue() / Math.pow(10,18),
                                    LaevitasHandler.latestSqueethData.getCurrentImpliedVolatility()/100,
                                    -(shortoSQTH.doubleValue() / Math.pow(10,18)),
                                    ethCollateral.doubleValue() / Math.pow(10,18)
                            );
                            lastRunVaultGreeks = vaultGreeks;
                        } catch (ExecutionException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    NumberFormat instance = NumberFormat.getInstance();

                    eb.setTitle("Crab Statistics");
                    eb.setThumbnail("https://c.tenor.com/3CIbJomibvYAAAAi/crab-rave.gif");
                    eb.setDescription("Get all of your crabby stats here!\n\nhttps://squeeth.com/strategies" + (LaevitasHandler.isDataStale() ? "\n\n**(Data is stale! Calculations may be off!)**" : ""));
                    eb.addField("Notice", crabV1Notice, false);
                    eb.addField("ETH Collateral", instance.format(ethCollateral.divide(BigInteger.valueOf((long) Math.pow(10,18))).doubleValue()) + " Ξ", false);
                    eb.addField("Vault Debt", instance.format(shortoSQTH.divide(BigInteger.valueOf((long) Math.pow(10,18))).doubleValue()) + " oSQTH", false);
                    eb.addField("Collateral Ratio", instance.format(calculateCollateralRatio()) + "%", false);
                    eb.addField("Price per Crab Token", "$" + instance.format(usdPerCrab) + " (" + instance.format(ethPerCrab) + " Ξ)", false);
                    eb.addField("Total Supply of Crab", instance.format(crabTotalSupply.divide(BigInteger.valueOf((long) Math.pow(10,18)))), false);
                    eb.addField("Last Rebalance", "<t:" + lastHedgeTime + ">", false);
                    eb.addField("Δ Delta", "$" + instance.format(vaultGreeks.delta), true);
                    eb.addField("Γ Gamma", "$" + instance.format(vaultGreeks.gamma), true);
                    eb.addBlankField(true);
                    eb.addField("ν Vega", "$" + instance.format(vaultGreeks.vega), true);
                    eb.addField("Θ Theta", "$" + instance.format(vaultGreeks.theta), true);
                    eb.addBlankField(true);
                    eb.addField("Greeks Notice", "*Greeks use some Laevitas data which is polled every 5-minutes*", false);
                    eb.setFooter("Last Updated at " + Instant.ofEpochSecond(lastRun).atOffset(ZoneOffset.UTC).toOffsetTime());
                    eb.setColor(Color.RED);
                }
                case "rebalance" -> {
                    NumberFormat instance = NumberFormat.getInstance();
                    DecimalFormat df = new DecimalFormat("#");

                    eb.setTitle("Crab Rebalance Statistics");
                    eb.setThumbnail("https://c.tenor.com/3CIbJomibvYAAAAi/crab-rave.gif");
                    eb.setDescription("View detailed information that happened during the last rebalance!\n\nParticipate in the auctions: https://www.squeethportal.xyz/auction");
                    eb.addField("Notice", crabV1Notice, false);

                    if(lastRebalanceRun + 60 < Instant.now().getEpochSecond()) {
                        try {
                            updateLastHedge();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        lastRebalanceRun = Instant.now().getEpochSecond();
                    }

                    try {
                        double timestamp = EthereumRPCHandler.web3.ethGetBlockByNumber(new DefaultBlockParameterNumber(lastHedgeBlock), true).send().getBlock().getTimestamp().doubleValue();
                        eb.addField("Last Rebalance", "<t:" + df.format(timestamp) + ">", false);
                        if(rebalanceWasAuction) {
                            eb.addField("Sold", instance.format(rebalanceOsqth) + " oSQTH", false);
                            eb.addField("Received", instance.format(rebalanceEth) + " ETH", false);
                        } else {
                            eb.addField("Bought", instance.format(rebalanceOsqth) + " oSQTH", false);
                            eb.addField("Paid", instance.format(rebalanceEth) + " ETH", false);
                        }
                        eb.addField("Δ Delta", "$" + instance.format(preVaultGreeksAtHedge.delta) + " → $" + instance.format(postVaultGreeksAtHedge.delta), true);
                        eb.addField("Γ Gamma", "$" + instance.format(preVaultGreeksAtHedge.gamma) + " → $" + instance.format(postVaultGreeksAtHedge.gamma), true);
                        eb.addBlankField(true);
                        eb.addField("ν Vega", "$" + instance.format(preVaultGreeksAtHedge.vega) + " → $" + instance.format(postVaultGreeksAtHedge.vega), true);
                        eb.addField("Θ Theta", "$" + instance.format(preVaultGreeksAtHedge.theta) + " → $" + instance.format(postVaultGreeksAtHedge.theta), true);
                        eb.addBlankField(true);
                        eb.addField("Greeks Notice", "Greeks shown here go from pre-rebalance → post-rebalance", false);
                        eb.setColor(Color.RED);
                    } catch (IOException e) {
                        eb.setDescription("An unexpected error has occurred. Please try again later.");
                    }

                }
            }
        }

        return eb.build();
    }

    @Override
    public void addCommand(@Nonnull JDA jda) {
        SubcommandData stats = new SubcommandData("stats", "Regular statistics on Crab");
        SubcommandData rebalance = new SubcommandData("rebalance", "Shows the rebalancing stats");

        Command cmd = jda.upsertCommand(name, description).addSubcommands(stats, rebalance).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@Nonnull JDA jda) {
        SubcommandData stats = new SubcommandData("stats", "Regular statistics on Crab");
        SubcommandData rebalance = new SubcommandData("rebalance", "Shows the rebalancing stats");

        Command cmd = jda.upsertCommand(name, description).addSubcommands(stats, rebalance).complete();

        commandId = cmd.getIdLong();
    }

    public static void updateLastHedge() throws IOException {
        EthFilter filter = new EthFilter(new DefaultBlockParameterNumber(15134805), new DefaultBlockParameterNumber(EthereumRPCHandler.web3.ethBlockNumber().send().getBlockNumber()), crab)
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

            rebalanceWasAuction = (boolean) FunctionReturnDecoder.decodeIndexedValue(data[0], new TypeReference<Bool>() {}).getValue();
            rebalanceEth = ((BigInteger) FunctionReturnDecoder.decodeIndexedValue(data[3], new TypeReference<Uint256>() {}).getValue()).doubleValue() / Math.pow(10,18);
            rebalanceOsqth = ((BigInteger) FunctionReturnDecoder.decodeIndexedValue(data[2], new TypeReference<Uint256>() {}).getValue()).doubleValue() / Math.pow(10,18);
        } else { // OTC Hedge

            String[] data = new String[5];
            String trimmedData = latestLog.get().getData().substring(2);

            for(int i = 0; i < 5; i++) {
                data[i] = trimmedData.substring(64*i, 64*(i+1));
            }

            rebalanceWasAuction = (boolean) FunctionReturnDecoder.decodeIndexedValue(data[0], new TypeReference<Bool>() {}).getValue();
            rebalanceEth = ((BigInteger) FunctionReturnDecoder.decodeIndexedValue(data[4], new TypeReference<Uint256>() {}).getValue()).doubleValue() / Math.pow(10,18);
            rebalanceOsqth = ((BigInteger) FunctionReturnDecoder.decodeIndexedValue(data[3], new TypeReference<Uint256>() {}).getValue()).doubleValue() / Math.pow(10,18);
        }

        try {
            Function callNormFactor = new Function("getExpectedNormalizationFactor",
                    Collections.emptyList(),
                    Arrays.asList(
                            new TypeReference<Uint256>(){}
                    )
            );

            List<Type> vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(crab, callVaultsFunc, lastHedgeBlock - 1);
            List<Type> osqthEthPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3PriceCheck, lastHedgeBlock - 1);
            List<Type> ethUsdcPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3PriceCheck_USDC, lastHedgeBlock - 1);
            BigInteger ethCollateral, shortoSQTH, priceOfoSQTH, priceOfETHinUSD, normFactor;
            double usdPerOsqth, usdPerEth, impliedVolInPercent;

            ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
            shortoSQTH = (BigInteger) vaultDetails.get(3).getValue();
            priceOfoSQTH = (BigInteger) osqthEthPrice.get(0).getValue();
            priceOfETHinUSD = (BigInteger) ethUsdcPrice.get(0).getValue();

            normFactor = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock("0x64187ae08781b09368e6253f9e94951243a493d5", callNormFactor, lastHedgeBlock - 1).get(0).getValue();
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
            vaultDetails = EthereumRPCHandler.ethCallAtSpecificBlock(crab, callVaultsFunc, lastHedgeBlock);
            osqthEthPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3PriceCheck, lastHedgeBlock);
            ethUsdcPrice = EthereumRPCHandler.ethCallAtSpecificBlock(oracle, callUniswapv3PriceCheck_USDC, lastHedgeBlock);

            ethCollateral = (BigInteger) vaultDetails.get(2).getValue();
            shortoSQTH = (BigInteger) vaultDetails.get(3).getValue();
            priceOfoSQTH = (BigInteger) osqthEthPrice.get(0).getValue();
            priceOfETHinUSD = (BigInteger) ethUsdcPrice.get(0).getValue();

            normFactor = (BigInteger) EthereumRPCHandler.ethCallAtSpecificBlock("0x64187ae08781b09368e6253f9e94951243a493d5", callNormFactor, lastHedgeBlock).get(0).getValue();
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

    private double calculateCollateralRatio() {
        BigInteger debt = shortoSQTH.multiply(priceOfETHinUSD).multiply(normFactor).divide(BigInteger.valueOf(10000));
        // Divide by 10^36 of debt to get the correctly scaled debt
        return ethCollateral.doubleValue() / (debt.doubleValue() / Math.pow(10,36)) * 100;
    }
}
