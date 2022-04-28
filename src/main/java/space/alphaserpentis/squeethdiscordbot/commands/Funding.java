package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import space.alphaserpentis.squeethdiscordbot.handler.LaevitasHandler;

import java.awt.*;
import java.text.NumberFormat;
import java.util.List;

public class Funding extends BotCommand {

    public Funding() {
        name = "funding";
        description = "Calculates the estimated amount of funding you would pay (or receive)";
        onlyEmbed = true;
    }

    @Override
    public Object runCommand(long userId) { // How did we get here?
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Error");
        eb.setDescription("No options passed?");
        eb.setColor(Color.RED);

        return eb.build();
    }

    @Override
    public Object runCommand(long userId, List<OptionMapping> optionMappingList) {
        EmbedBuilder eb = new EmbedBuilder();
        Double amt, funding, thetaCalculated, amtHeld;
        Integer days;

        // 0th Index should be amount of oSQTH in USD
        amt = optionMappingList.get(0).getAsDouble();
        // 1st Index should be number of days position is held
        days = optionMappingList.get(1).getAsInt();
        // 2nd Index should be funding rate in % (optional)
        if(optionMappingList.size() == 3)
            funding = optionMappingList.get(2).getAsDouble();
        else
            funding = LaevitasHandler.latestSqueethData.getCurrentImpliedFundingValue();

        thetaCalculated = (funding/100) * LaevitasHandler.latestSqueethData.getoSQTHPrice();
        amtHeld = amt/LaevitasHandler.latestSqueethData.getoSQTHPrice();

        eb.setTitle("Funding Calculator");
        eb.setDescription("**Disclaimer**: The following values are estimates! Funding rates are dynamic!");
        eb.addField("Estimated Funding", "With $" + amt + " (" + NumberFormat.getInstance().format(amtHeld) + " oSQTH) worth of oSQTH, at " + funding + "% current implied funding, and holding for " + days + " days, you might pay $" + NumberFormat.getInstance().format(amtHeld * thetaCalculated * days) + " in funding.", false);

        return eb.build();
    }

    @Override
    public void addCommand(JDA jda) {
        Command cmd = jda.upsertCommand(name, description)
                .addOption(OptionType.NUMBER, "amount", "The amount of oSQTH in USD you have", true)
                .addOption(OptionType.INTEGER, "days", "The amount of days you will maintain this position", true)
                .addOption(OptionType.NUMBER, "funding", "The funding rate in %", false)
                .complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(JDA jda) {
        Command cmd = jda.editCommandById(getCommandId()).complete();

        cmd.editCommand().clearOptions()
                .addOption(OptionType.NUMBER, "amount", "The amount of oSQTH in USD you have", true)
                .addOption(OptionType.INTEGER, "days", "The amount of days you will maintain this position", true)
                .addOption(OptionType.NUMBER, "funding", "The funding rate in %", false)
                .complete();

        System.out.println("[Funding] Updating command");
    }
}
