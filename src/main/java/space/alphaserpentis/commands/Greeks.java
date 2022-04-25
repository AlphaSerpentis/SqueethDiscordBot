package space.alphaserpentis.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import space.alphaserpentis.handler.LaevitasHandler;

import java.awt.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class Greeks extends ICommand {

    public Greeks() {
        name = "greeks";
        description = "Display the Greeks for the Squeeth!";
        onlyEmbed = true;
    }
    @Override
    public Object runCommand(long userId) {
        EmbedBuilder eb = new EmbedBuilder();
        Double[] greeks = LaevitasHandler.latestSqueethData.getGreeks();

//        Instant epoch = Instant.ofEpochMilli(LaevitasHandler.latestSqueethData.getDate());
//        ZonedDateTime time = ZonedDateTime.ofInstant(epoch, ZoneOffset.UTC);

        eb.setTitle("Squeethy Greeks");
        eb.addField("Δ Delta: " + greeks[0].toString(), "For every $1 ETH moves, oSQTH moves by $" + greeks[0], false);
        eb.addField("Γ Gamma: " + new BigDecimal(greeks[1].toString()).toPlainString(), "For every $1 ETH changes, the delta of oSQTH changes by " + new BigDecimal(greeks[1].toString()).toPlainString(), false);
        eb.addField("ν Vega: " + greeks[2].toString(), "For every 100% IV changes, oSQTH changes by $" + greeks[2], false);
        eb.addField("Θ Theta: " + greeks[3].toString(), "For every hour that goes by, oSQTH decays by $" + greeks[3], false);
        eb.addField("Current IV: " + greeks[4].toString() + "%" , "As IV increases, oSQTH will increase in value and vice versa", false);
        eb.setFooter("Last Updated at " + LaevitasHandler.latestSqueethData.getDate() + " | API Data by Laevitas");
        eb.setColor(new Color(14, 255, 212, 76));

        return eb.build();
    }
}
