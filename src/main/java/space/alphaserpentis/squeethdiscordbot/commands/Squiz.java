package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;

public class Squiz extends ButtonCommand {

    enum States {
        DEFAULT,
        IN_PROGRESS,
        COMPLETE
    }

    private static HashMap<Long, States> stateHashMap = new HashMap<>();

    public Squiz() {
        name = "squiz";
        description = "Squeeth quiz!";
        onlyEmbed = true;
        onlyEphemeral = true;

        buttonHashMap.put("Start", Button.primary("start", "start"));
        buttonHashMap.put("End", Button.primary("end", "end"));
        buttonHashMap.put("A", Button.primary("answer_a", "a"));
        buttonHashMap.put("B", Button.primary("answer_b", "b"));
        buttonHashMap.put("C", Button.primary("answer_c", "c"));
        buttonHashMap.put("D", Button.primary("answer_d", "d"));
    }

    @Override
    public MessageEmbed runCommand(long userId, @NotNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        States state = stateHashMap.getOrDefault(userId, States.DEFAULT);

        if(event.getSubcommandName() != null) {
            switch(event.getSubcommandName()) {
                case "leaderboard" -> {

                }
                case "random_questions" -> {

                }
            }
        }

        return eb.build();
    }

    @Override
    public void addCommand(@NotNull JDA jda) {
        SubcommandData leaderboard = new SubcommandData("leaderboard", "Displays the leaderboard for the Squiz competitions");
        SubcommandData randomQuestions = new SubcommandData("random_questions", "Displays a random question from the Squiz question pool")
                .addOption(OptionType.BOOLEAN, "enable", "Enable random questions", true);

        Command cmd = jda.upsertCommand(name, description).addSubcommands(leaderboard, randomQuestions).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@NotNull JDA jda) {
        SubcommandData leaderboard = new SubcommandData("leaderboard", "Displays the leaderboard for the Squiz competitions");
        SubcommandData randomQuestions = new SubcommandData("random_questions", "Displays a random question from the Squiz question pool")
                .addOption(OptionType.BOOLEAN, "enable", "Enable random questions", true);

        Command cmd = jda.upsertCommand(name, description).addSubcommands(leaderboard, randomQuestions).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void runButtonInteraction(@NotNull ButtonInteractionEvent event) {
        States state = stateHashMap.getOrDefault(event.getUser().getIdLong(), States.DEFAULT);

        switch(event.getButton().getId()) {
            case "start" -> {

            }
            case "end" -> {

            }
            case "answer_a" -> {

            }
            case "answer_b" -> {

            }
            case "answer_c" -> {

            }
            case "answer_d" -> {

            }
        }
    }

    @Override
    public Collection<ItemComponent> addButtons() {
        return null;
    }

}
