package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;

public class Resources extends BotCommand {

    public Resources() {
        name = "resources";
        description = "Obtain educational resources all about Squeeth!";
        onlyEmbed = true;
    }

    @Override
    public Object runCommand(long userId, @NotNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Educational Resources");
        eb.setDescription("Learn all about Squeeth here!");

        eb.addField("Docs & FAQ", "[Official Squeeth Documentation](https://opyn.gitbook.io/squeeth/resources/squeeth-faq)", false);
        eb.addField(
                "Squeeth Articles",
                "[Everlasting Options by Dave White](https://www.paradigm.xyz/2021/05/everlasting-options/)" +
                        "\n\n" +
                        "[Power Perpetuals by Dave White](https://www.paradigm.xyz/2021/08/power-perpetuals/)" +
                        "\n\n" +
                        "[Squeeth Primer](https://medium.com/opyn/squeeth-primer-a-guide-to-understanding-opyns-implementation-of-squeeth-a0f5e8b95684)" +
                        "\n\n" +
                        "[Automated Squeeth Strategies](https://medium.com/opyn/automated-squeeth-strategies-the-crab-strategy-is-now-live-b92281ebe701)" +
                        "\n\n" +
                        "[Squeeth Insides Volume 1: Funding and Volatility by Joe Clark](https://medium.com/opyn/squeeth-insides-volume-1-funding-and-volatility-f16bed146b7d)" +
                        "\n\n" +
                        "[Guide to Hedging Uniswap v3 with Squeeth by Joe Clark](https://medium.com/opyn/hedging-uniswap-v3-with-squeeth-bcaf1750ea11)" +
                        "\n\n" +
                        "[Demystifying Squeeth LPing](https://medium.com/opyn/lpeeeeeeth-demystifying-squeeth-lping-faee7e50ed33)" +
                        "\n\n" +
                        "[Squeeth Crab Auction Tutorial](https://medium.com/opyn/participating-in-the-squeeth-crab-auction-b75a1defd8d6)",
                false);
        eb.addField(
                "Squeeth Twitter Threads",
                "[Smooth Brain Thread on Squeeth by Wade](https://twitter.com/wadepros/status/1444690047639461893?s=20)" +
                        "\n\n" +
                        "[4 Ways to Squeeth by Wade](https://twitter.com/wadepros/status/1466165414354989064?s=20)" +
                        "\n\n" +
                        "[Understanding Squeeth Funding by Wade](https://twitter.com/wadepros/status/1473724744398692353?s=21)" +
                        "\n\n" +
                        "[Squeeth is Not as Complicated as it Seems by Wade](https://twitter.com/wadepros/status/1477752798058057737?s=20)" +
                        "\n\n" +
                        "[What Affects Long Squeeth Returns by Wade](https://twitter.com/wadepros/status/1507008456766595081?s=20&t=f2wGIZW3gyCDqTIBPqqviA)" +
                        "\n\n" +
                        "[Quick Guide to Convexity in Options by Dave White](https://twitter.com/_Dave__White_/status/1423740205874302976?s=20)" +
                        "\n\n" +
                        "[Power Perpetuals by macrocephalopod](https://twitter.com/macrocephalopod/status/1428126812878553091?s=20)" +
                        "\n\n" +
                        "[Perp Swap, Everlasting Options, Squeeth by Wade](https://twitter.com/wadepros/status/1449850125354950664?s=21)" +
                        "\n\n" +
                        "[Derivatives Pricing: An Introduction by zk](https://twitter.com/snarkyzk/status/1476259224988663809?s=20)",
                false
        );

        return eb.build();
    }

    @Override
    public void addCommand(JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(JDA jda) {
        return;
    }
}
