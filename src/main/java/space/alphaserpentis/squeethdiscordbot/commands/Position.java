package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.request.Transaction;
import space.alphaserpentis.squeethdiscordbot.handler.EthereumRPCHandler;

import java.io.IOException;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Position extends BotCommand {

    private HashMap<Long, Long> ratelimitMap = new HashMap<>();

    public Position() {
        name = "position";
        description = "Checks your wallet's Squeeth position";
        onlyEmbed = true;
        onlyEphemeral = true;
        deferReplies = true;
//        isActive = false;
    }

    @Override
    public Object runCommand(long userId, @NotNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        Long rateLimit = ratelimitMap.get(event.getUser().getIdLong());

        if(rateLimit != null) {
            if(rateLimit > Instant.now().getEpochSecond()) eb.setDescription("You are still rate limited");
        }

        if(event.getOptions().isEmpty())
            eb.setDescription("Missing Ethereum address");

        String ethUSDPool = "0x8ad599c3a0ff1de082011efddc58f1908eb6e6d8", ethOSQTHPool = "0x82c427adfdf2d245ec51d8046b41c4ee87f0d29c", oracle = "0x65d66c76447ccb45daf1e8044e918fa786a483a1", usdc = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", osqth = "0xf1b99e3e573a1a9c5e6b2ce818b617f0e664e86b", weth = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2";
        BigInteger response_osqthBal = null, response_ethUSD = null, response_osqthETH = null;

        try {
            EthereumRPCHandler.getAssetTransfersOfUser(event.getOptions().get(0).getAsString(), osqth);

//            Function balanceOf = new Function(
//                    "balanceOf",
//                    List.of(
//                            new Address(event.getOptions().get(0).getAsString())
//                    ),
//                    List.of(
//                            new TypeReference<Uint256>() {
//                            }
//                    )
//            );
//
//            Function getTwap_ethUSD = new Function(
//                    "getTwap",
//                    Arrays.asList(
//                            new org.web3j.abi.datatypes.Address(ethUSDPool),
//                            new org.web3j.abi.datatypes.Address(weth),
//                            new org.web3j.abi.datatypes.Address(usdc),
//                            new Uint32(420),
//                            new org.web3j.abi.datatypes.Bool(true)
//                    ),
//                    List.of(
//                            new TypeReference<Uint256>() {
//                            }
//                    )
//            );
//
//            Function getTwap_osqth = new Function(
//                    "getTwap",
//                    Arrays.asList(
//                            new org.web3j.abi.datatypes.Address(ethOSQTHPool),
//                            new org.web3j.abi.datatypes.Address(osqth),
//                            new org.web3j.abi.datatypes.Address(weth),
//                            new Uint32(420),
//                            new org.web3j.abi.datatypes.Bool(true)
//                    ),
//                    List.of(
//                            new TypeReference<Uint256>() {
//                            }
//                    )
//            );
//
//            Web3j web3 = EthereumRPCHandler.web3;
//
//            response_osqthBal = (BigInteger) FunctionReturnDecoder.decode(
//                    web3.ethCall(
//                            Transaction.createEthCallTransaction(
//                                    "0x0000000000000000000000000000000000000000",
//                                    osqth,
//                                    FunctionEncoder.encode(
//                                            balanceOf
//                                    )
//                            ),
//                            DefaultBlockParameter.valueOf(web3.ethBlockNumber().send().getBlockNumber())
//                    ).sendAsync().get().getResult(),
//                    balanceOf.getOutputParameters()
//            ).get(0).getValue();
//
//            response_ethUSD = (BigInteger) FunctionReturnDecoder.decode(
//                    web3.ethCall(
//                            Transaction.createEthCallTransaction(
//                                    "0x0000000000000000000000000000000000000000",
//                                    oracle,
//                                    FunctionEncoder.encode(
//                                            getTwap_ethUSD
//                                    )
//                            ),
//                            DefaultBlockParameter.valueOf(web3.ethBlockNumber().send().getBlockNumber())
//                    ).sendAsync().get().getResult(),
//                    balanceOf.getOutputParameters()
//            ).get(0).getValue();
//
//            response_osqthETH = (BigInteger) FunctionReturnDecoder.decode(
//                    web3.ethCall(
//                            Transaction.createEthCallTransaction(
//                                    "0x0000000000000000000000000000000000000000",
//                                    oracle,
//                                    FunctionEncoder.encode(
//                                            getTwap_osqth
//                                    )
//                            ),
//                            DefaultBlockParameter.valueOf(web3.ethBlockNumber().send().getBlockNumber())
//                    ).sendAsync().get().getResult(),
//                    balanceOf.getOutputParameters()
//            ).get(0).getValue();
//        } catch(IOException e) {
//            eb.setDescription("Error Occurred: IOException");
//            throw new RuntimeException(e);
//        } catch (ExecutionException e) {
//            eb.setDescription("Error Occurred: ExecutionException");
//            throw new RuntimeException(e);
//        } catch (InterruptedException e) {
//            eb.setDescription("Error Occurred: InterruptedException");
//            throw new RuntimeException(e);
        } finally {
            if (response_ethUSD == null && response_osqthETH == null && response_osqthBal == null) {
                eb.setDescription("Data error: One of the API responses are null");
            } else {
                eb.setDescription("**Notice**: This command currently only shows the CURRENT value of your oSQTH for longs! Refer to https://squeeth.com to check your position!");
                eb.addField("oSQTH Balance", NumberFormat.getInstance().format(response_osqthBal.doubleValue() / Math.pow(10, 18)) + " oSQTH", false);
                eb.addField("oSQTH Value", "$" + NumberFormat.getInstance().format((response_osqthBal.doubleValue() / Math.pow(10, 18)) * (((response_osqthETH.doubleValue() / Math.pow(10, 18)) * (response_ethUSD.doubleValue() / Math.pow(10, 18))))), false);
            }
            return eb.build();
        }
    }

    @Override
    public void addCommand(@NotNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.STRING, "address", "Your Ethereum address")
                .complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@NotNull JDA jda) {

    }
}
