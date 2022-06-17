// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import space.alphaserpentis.squeethdiscordbot.data.ServerData;
import space.alphaserpentis.squeethdiscordbot.data.SquizLeaderboard;
import space.alphaserpentis.squeethdiscordbot.data.SquizQuestions;
import space.alphaserpentis.squeethdiscordbot.handler.ServerDataHandler;
import space.alphaserpentis.squeethdiscordbot.handler.SquizHandler;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class Squiz extends ButtonCommand {

    enum States {
        DEFAULT,
        IN_PROGRESS,
        COMPLETE
    }

    static class SquizSession {
        public States currentState = States.DEFAULT;
        public int currentQuestion = 0;
        public int currentScore = 0;
        public char correctCurrentAnswer = ' ';
        public ArrayList<SquizQuestions> questions;
        public HashMap<Integer, SquizQuestions> missedQuestions = new HashMap<>();
    }

    public static HashMap<Long, SquizSession> squizSessionHashMap = new HashMap<>();

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
        buttonHashMap.put("Review", Button.primary("squiz_review", "Review"));
    }

    @Nonnull
    @Override
    public MessageEmbed runCommand(long userId, @Nonnull SlashCommandInteractionEvent event) {
        SquizSession session = squizSessionHashMap.getOrDefault(userId, new SquizSession());
        EmbedBuilder eb = new EmbedBuilder();

        States state = session.currentState;

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
                    if(!verifyServerPerms(event.getMember())) {
                        eb.setTitle("Squiz Random Questions");
                        eb.setDescription("You do not have the required permissions to use this command (`MANAGE_SERVER`).");
                        return eb.build();
                    }

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
                    if(state == States.DEFAULT || state == States.COMPLETE) {
                        eb.setTitle("Squiz!");
                        session.currentState = States.DEFAULT;
                        eb.setDescription("Welcome to the Squeeth quiz!\n\n" +
                                "The quiz will ask you 10 questions and you will have to answer each question with the correct choice.\n" +
                                "You can end the quiz by pressing the \"End\" button.");
                    } else if (state == States.IN_PROGRESS) {
                        eb.setDescription("You are already in a quiz!");
                    }
                }
            }
        }

        return eb.build();
    }

    @Override
    public void addCommand(@Nonnull JDA jda) {
        SubcommandData leaderboard = new SubcommandData("leaderboard", "Displays the leaderboard for the Squiz competitions")
            .addOption(OptionType.CHANNEL, "channel", "The channel to display the leaderboard in", true);
        SubcommandData randomQuestions = new SubcommandData("random_questions", "Displays a random question from the Squiz question pool")
                .addOption(OptionType.BOOLEAN, "enable", "Enable random questions", true);
        SubcommandData play = new SubcommandData("play", "Starts your personal Squiz questionairre");

        Command cmd = jda.upsertCommand(name, description).addSubcommands(leaderboard, randomQuestions, play).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@Nonnull JDA jda) {
        SubcommandData leaderboard = new SubcommandData("leaderboard", "Displays the leaderboard for the Squiz competitions")
            .addOption(OptionType.CHANNEL, "channel", "The channel to display the leaderboard in");
        SubcommandData randomQuestions = new SubcommandData("random_questions", "Displays a random question from the Squiz question pool")
                .addOption(OptionType.BOOLEAN, "enable", "Enable random questions", true);
        SubcommandData play = new SubcommandData("play", "Starts your personal Squiz questionairre");

        Command cmd = jda.upsertCommand(name, description).addSubcommands(leaderboard, randomQuestions, play).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void runButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        SquizSession session = squizSessionHashMap.getOrDefault(event.getUser().getIdLong(), new SquizSession());
        Collection<ItemComponent> buttons = null;
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Squiz!");

        switch(event.getButton().getId()) {
            case "squiz_start" -> {
                int questionNumber = session.currentQuestion;
                int wrongAnswerCounter = 0;
                char correctAnswerAtRandom = (char) (Math.random() * 4 + 'A');

                session.correctCurrentAnswer = correctAnswerAtRandom;
                session.questions = getRandomQuestions();
                session.currentState = States.IN_PROGRESS;

                eb.addField("Question " + (questionNumber + 1), session.questions.get(questionNumber).question, false);

                for(int i = 0; i < 4; i++) {
                    if(i == correctAnswerAtRandom - 'A') {
                        eb.addField(String.valueOf((char) (i + 'A')), session.questions.get(questionNumber).answer, false);
                    } else {
                        eb.addField(String.valueOf((char) (i + 'A')), session.questions.get(questionNumber).wrongAnswers[wrongAnswerCounter++], false);
                    }
                }

                session.currentQuestion++;

                buttons = List.of(
                        buttonHashMap.get("A"),
                        buttonHashMap.get("B"),
                        buttonHashMap.get("C"),
                        buttonHashMap.get("D"),
                        buttonHashMap.get("End")
                );
            }
            case "squiz_end" -> {
                session.currentState = States.COMPLETE;
                eb.setDescription("You have ended the quiz.\n\n" +
                        "Your score is " + session.currentScore + ".");

                buttons = List.of(buttonHashMap.get("Review"));
            }
            case "squiz_answer_a" -> {
                if(checkIfCorrectAnswer(session.correctCurrentAnswer, 'A', session)) {
                    session.currentScore++;
                }

                buttons = handleNextQuestion(session, eb);
            }
            case "squiz_answer_b" -> {
                if(checkIfCorrectAnswer(session.correctCurrentAnswer, 'B', session)) {
                    session.currentScore++;
                }

                buttons = handleNextQuestion(session, eb);
            }
            case "squiz_answer_c" -> {
                if(checkIfCorrectAnswer(session.correctCurrentAnswer, 'C', session)) {
                    session.currentScore++;
                }

                buttons = handleNextQuestion(session, eb);
            }
            case "squiz_answer_d" -> {
                if(checkIfCorrectAnswer(session.correctCurrentAnswer, 'D', session)) {
                    session.currentScore++;
                }

                buttons = handleNextQuestion(session, eb);
            }
            case "squiz_leaderboard" -> {

            }
            case "squiz_review" -> {
                if(session.missedQuestions.size() == 0) {
                    eb.setDescription("Perfect score! :tada:");
                } else {
                    eb.setDescription("Questions you got wrong:");
                    for(int i = 0; i < session.missedQuestions.size(); i++) {
                        eb.addField(session.missedQuestions.get(i).question, "Correct answer was: " + session.missedQuestions.get(i).answer, false);
                    }
                }
            }
        }

        squizSessionHashMap.put(event.getUser().getIdLong(), session);
        MessageEditCallbackAction pending = event.editMessageEmbeds(eb.build());

        if(buttons != null) pending.setActionRow(buttons);
        pending.complete();
    }

    @Override
    public Collection<ItemComponent> addButtons(@Nonnull GenericCommandInteractionEvent event) {
        SquizSession session = squizSessionHashMap.getOrDefault(event.getUser().getIdLong(), new SquizSession());
        States state = session.currentState;

        if(state == States.IN_PROGRESS && !event.getSubcommandName().equalsIgnoreCase("play")) {
            return List.of(
                    buttonHashMap.get("A"),
                    buttonHashMap.get("B"),
                    buttonHashMap.get("C"),
                    buttonHashMap.get("D"),
                    buttonHashMap.get("End")
            );
        } else if(state == States.COMPLETE) {
            return List.of(buttonHashMap.get("Review"));
        } else if(event.getSubcommandName().equalsIgnoreCase("play") && state == States.IN_PROGRESS && session.currentQuestion != 0) {
            return List.of(buttonHashMap.get("End"));
        } else { // Assumes this is DEFAULT
            return List.of(buttonHashMap.get("Start"));
        }
    }

    private boolean checkIfCorrectAnswer(char answer, char correctAnswer, SquizSession session) {
        boolean response = answer == correctAnswer;

        if(!response) {
            session.missedQuestions.put(session.missedQuestions.size(), session.questions.get(session.currentQuestion - 1));
        }

        return response;
    }

    private List<ItemComponent> handleNextQuestion(SquizSession session, EmbedBuilder eb) {
        if(session.currentQuestion == session.questions.size()) {
            session.currentState = States.COMPLETE;
            eb.setDescription("You have ended the quiz.\n\n" +
                    "Your score is " + session.currentScore + ".");
            return List.of(
                    buttonHashMap.get("Review")
            );
        }

        int wrongAnswerCounter = 0;
        char correctAnswerAtRandom = (char) (Math.random() * 4 + 'A');

        eb.addField("Question " + (session.currentQuestion + 1), session.questions.get(session.currentQuestion).question, false);


        for(int i = 0; i < 4; i++) {
            if(i == correctAnswerAtRandom - 'A') {
                eb.addField(String.valueOf((char) (i + 'A')), session.questions.get(session.currentQuestion).answer, false);
            } else {
                eb.addField(String.valueOf((char) (i + 'A')), session.questions.get(session.currentQuestion).wrongAnswers[wrongAnswerCounter++], false);
            }
        }

        session.correctCurrentAnswer = session.questions.get(session.currentQuestion).answer.charAt(0);
        session.currentQuestion++;
        return List.of(
                buttonHashMap.get("A"),
                buttonHashMap.get("B"),
                buttonHashMap.get("C"),
                buttonHashMap.get("D"),
                buttonHashMap.get("End")
        );
    }

    private ArrayList<SquizQuestions> getRandomQuestions() {
        ArrayList<SquizQuestions> questions = new ArrayList<>();

        int random = (int) (Math.random() * SquizHandler.squizQuestions.size());
        while(questions.size() != 10) {
            if(!questions.contains(SquizHandler.squizQuestions.get(random))) {
                questions.add(SquizHandler.squizQuestions.get(random));
            }
            random = (int) (Math.random() * SquizHandler.squizQuestions.size());
        }

        return questions;
    }

    private boolean verifyServerPerms(Member member) {
        return member.hasPermission(
                Permission.MANAGE_SERVER
        );
    }
}
