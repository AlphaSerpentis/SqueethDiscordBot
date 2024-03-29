// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Resources extends ButtonCommand<MessageEmbed> {

    protected static final ArrayList<MessageEmbed.Field> pages = new ArrayList<>() {{
        add(new MessageEmbed.Field("Docs & FAQ", "[Official Squeeth Documentation](https://opyn.gitbook.io/squeeth/resources/squeeth-faq)", false));
        add(new MessageEmbed.Field("Squeeth Articles",
                """
                        [Everlasting Options by Dave White](https://www.paradigm.xyz/2021/05/everlasting-options/)

                        [Power Perpetuals by Dave White](https://www.paradigm.xyz/2021/08/power-perpetuals/)

                        [Squeeth Primer](https://medium.com/opyn/squeeth-primer-a-guide-to-understanding-opyns-implementation-of-squeeth-a0f5e8b95684)

                        [Automated Squeeth Strategies](https://medium.com/opyn/automated-squeeth-strategies-the-crab-strategy-is-now-live-b92281ebe701)

                        [Squeeth Insides Volume 1: Funding and Volatility by Joe Clark](https://medium.com/opyn/squeeth-insides-volume-1-funding-and-volatility-f16bed146b7d)

                        [Guide to Hedging Uniswap v3 with Squeeth by Joe Clark](https://medium.com/opyn/hedging-uniswap-v3-with-squeeth-bcaf1750ea11)

                        [Demystifying Squeeth LPing](https://medium.com/opyn/lpeeeeeeth-demystifying-squeeth-lping-faee7e50ed33)

                        [Squeeth Crab Auction Tutorial](https://medium.com/opyn/participating-in-the-squeeth-crab-auction-b75a1defd8d6)""",
                false));
        add(new MessageEmbed.Field("Squeeth Twitter Threads",
                """
                        [Smooth Brain Thread on Squeeth by Wade](https://twitter.com/wadepros/status/1444690047639461893?s=20)

                        [4 Ways to Squeeth by Wade](https://twitter.com/wadepros/status/1466165414354989064?s=20)

                        [Understanding Squeeth Funding by Wade](https://twitter.com/wadepros/status/1473724744398692353?s=21)

                        [Squeeth is Not as Complicated as it Seems by Wade](https://twitter.com/wadepros/status/1477752798058057737?s=20)

                        [What Affects Long Squeeth Returns by Wade](https://twitter.com/wadepros/status/1507008456766595081?s=20&t=f2wGIZW3gyCDqTIBPqqviA)

                        [Quick Guide to Convexity in Options by Dave White](https://twitter.com/_Dave__White_/status/1423740205874302976?s=20)

                        [Power Perpetuals by macrocephalopod](https://twitter.com/macrocephalopod/status/1428126812878553091?s=20)

                        [Perp Swap, Everlasting Options, Squeeth by Wade](https://twitter.com/wadepros/status/1449850125354950664?s=21)

                        [Derivatives Pricing: An Introduction by zk](https://twitter.com/snarkyzk/status/1476259224988663809?s=20)""",
                false));
    }};

    public Resources() {
        super(new BotCommandOptions(
            "resources",
            "Obtain educational resources all about Squeeth!",
            true,
            true,
            TypeOfEphemeral.DEFAULT
        ));

        buttonHashMap.put("Previous", Button.primary("resources_previous", "Previous").asDisabled());
        buttonHashMap.put("Page", Button.secondary("resources_page", "1/" + pages.size()).asDisabled());
        buttonHashMap.put("Next", Button.primary("resources_next", "Next"));
    }

    @NonNull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Educational Resources");
        eb.setDescription("Learn all about Squeeth here!");

        eb.addField(pages.get(0));

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void runButtonInteraction(@NonNull ButtonInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Educational Resources");
        eb.setDescription("Learn all about Squeeth here!");

        List<Button> buttons = new ArrayList<>();
        int currentPage = Integer.parseInt(event.getMessage().getButtons().get(1).getLabel().substring(0,1));

        MessageEditCallbackAction pending = null;

        switch(event.getButton().getId()) {
            case "resources_next" -> {
                pending = event.editMessageEmbeds(eb.addField(pages.get(currentPage)).build());
                buttons.add(Button.secondary("resources_page", currentPage++ + 1 + "/" + pages.size()).asDisabled());
            }
            case "resources_previous" -> {
                pending = event.editMessageEmbeds(eb.addField(pages.get(currentPage - 2)).build());
                buttons.add(Button.secondary("resources_page", currentPage-- - 1 + "/" + pages.size()).asDisabled());
            }
        }

        if(currentPage == pages.size()) {
            buttons.add(0, Button.primary("resources_previous", "Previous").asEnabled());
            buttons.add(Button.primary("resources_next", "Next").asDisabled());
        } else if(currentPage == 1) {
            buttons.add(0, Button.primary("resources_previous", "Previous").asDisabled());
            buttons.add(Button.primary("resources_next", "Next").asEnabled());
        } else {
            buttons.add(0, Button.primary("resources_previous", "Previous").asEnabled());
            buttons.add(Button.primary("resources_next", "Next").asEnabled());
        }

        pending.setActionRow(buttons);

        pending.queue();

    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .complete();

        commandId = cmd.getIdLong();
    }

    @Override
    @NonNull
    public Collection<ItemComponent> addButtons(@NonNull GenericCommandInteractionEvent event) {
        return Arrays.asList(new ItemComponent[]{getButton("Previous"), getButton("Page"), getButton("Next")});
    }

}
