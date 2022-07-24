// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import space.alphaserpentis.squeethdiscordbot.data.server.ServerCache;
import space.alphaserpentis.squeethdiscordbot.data.server.ServerData;
import space.alphaserpentis.squeethdiscordbot.data.server.squiz.SquizLeaderboard;
import space.alphaserpentis.squeethdiscordbot.data.server.squiz.SquizQuestions;
import space.alphaserpentis.squeethdiscordbot.handler.ServerDataHandler;
import space.alphaserpentis.squeethdiscordbot.handler.SquizHandler;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class Squiz extends ButtonCommand {

    enum States {
        DEFAULT,
        RANDOM,
        IN_PROGRESS,
        COMPLETE,
        VIEWING_LEADERBOARD
    }

    public static class SquizSession {
        public States currentState = States.DEFAULT;
        public int currentQuestion = 0;
        public int currentScore = 0;
        public char correctCurrentAnswer = ' ';
        public ArrayList<SquizQuestions> questions;
        public HashMap<Integer, SquizQuestions> missedQuestions = new HashMap<>();

        @Override
        public String toString() {
            return "currentState: " + currentState + "," +
                    "currentQuestion: " + currentQuestion + "," +
                    "currentScore: " + currentScore + "," +
                    "correctCurrentAnswer: " + correctCurrentAnswer + "," +
                    "questions: " + questions + "," +
                    "missedQuestions: " + missedQuestions;
        }
    }

    public static class RandomSquizSession extends SquizSession {
        public long serverId;
        public long userWhoResponded;
        public Message message;
    }

    public static HashMap<Long, SquizSession> squizSessionHashMap = new HashMap<>();
    public static HashMap<Long, RandomSquizSession> randomSquizSessionsHashMap = new HashMap<>();

    public Squiz() {
        name = "squiz";
        description = "Squeeth quiz!";
        onlyEmbed = true;
        onlyEphemeral = true;
        messagesExpire = true;
        messageExpirationLength = 15;

        buttonHashMap.put("Start", Button.primary("squiz_start", "Start"));
        buttonHashMap.put("End", Button.primary("squiz_end", "End"));
        buttonHashMap.put("A", Button.primary("squiz_answer_a", "A"));
        buttonHashMap.put("B", Button.primary("squiz_answer_b", "B"));
        buttonHashMap.put("C", Button.primary("squiz_answer_c", "C"));
        buttonHashMap.put("D", Button.primary("squiz_answer_d", "D"));
        buttonHashMap.put("True", Button.primary("squiz_answer_true", "True"));
        buttonHashMap.put("False", Button.primary("squiz_answer_false", "False"));
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
                    session.currentState = States.VIEWING_LEADERBOARD;
                    generateLeaderboard(eb, event.getGuild().getIdLong());
                }
                case "play" -> {
                    if(state != States.IN_PROGRESS) {
                        session = new SquizSession(); // dereference to prevent reusing old reference
                        squizSessionHashMap.put(userId, session);
                        eb.setTitle("Squiz!");
                        session.currentState = States.DEFAULT;
                        eb.setDescription("Welcome to the Squeeth quiz!\n\n" +
                                "The quiz will ask you 10 questions and you will have to answer each question with the correct choice.\n" +
                                "You can end the quiz by pressing the \"End\" button.");
                    } else {
                        eb.setDescription("You are already in a quiz!");
                    }
                }
            }
        }

        squizSessionHashMap.putIfAbsent(userId, session);

        return eb.build();
    }

    @Override
    public void addCommand(@Nonnull JDA jda) {
        SubcommandData leaderboard = new SubcommandData("leaderboard", "Displays the leaderboard for the Squiz competitions");
        SubcommandData play = new SubcommandData("play", "Starts your personal Squiz questionairre");

        Command cmd = jda.upsertCommand(name, description).addSubcommands(leaderboard, play).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void updateCommand(@Nonnull JDA jda) {
        SubcommandData leaderboard = new SubcommandData("leaderboard", "Displays the leaderboard for the Squiz competitions");
        SubcommandData play = new SubcommandData("play", "Starts your personal Squiz questionairre");

        Command cmd = jda.upsertCommand(name, description).addSubcommands(leaderboard, play).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void runButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        Collection<ItemComponent> buttons = null;
        EmbedBuilder eb = new EmbedBuilder();
        long serverId = event.getGuild().getIdLong();
        long userId = event.getUser().getIdLong();

        SquizSession session = squizSessionHashMap.get(userId);

        eb.setTitle("Squiz!");

        // Check if the event is from an active Squiz
        if(randomSquizSessionsHashMap.get(serverId) != null) {
            if(event.getMessage().getIdLong() == randomSquizSessionsHashMap.get(serverId).message.getIdLong()) {
                session = randomSquizSessionsHashMap.get(serverId);
                if(((RandomSquizSession) session).userWhoResponded == 0) {
                    ((RandomSquizSession) session).userWhoResponded = userId;
                } else { // if two users respond fast enough
                    return;
                }
                eb.setTitle("Random Squiz!");
            } else {
                squizSessionHashMap.put(userId, session);
            }
        } else {
            squizSessionHashMap.put(userId, session);
        }

        switch(event.getButton().getId()) {
            case "squiz_start" -> {
                int questionNumber = session.currentQuestion;
                int wrongAnswerCounter = 0;

                session.questions = getRandomQuestions(10);
                session.currentState = States.IN_PROGRESS;

                eb.addField("Question " + (questionNumber + 1), session.questions.get(questionNumber).question, false);

                if(session.questions.get(questionNumber).wrongAnswers.length == 1) {

                    boolean isTrueCorrect = session.questions.get(questionNumber).answer.equalsIgnoreCase("true");

                    if(isTrueCorrect) {
                        session.correctCurrentAnswer = 'T';
                        eb.addField("True", session.questions.get(questionNumber).answer, false);
                        eb.addField("False", session.questions.get(questionNumber).wrongAnswers[0], false);
                    } else {
                        session.correctCurrentAnswer = 'F';
                        eb.addField("True", session.questions.get(questionNumber).wrongAnswers[0], false);
                        eb.addField("False", session.questions.get(questionNumber).answer, false);
                    }

                    buttons = List.of(
                            buttonHashMap.get("True"),
                            buttonHashMap.get("False"),
                            buttonHashMap.get("End")
                    );
                } else {
                    char correctAnswerAtRandom = (char) (Math.random() * 4 + 'A');

                    session.correctCurrentAnswer = correctAnswerAtRandom;
                    for(int i = 0; i < 4; i++) {
                        if(i == correctAnswerAtRandom - 'A') {
                            eb.addField(String.valueOf((char) (i + 'A')), session.questions.get(questionNumber).answer, false);
                        } else {
                            eb.addField(String.valueOf((char) (i + 'A')), session.questions.get(questionNumber).wrongAnswers[wrongAnswerCounter++], false);
                        }
                    }

                    buttons = List.of(
                            buttonHashMap.get("A"),
                            buttonHashMap.get("B"),
                            buttonHashMap.get("C"),
                            buttonHashMap.get("D"),
                            buttonHashMap.get("End")
                    );
                }

                session.currentQuestion++;
            }
            case "squiz_end" -> {
                session.currentState = States.COMPLETE;
                eb.setDescription("You have ended the quiz.\n\n" +
                        "Your score is " + session.currentScore + ".");

                buttons = List.of(buttonHashMap.get("Review"));
            }
            case "squiz_answer_a" -> {
                if(checkIfCorrectAnswer('A', session.correctCurrentAnswer, session)) {
                    session.currentScore++;
                }

                buttons = handleNextQuestion(session, eb);
            }
            case "squiz_answer_b" -> {
                if(checkIfCorrectAnswer('B', session.correctCurrentAnswer, session)) {
                    session.currentScore++;
                }

                buttons = handleNextQuestion(session, eb);
            }
            case "squiz_answer_c" -> {
                if(checkIfCorrectAnswer('C', session.correctCurrentAnswer, session)) {
                    session.currentScore++;
                }

                buttons = handleNextQuestion(session, eb);
            }
            case "squiz_answer_d" -> {
                if(checkIfCorrectAnswer('D', session.correctCurrentAnswer, session)) {
                    session.currentScore++;
                }

                buttons = handleNextQuestion(session, eb);
            }
            case "squiz_answer_true" -> {
                if(checkIfCorrectAnswer('T', session.correctCurrentAnswer, session)) {
                    session.currentScore++;
                }

                buttons = handleNextQuestion(session, eb);
            }
            case "squiz_answer_false" -> {
                if(checkIfCorrectAnswer('F', session.correctCurrentAnswer, session)) {
                    session.currentScore++;
                }

                buttons = handleNextQuestion(session, eb);
            }
            case "squiz_leaderboard" -> {
                generateLeaderboard(eb, serverId);
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
                squizSessionHashMap.remove(userId);
            }
        }

        MessageEditCallbackAction pending = event.editComponents().setEmbeds(eb.build());

        if(buttons != null)
            pending.setActionRow(buttons).complete();
        else
            pending.complete();
    }

    @Nonnull
    @Override
    public Collection<ItemComponent> addButtons(@Nonnull GenericCommandInteractionEvent event) {
        SquizSession session = squizSessionHashMap.get(event.getUser().getIdLong());
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
        } else if(state == States.VIEWING_LEADERBOARD) {
            return Collections.emptyList();
        } else { // Assumes this is DEFAULT
            return List.of(buttonHashMap.get("Start"));
        }
    }

    public void setRandomSquizQuestionSession(long serverId) {
        RandomSquizSession session = new RandomSquizSession();
        session.currentState = States.RANDOM;
        session.serverId = serverId;
        session.questions = getRandomQuestions(1);

        randomSquizSessionsHashMap.put(serverId, session);
    }

    public void sendRandomSquizQuestion(long serverId) {
        ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);
        RandomSquizSession session = randomSquizSessionsHashMap.get(serverId);
        EmbedBuilder eb = new EmbedBuilder();

        TextChannel channelRand = Launcher.api.getTextChannelById(sd.getRandomSquizQuestionsChannels().get((int) (Math.random() * sd.getRandomSquizQuestionsChannels().size())));
        List<ItemComponent> buttons = handleNextQuestion(session, eb);

        eb.setTitle("Random Squiz!");

        Message message = channelRand.sendMessageEmbeds(eb.build()).setActionRow(buttons).complete();
        session.message = message;
        ServerCache.addNewMessage(serverId, message);
    }

    public void updateLeaderboard(long serverId) throws NullPointerException {
        ServerData sd = ServerDataHandler.serverDataHashMap.get(serverId);
        TextChannel channel = Launcher.api.getTextChannelById(sd.getLeaderboardChannelId());
        EmbedBuilder eb = new EmbedBuilder();

        if(channel == null) throw new NullPointerException(); // invalid state to try to operate with

        if(sd.getLastLeaderboardMessage() != 0) {
            // Check if the message is still valid
            Message message = null;
            try {
                message = channel.retrieveMessageById(sd.getLastLeaderboardMessage()).complete();
            } catch(ErrorResponseException ignored) {

            }

            // construct the new embed
            generateLeaderboard(eb, serverId);

            if(message == null) { // create a new leaderboard message
                message = channel.sendMessageEmbeds(eb.build()).complete();
                sd.setLastLeaderboardMessage(message.getIdLong());
            } else { // reuse the message
                message.editMessageEmbeds(eb.build()).complete();
            }
        } else {
            generateLeaderboard(eb, serverId);

            sd.setLastLeaderboardMessage(
                    channel.sendMessageEmbeds(eb.build()).complete().getIdLong()
            );
        }
    }

    private boolean checkIfCorrectAnswer(char answer, char correctAnswer, SquizSession session) {
        boolean response = answer == correctAnswer;

        if(!response) {
            session.missedQuestions.put(session.missedQuestions.size(), session.questions.get(session.currentQuestion - 1));
        } else {
            if(session instanceof RandomSquizSession) {
                Map<Long, SquizLeaderboard> squizLeaderboardHashMap = SquizHandler.squizLeaderboardHashMap;

                SquizLeaderboard serverLeaderboard = squizLeaderboardHashMap.getOrDefault(((RandomSquizSession) session).serverId, new SquizLeaderboard());
                serverLeaderboard.addPoint(((RandomSquizSession) session).userWhoResponded);
                squizLeaderboardHashMap.putIfAbsent(((RandomSquizSession) session).serverId, serverLeaderboard);
                try {
                    SquizHandler.updateSquizLeaderboard();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return response;
    }

    @Nullable
    private List<ItemComponent> handleNextQuestion(SquizSession session, EmbedBuilder eb) {
        if(session.currentQuestion == session.questions.size()) {
            if(session instanceof RandomSquizSession) { // If the session is a random squiz
                if(session.missedQuestions.size() == 1) {
                    eb.setDescription("You did not earn a point :slight_frown:\n\n" +
                            "The correct answer was " + session.missedQuestions.get(0).answer);
                } else {
                    eb.setDescription("You earned a point!");
                    try {
                        updateLeaderboard(((RandomSquizSession) session).serverId);
                        ServerDataHandler.updateServerData();
                    } catch(NullPointerException ignored) {

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    // TODO: SHOW USER'S CURRENT POSITION IN THE SERVER LEADERBOARD
                }
                randomSquizSessionsHashMap.remove(((RandomSquizSession) session).serverId); // Removes the reference
                letMessageExpire(this, ((RandomSquizSession) session).message);
                return null;
            }
            session.currentState = States.COMPLETE;
            eb.setDescription("You have ended the quiz.\n\n" +
                    "Your score is " + session.currentScore + ".");
            return List.of(
                    buttonHashMap.get("Review")
            );
        }

        int wrongAnswerCounter = 0;

        eb.addField("Question " + (session.currentQuestion + 1), session.questions.get(session.currentQuestion).question, false);

        if(session.questions.get(session.currentQuestion).wrongAnswers.length == 1) {

            boolean isTrueCorrect = session.questions.get(session.currentQuestion).answer.equalsIgnoreCase("true");

            if(isTrueCorrect) {
                session.correctCurrentAnswer = 'T';
                eb.addField("True", session.questions.get(session.currentQuestion).answer, false);
                eb.addField("False", session.questions.get(session.currentQuestion).wrongAnswers[0], false);
            } else {
                session.correctCurrentAnswer = 'F';
                eb.addField("True", session.questions.get(session.currentQuestion).wrongAnswers[0], false);
                eb.addField("False", session.questions.get(session.currentQuestion).answer, false);
            }

            session.currentQuestion++;

            if(session.currentState == States.RANDOM) {
                return List.of(
                        buttonHashMap.get("True"),
                        buttonHashMap.get("False")
                );
            }

            return List.of(
                    buttonHashMap.get("True"),
                    buttonHashMap.get("False"),
                    buttonHashMap.get("End")
            );
        } else {
            char correctAnswerAtRandom = (char) (Math.random() * 4 + 'A');

            session.correctCurrentAnswer = correctAnswerAtRandom;
            for(int i = 0; i < 4; i++) {
                if(i == correctAnswerAtRandom - 'A') {
                    eb.addField(String.valueOf((char) (i + 'A')), session.questions.get(session.currentQuestion).answer, false);
                } else {
                    eb.addField(String.valueOf((char) (i + 'A')), session.questions.get(session.currentQuestion).wrongAnswers[wrongAnswerCounter++], false);
                }
            }

            session.currentQuestion++;

            if(session.currentState == States.RANDOM) {
                return List.of(
                        buttonHashMap.get("A"),
                        buttonHashMap.get("B"),
                        buttonHashMap.get("C"),
                        buttonHashMap.get("D")
                );
            }

            return List.of(
                    buttonHashMap.get("A"),
                    buttonHashMap.get("B"),
                    buttonHashMap.get("C"),
                    buttonHashMap.get("D"),
                    buttonHashMap.get("End")
            );
        }
    }

    @Nonnull
    private ArrayList<SquizQuestions> getRandomQuestions(int size) {
        ArrayList<SquizQuestions> questions = new ArrayList<>();

        int random = (int) (Math.random() * SquizHandler.squizQuestions.size());
        while(questions.size() != size) {
            if(!questions.contains(SquizHandler.squizQuestions.get(random))) {
                questions.add(SquizHandler.squizQuestions.get(random));
            }
            random = (int) (Math.random() * SquizHandler.squizQuestions.size());
        }

        return questions;
    }

    private static void generateLeaderboard(EmbedBuilder eb, long serverId) {
        eb.setTitle("Squiz Leaderboard");
        eb.setDescription("Shows the top 5 people on the leaderboard for the server plus where you are currently in the leaderboard");
        SquizLeaderboard leaderboard = SquizHandler.squizLeaderboardHashMap.getOrDefault(serverId, new SquizLeaderboard());
        ArrayList<Long> topFive = leaderboard.getTopFive();
        Guild guild = Launcher.api.getGuildById(serverId);

        for(int i = 0; i < 5 && i < topFive.size(); i++) {
            eb.addField(String.format("%d. %s", i + 1, guild.retrieveMemberById(topFive.get(i)).complete().getUser().getAsTag()), String.format("%d", leaderboard.leaderboard.get(topFive.get(i))), false);
        }
    }
}
