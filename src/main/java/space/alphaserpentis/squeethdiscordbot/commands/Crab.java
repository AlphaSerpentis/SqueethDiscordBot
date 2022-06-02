package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint128;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint96;
import space.alphaserpentis.squeethdiscordbot.handler.EthereumRPCHandler;
import space.alphaserpentis.squeethdiscordbot.handler.LaevitasHandler;

import java.awt.*;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
                    new Uint32(420),
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
                    new Uint32(420),
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

    private static long lastRun = 0;
    private static BigInteger ethCollateral, shortoSQTH, priceOfoSQTH, priceOfETHinUSD, crabTotalSupply, normFactor;
    private static double ethPerCrab, usdPerCrab;
    private static long lastHedgeTime;

    public Crab() {
        name = "crab";
        description = "Get current statistics on the Crab strategy!";
        onlyEmbed = true;
        deferReplies = true;
    }

    @Override
    public MessageEmbed runCommand(long userId, @NotNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
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
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        eb.setTitle("Crab Statistics");
        eb.setThumbnail("https://c.tenor.com/3CIbJomibvYAAAAi/crab-rave.gif");
        eb.setDescription("Get all of your crabby stats here!\n\nhttps://squeeth.com/strategies");
        eb.addField("ETH Collateral", NumberFormat.getInstance().format(ethCollateral.divide(BigInteger.valueOf((long) Math.pow(10,18))).doubleValue()) + " Ξ", false);
        eb.addField("Vault Debt", NumberFormat.getInstance().format(shortoSQTH.divide(BigInteger.valueOf((long) Math.pow(10,18))).doubleValue()) + " oSQTH", false);
        eb.addField("Collateral Ratio", NumberFormat.getInstance().format(calculateCollateralRatio()) + "%", false);
        eb.addField("Price per Crab Token", "$" + NumberFormat.getInstance().format(usdPerCrab) + " (" + NumberFormat.getInstance().format(ethPerCrab) + " Ξ)", false);
        eb.addField("Total Supply of Crab", NumberFormat.getInstance().format(crabTotalSupply.divide(BigInteger.valueOf((long) Math.pow(10,18)))), false);
        eb.addField("Last Rebalance", "<t:" + lastHedgeTime + ">", false);
        eb.setFooter("Last Updated at " + Instant.ofEpochSecond(lastRun).atOffset(ZoneOffset.UTC).toOffsetTime());
        eb.setColor(Color.RED);

        return eb.build();
    }

    @Override
    public void addCommand(@NotNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@NotNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }

    public double calculateCollateralRatio() {
        BigInteger debt = shortoSQTH.multiply(priceOfETHinUSD).multiply(normFactor).divide(BigInteger.valueOf(10000));
        // Divide by 10^36 of debt to get the correctly scaled debt
        return ethCollateral.doubleValue() / (debt.doubleValue() / Math.pow(10,36)) * 100;
    }
}
