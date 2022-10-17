package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;

public class PaperTrade extends ButtonCommand<MessageEmbed> {

    public PaperTrade() {
        super(new BotCommandOptions(
            "paper",
            "Paper trading game",
            true,
            true,
            TypeOfEphemeral.DEFAULT // might change later
        ));

        buttonHashMap.put("Confirm", Button.danger("papertrade_confirm", "Confirm"));
        buttonHashMap.put("Cancel", Button.primary("papertrade_cancel", "Cancel"));
    }

    private static final String defaultTitle = "Paper Trading Game!";

    @Nonnull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @Nonnull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        switch(event.getSubcommandName()) {
            case "about" -> aboutPage(eb);
            case "open" -> openAccountPage(eb);
        }

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@Nonnull JDA jda) {
        SubcommandData about = new SubcommandData("about", "What is Paper Trading?");
        SubcommandData trade = new SubcommandData("trade", "Trade your positions");
        SubcommandGroupData account = new SubcommandGroupData("account", "Account-related commands")
                .addSubcommands(
                        new SubcommandData("open", "Open (or reset) a paper trading account"),
                        new SubcommandData("view", "View PNL and current positions")
                );
        Command cmd = jda.upsertCommand(name, description).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void runButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        InteractionHook pending = event.deferEdit().complete();


        if(event.getButton().getId().equalsIgnoreCase("papertrade_confirm")) {

        } else if(event.getButton().getId().equalsIgnoreCase("papertrade_cancel")) {

        }
    }

    @Nonnull
    @Override
    public Collection<ItemComponent> addButtons(@Nonnull GenericCommandInteractionEvent event) {
        if(event.getSubcommandName().equalsIgnoreCase("open")) {
            return Arrays.asList(new ItemComponent[]{getButton("Confirm"), getButton("Cancel")});
        }

        return null;
    }

    private static void aboutPage(@Nonnull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setDescription("This is a trading game that trades with equivalent assets of ETH, oSQTH, Crab v2, and USDC! The funds are all virtual and do not actually trade on the Ethereum network or elsewhere.");
        eb.addField("How do I play?", "Simply run `/paper account open` to open a new account", false);
    }

    private static void openAccountPage(@Nonnull EmbedBuilder eb) {
        eb.setTitle(defaultTitle);
        eb.setDescription("Are you sure you want to open/reset your account?\n\nIf you're new, press 'Confirm'");
    }
}
