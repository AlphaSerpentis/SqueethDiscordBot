package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;
import space.alphaserpentis.squeethdiscordbot.data.ServerData;
import space.alphaserpentis.squeethdiscordbot.data.SquizLeaderboard;
import space.alphaserpentis.squeethdiscordbot.handler.ServerDataHandler;
import space.alphaserpentis.squeethdiscordbot.handler.SquizHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class Squiz extends ButtonCommand {

    enum States {
        DEFAULT,
        IN_PROGRESS,
        COMPLETE
    }

    private static HashMap<Long, States> stateHashMap = new HashMap<>();
    private static HashMap<Long, Integer> userQuestionHashMap = new HashMap<>();

    public Squiz() {
        name = "squiz";
        description = "Squeeth quiz!";
        onlyEmbed = true;
        onlyEphemeral = true;

        buttonHashMap.put("Start", Button.primary("squiz_start", "Start"));
        buttonHashMap.put("End", Button.primary("squiz_end", "End"));
        buttonHashMap.put("A", Button.primary("squiz_answer_a", "A"));
        buttonHashMap.put("B", Button.primary("squiz_answer_b", "B"));
        buttonHashMap.put("C", Button.primary("squiz_answer_c", "C"));
        buttonHashMap.put("D", Button.primary("squiz_answer_d", "D"));
        buttonHashMap.put("Leaderboard", Button.primary("squiz_leaderboard", "Leaderboard"));
    }

    @Override
    public MessageEmbed runCommand(long userId, @NotNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        States state = stateHashMap.getOrDefault(userId, States.DEFAULT);

        if(event.getSubcommandName() != null) {
            switch(event.getSubcommandName()) {
                case "leaderboard" -> {
                    eb.setTitle("Squiz Leaderboard");
                    SquizLeaderboard leaderboard = SquizHandler.squizLeaderboardHashMap.getOrDefault(event.getGuild().getId(), new SquizLeaderboard());
                    HashMap<Long, Integer> topFive = leaderboard.getTopFive();

                    for(int i = 0; i < 5 && topFive.size() > 0; i++) {
                        eb.addField(String.format("%d. %s", i + 1, event.getJDA().getUserById((String) topFive.keySet().toArray()[i]).getAsMention()), String.format("%d", topFive.values().toArray()[i]), true);
                    }
                }
                case "random_questions" -> {
                    ServerData serverData = ServerDataHandler.serverDataHashMap.getOrDefault(event.getGuild().getId(), new ServerData());
                    serverData.setDoRandomSquizQuestions(event.getOptions().get(0).getAsBoolean());

                    try {
                        ServerDataHandler.updateServerData();
                        eb.setTitle("Squiz Random Questions");
                        eb.setDescription("Random questions are toggled " + (serverData.doRandomSquizQuestions() ? "on" : "off") + ".");
                    } catch (IOException e) {
                        eb.setTitle("Squiz Random Questions");
                        eb.setDescription("Failed to toggle random questions. If this persists, please contact AlphaSerpentis#3203 at discord.gg/opyn");
                        e.printStackTrace();
                    }
                }
                case "play" -> {
                    if (state == States.DEFAULT) {
                        Integer questionNumber = userQuestionHashMap.getOrDefault(userId, 0);
                        stateHashMap.put(userId, States.IN_PROGRESS);
                        eb.setTitle("Squiz!");
                        eb.addField("Question " + ++questionNumber, "", false);
                        eb.addField("A", "", false);
                        eb.addField("B", "", false);
                        eb.addField("C", "", false);
                        eb.addField("D", "", false);
                    } else if (state == States.IN_PROGRESS) {
                        eb.setDescription("You are already in a quiz!");
                    }
                }
            }
        }

        return eb.build();
    }

    @Override
    public void addCommand(@NotNull JDA jda) {
        SubcommandData leaderboard = new SubcommandData("leaderboard", "Displays the leaderboard for the Squiz competitions")
            .addOption(OptionType.CHANNEL, "channel", "The channel to display the leaderboard in", true);
        SubcommandData randomQuestions = new SubcommandData("random_questions", "Displays a random question from the Squiz question pool")
                .addOption(OptionType.BOOLEAN, "enable", "Enable random questions", true);
        SubcommandData play = new SubcommandData("play", "Starts your personal Squiz questionairre");

        Command cmd = jda.upsertCommand(name, description).addSubcommands(leaderboard, randomQuestions, play).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@NotNull JDA jda) {
        SubcommandData leaderboard = new SubcommandData("leaderboard", "Displays the leaderboard for the Squiz competitions")
            .addOption(OptionType.CHANNEL, "channel", "The channel to display the leaderboard in");
        SubcommandData randomQuestions = new SubcommandData("random_questions", "Displays a random question from the Squiz question pool")
                .addOption(OptionType.BOOLEAN, "enable", "Enable random questions", true);
        SubcommandData play = new SubcommandData("play", "Starts your personal Squiz questionairre");

        Command cmd = jda.upsertCommand(name, description).addSubcommands(leaderboard, randomQuestions, play).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void runButtonInteraction(@NotNull ButtonInteractionEvent event) {
        States state = stateHashMap.getOrDefault(event.getUser().getIdLong(), States.DEFAULT);

        switch(event.getButton().getId()) {
            case "squiz_start" -> {
                state = States.IN_PROGRESS;
            }
            case "squiz_end" -> {
                state = States.COMPLETE;
            }
            case "squiz_answer_a" -> {

            }
            case "squiz_answer_b" -> {

            }
            case "squiz_answer_c" -> {

            }
            case "squiz_answer_d" -> {

            }
            case "squiz_leaderboard" -> {

            }
        }
    }

    @Override
    public Collection<ItemComponent> addButtons(@NotNull GenericCommandInteractionEvent event) {
        States state = stateHashMap.getOrDefault(event.getUser().getIdLong(), States.DEFAULT);

        if(state == States.IN_PROGRESS && !event.getSubcommandName().equalsIgnoreCase("play")) {
            return List.of(
                    buttonHashMap.get("A"),
                    buttonHashMap.get("B"),
                    buttonHashMap.get("C"),
                    buttonHashMap.get("D"),
                    buttonHashMap.get("End")
            );
        } else if(state == States.COMPLETE) {
            return List.of(buttonHashMap.get("squiz_leaderboard"));
        } else if(event.getSubcommandName().equalsIgnoreCase("play")){
            return List.of();
        } else { // Assumes this is DEFAULT
            return List.of(buttonHashMap.get("squiz_start"));
        }
    }
}
