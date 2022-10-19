// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.LaevitasHandler;

import javax.annotation.Nonnull;
import java.text.NumberFormat;
import java.util.List;

public class Funding extends BotCommand<MessageEmbed> {

    public Funding() {
        super(new BotCommandOptions(
            "funding",
            "Calculates the estimated amount of funding you would pay (or receive)",
            true,
            false,
            TypeOfEphemeral.DEFAULT
        ));
    }

    @Nonnull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @Nonnull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        List<OptionMapping> optionMappingList = event.getOptions();
        NumberFormat instance = NumberFormat.getInstance();
        double amt, funding, currentEthPrice, thetaCalculated, deltaCalculated, gammaCalculated, amtHeld, breakevenEthChange;
        int days;

        // 0th Index should be amount of oSQTH in USD
        amt = optionMappingList.get(0).getAsDouble();
        // 1st Index should be number of days position is held
        days = optionMappingList.get(1).getAsInt();
        // 2nd Index should be funding rate in % (optional)
        if(optionMappingList.size() == 3)
            funding = optionMappingList.get(2).getAsDouble();
        else
            funding = LaevitasHandler.latestSqueethData.getCurrentImpliedFundingValue();

        amtHeld = amt/LaevitasHandler.latestSqueethData.getoSQTHPrice();
        thetaCalculated = (-funding/100) * LaevitasHandler.latestSqueethData.getoSQTHPrice() * amtHeld * days;
        deltaCalculated = LaevitasHandler.latestSqueethData.getDelta() * amtHeld;
        gammaCalculated = LaevitasHandler.latestSqueethData.getGamma() * amtHeld;
        breakevenEthChange = -(thetaCalculated + gammaCalculated)/deltaCalculated;
        currentEthPrice = LaevitasHandler.latestSqueethData.getUnderlyingPrice();

        eb.setTitle("Funding Calculator");
        eb.setDescription("**Disclaimer**: The following values are estimates! Funding rates are dynamic and other factors like volatility will affect a position's profitability!");
        eb.addField("Estimated Funding", "With $" + instance.format(amt) +
                " (" + instance.format(amtHeld) + " oSQTH) worth of oSQTH, at " + funding +
                "% current implied funding, and holding for " + days + " days, you might pay $" +
                instance.format(-thetaCalculated) + " in funding. ETH needs to move up by $" +
                instance.format(breakevenEthChange) + " to breakeven.",
                false
        );
        eb.addField("Current ETH Price", "$" + instance.format(currentEthPrice), false);
        eb.addField("Breakeven ETH Price", "$" + instance.format(currentEthPrice + breakevenEthChange), false);

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@Nonnull JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.NUMBER, "amount", "The amount of oSQTH in USD you have", true)
                .addOption(OptionType.INTEGER, "days", "The amount of days you will maintain this position", true)
                .addOption(OptionType.NUMBER, "funding", "The funding rate in %", false)
                .complete();

        commandId = cmd.getIdLong();
    }
}
