// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction;
import space.alphaserpentis.squeethdiscordbot.data.bot.CommandResponse;
import space.alphaserpentis.squeethdiscordbot.data.server.ServerCache;
import space.alphaserpentis.squeethdiscordbot.data.server.ServerData;
import space.alphaserpentis.squeethdiscordbot.data.server.squiz.SquizLeaderboard;
import space.alphaserpentis.squeethdiscordbot.data.server.squiz.SquizQuestions;
import space.alphaserpentis.squeethdiscordbot.data.server.squiz.SquizTracking;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.servers.ServerDataHandler;
import space.alphaserpentis.squeethdiscordbot.handler.games.SquizHandler;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public class Squiz extends ButtonCommand<MessageEmbed> {

    enum States {
        DEFAULT,
        RANDOM,
        IN_PROGRESS,
        COMPLETE,
        VIEWING_LEADERBOARD,
        GETTING_PLAYERS,
        PENDING_CONFIRMATION,
        IGNORE
    }

    public static class SquizSession {
        public States currentState = States.DEFAULT;
        public int currentQuestion = 0;
        public int currentScore = 0;
        public char correctCurrentAnswer = ' ';
        public ArrayList<SquizQuestions> questions;
        public final HashMap<Integer, SquizQuestions> missedQuestions = new HashMap<>();

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
        public static final HashMap<Long, Integer> consecutiveAnswers = new HashMap<>();
        public Thread expiringThread;
        public final HashMap<Long, Character> responses = new HashMap<>();
        public long serverId;
        public long timeReacted = 0;
        public long epochInMillisecondsWhenPosted;
        public Message message;
    }

    public static class ViewingPlayersSession extends SquizSession {
        public ArrayList<String> pages;
        public int currentPage = 0;
    }

    public static final HashMap<Long, SquizSession> squizSessionHashMap = new HashMap<>();
    public static final HashMap<Long, RandomSquizSession> randomSquizSessionsHashMap = new HashMap<>();

    public Squiz() {
        super(new BotCommandOptions(
            "squiz",
            "Squeeth quiz!",
            0,
            60,
            true,
            true,
            TypeOfEphemeral.DYNAMIC,
            true,
            false,
            false,
            true
        ));

        buttonHashMap.put("Start", Button.primary("squiz_start", "Start"));
        buttonHashMap.put("End", Button.danger("squiz_end", "End"));
        buttonHashMap.put("A", Button.primary("squiz_answer_a", "A"));
        buttonHashMap.put("B", Button.primary("squiz_answer_b", "B"));
        buttonHashMap.put("C", Button.primary("squiz_answer_c", "C"));
        buttonHashMap.put("D", Button.primary("squiz_answer_d", "D"));
        buttonHashMap.put("True", Button.primary("squiz_answer_true", "True"));
        buttonHashMap.put("False", Button.primary("squiz_answer_false", "False"));
        buttonHashMap.put("Leaderboard", Button.primary("squiz_leaderboard", "Leaderboard"));
        buttonHashMap.put("Review", Button.primary("squiz_review", "Review"));
        buttonHashMap.put("Previous", Button.primary("squiz_previous", "Previous").asDisabled());
        buttonHashMap.put("1/?", Button.secondary("squiz_page", "1/?").asDisabled());
        buttonHashMap.put("Next", Button.primary("squiz_next", "Next"));
        buttonHashMap.put("Confirm", Button.danger("squiz_confirm", "Confirm"));
        buttonHashMap.put("Cancel", Button.primary("squiz_cancel", "Cancel"));
    }

    @NonNull
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        SquizSession session = squizSessionHashMap.getOrDefault(userId, new SquizSession());
        EmbedBuilder eb = new EmbedBuilder();

        States state = session.currentState;
        eb.setTitle("Squiz!");

        if(event.getGuild() == null) {
            eb.setDescription("Command cannot be used in the DMs currently.");
        }

        if(event.getSubcommandName() != null) {
            switch(event.getSubcommandName()) {
                case "leaderboard" -> {
                    session.currentState = States.VIEWING_LEADERBOARD;
                    generateLeaderboard(eb, event.getGuild().getIdLong(), event.getUser().getIdLong());
                }
                case "play" -> {
                    if(state != States.IN_PROGRESS) {
                        session = new SquizSession(); // dereference to prevent reusing old reference
                        squizSessionHashMap.put(userId, session);
                        session.currentState = States.DEFAULT;
                        eb.setDescription("""
                                Welcome to the Squeeth quiz!

                                The quiz will ask you 10 questions and you will have to answer each question with the correct choice.
                                You can end the quiz by pressing the "End" button.""");
                    } else {
                        eb.setDescription("You are already in a quiz!");
                    }
                }
                case "add_point" -> {
                    session.currentState = States.IGNORE;
                    if(verifyManageServerPerms(event.getMember())) {
                        SquizLeaderboard leaderboard = SquizHandler.squizLeaderboardHashMap.getOrDefault(event.getGuild().getIdLong(), new SquizLeaderboard());

                        leaderboard.addPoint(event.getUser().getIdLong());

                        SquizHandler.squizLeaderboardHashMap.putIfAbsent(event.getGuild().getIdLong(), leaderboard);

                        eb.setDescription("Added one point to " + event.getOptions().get(0).getAsUser().getAsMention());
                    } else {
                        eb.setDescription("Insufficient permissions");
                    }
                }
                case "remove_point" -> {
                    session.currentState = States.IGNORE;
                    if(verifyManageServerPerms(event.getMember())) {
                        SquizLeaderboard leaderboard = SquizHandler.squizLeaderboardHashMap.getOrDefault(event.getGuild().getIdLong(), new SquizLeaderboard());

                        leaderboard.removePoint(event.getUser().getIdLong());

                        SquizHandler.squizLeaderboardHashMap.putIfAbsent(event.getGuild().getIdLong(), leaderboard);

                        eb.setDescription("Removed one point to " + event.getOptions().get(0).getAsUser().getAsMention());
                    } else {
                        eb.setDescription("Insufficient permissions");
                    }
                }
                case "set_point" -> {
                    session.currentState = States.IGNORE;
                    if(verifyManageServerPerms(event.getMember())) {
                        SquizLeaderboard leaderboard = SquizHandler.squizLeaderboardHashMap.getOrDefault(event.getGuild().getIdLong(), new SquizLeaderboard());

                        leaderboard.setCustomPoint(event.getOptions().get(0).getAsUser().getIdLong(), event.getOptions().get(1).getAsInt());

                        SquizHandler.squizLeaderboardHashMap.putIfAbsent(event.getGuild().getIdLong(), leaderboard);

                        eb.setDescription("Set points for <@" + event.getOptions().get(0).getAsUser().getIdLong() + "> to " + event.getOptions().get(1).getAsInt());
                    } else {
                        eb.setDescription("Insufficient permissions");
                    }
                }
                case "get_players" -> {
                    if(verifyManageServerPerms(event.getMember())) {
                        session = new ViewingPlayersSession();
                        session.currentState = States.GETTING_PLAYERS;
                        StringBuilder users = new StringBuilder("\n");
                        SquizLeaderboard leaderboard = SquizHandler.squizLeaderboardHashMap.get(event.getGuild().getIdLong());
                        ArrayList<String> pages = new ArrayList<>();

                        if(leaderboard == null) {
                            eb.setDescription("No players have played the Squiz yet");
                        } else {
                            ArrayList<Long> sortedPlayers = new ArrayList<>(leaderboard.leaderboard.keySet());

                            sortedPlayers.sort(
                                    (o1, o2) -> leaderboard.leaderboard.get(o2).compareTo(leaderboard.leaderboard.get(o1))
                            );
                            String backupString;
                            pages = new ArrayList<>();
                            for(long playerId: sortedPlayers) {
                                backupString = users.toString();
                                users.append("<@").append(playerId).append("> = ").append(leaderboard.leaderboard.get(playerId)).append("\n");
                                if(users.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH) {
                                    pages.add(backupString + "\n");
                                    users = new StringBuilder("\n");
                                }
                            }
                            users.append("\n");
                            pages.add(users.toString());
                            eb.setDescription(pages.get(0));
                        }

                        ((ViewingPlayersSession) session).pages = pages;
                    } else {
                        eb.setDescription("Insufficient permissions");
                    }
                }
                case "clear_leaderboard" -> {
                    if(verifyManageServerPerms(event.getMember())) {
                        eb.setDescription("**Are you sure you want to clear the leaderboard?**\n\n**THIS IS IRREVERSIBLE**");
                        session.currentState = States.PENDING_CONFIRMATION;
                    } else {
                        eb.setDescription("Insufficient permissions");
                    }
                }
                case "get_tracking" -> {
                    session.currentState = States.IGNORE;
                    if(verifyManageServerPerms(event.getMember())) {
                        try {
                            eb.setTitle("Squiz User Tracking for " + event.getUser().getAsTag());
                            StringBuilder csvString = new StringBuilder("Date,Response Time,Is Correct\n");

                            for(SquizTracking.UserData.Response response: SquizHandler.squizTracking.serverMapping.get(event.getGuild().getIdLong()).get(event.getOptions().get(0).getAsUser().getIdLong()).responses) {
                                csvString.append(response.date).append(',').append(response.responseTime).append(',').append(response.isCorrect).append('\n');
                            }

                            eb.setDescription(
                                    getPastebinUrl(csvString.toString())
                            );
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        eb.setDescription("Insufficient permissions");
                    }
                }
            }
        }

        squizSessionHashMap.putIfAbsent(userId, session);

        return new CommandResponse<>(eb.build(), onlyEphemeral);
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        SubcommandData leaderboard = new SubcommandData("leaderboard", "Displays the leaderboard for the Squiz competitions");
        SubcommandData play = new SubcommandData("play", "Starts your personal Squiz questionairre");
        SubcommandData addPoint = new SubcommandData("add_point", "Adds a point for a user").addOption(OptionType.USER, "user", "Which user to add the point for", true);
        SubcommandData removePoint = new SubcommandData("remove_point", "Removes a point for a user").addOption(OptionType.USER, "user", "Which user to remove the point for", true);
        SubcommandData setPoint = new SubcommandData("set_point", "Sets the amount of points for a user").addOption(OptionType.USER, "user", "Which user to set custom points for", true).addOption(OptionType.INTEGER, "points", "Custom points to set", true);
        SubcommandData getPlayers = new SubcommandData("get_players", "Generates a list of players");
        SubcommandData clearLeaderboard = new SubcommandData("clear_leaderboard", "Clears the leaderboard");
        SubcommandData getUserTrackingDump = new SubcommandData("get_tracking", "Gets the Random Squiz user tracking data").addOption(OptionType.USER, "user", "Which user to get tracking data from", true);

        Command cmd = jda.upsertCommand(name, description).addSubcommands(leaderboard, play, addPoint, removePoint, setPoint, getPlayers, clearLeaderboard, getUserTrackingDump).complete();

        commandId = cmd.getIdLong();
    }

    @Override
    public void runButtonInteraction(@NonNull ButtonInteractionEvent event) {
        Collection<ItemComponent> buttons = null;
        EmbedBuilder eb = new EmbedBuilder();
        //noinspection ConstantConditions
        long serverId = event.getGuild().getIdLong();
        long userId = event.getUser().getIdLong();

        SquizSession session = squizSessionHashMap.get(userId);

        eb.setTitle("Squiz!");

        // Check if the event is from an active Squiz
        if(randomSquizSessionsHashMap.get(serverId) != null) {
            if(event.getMessage().getIdLong() == randomSquizSessionsHashMap.get(serverId).message.getIdLong()) {
                session = randomSquizSessionsHashMap.get(serverId);
                eb.setTitle("Random Squiz!");
                if (((RandomSquizSession) session).responses.size() >= 4 || (Instant.now().getEpochSecond() >= ((RandomSquizSession) session).timeReacted + 15 && ((RandomSquizSession) session).timeReacted != 0)) { // catch this if the bot hasn't finished editing/expiring the message
                    eb.setDescription("Too late! Either four others have answered or 15 seconds has passed since the first answer!");

                    event.replyEmbeds(eb.build()).setEphemeral(true).complete();
                    return;
                } else if(((RandomSquizSession) session).responses.containsKey(userId)) { // tried to switch responses
                    eb.setDescription("You cannot change responses!");

                    event.replyEmbeds(eb.build()).setEphemeral(true).complete();
                } else {
                    //noinspection ConstantConditions
                    ((RandomSquizSession) session).responses.put(userId, getAnswerChar(event.getButton().getId()));

                    eb.setDescription("You chose " + ((RandomSquizSession) session).responses.get(userId));
                    event.replyEmbeds(eb.build()).setEphemeral(true).complete();
                    if(((RandomSquizSession) session).responses.size() == 4) { // if the size is now 4, interrupt the thread
                        ((RandomSquizSession) session).expiringThread.interrupt();
                    } else if(((RandomSquizSession) session).expiringThread == null) { // else, check if thread is null to start it
                        randomSquizExpires((RandomSquizSession) session);
                        ((RandomSquizSession) session).timeReacted = Instant.now().getEpochSecond();
                    }
                    HashMap<Long, SquizTracking.UserData> tracking = SquizHandler.squizTracking.serverMapping.getOrDefault(serverId, new HashMap<>());
                    SquizTracking.UserData userData = tracking.getOrDefault(userId, new SquizTracking.UserData());
                    SquizTracking.UserData.Response response = new SquizTracking.UserData.Response();
                    LocalDate date = LocalDate.now();

                    response.responseTime = Instant.now().toEpochMilli() - ((RandomSquizSession) session).epochInMillisecondsWhenPosted;
                    response.date = date.getMonth() + "-" + date.getDayOfMonth() + "-" + date.getYear();
                    response.isCorrect = session.correctCurrentAnswer == getAnswerChar(event.getButton().getId());

                    userData.responses.add(response);

                    tracking.put(userId, userData);
                    SquizHandler.squizTracking.serverMapping.put(serverId, tracking);
                }
                return;
            } else {
                squizSessionHashMap.put(userId, session);
            }
        } else {
            squizSessionHashMap.put(userId, session);
        }

        //noinspection ConstantConditions
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
            case "squiz_answer_a", "squiz_answer_b", "squiz_answer_c", "squiz_answer_d", "squiz_answer_false", "squiz_answer_true" -> {
                if(checkIfCorrectAnswer(getAnswerChar(event.getButton().getId()), session.correctCurrentAnswer, session)) {
                    session.currentScore++;
                }

                buttons = handleNextQuestion(session, eb);
            }
            case "squiz_leaderboard" -> generateLeaderboard(eb, serverId, userId);
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
            case "squiz_next" -> {
                buttons = new ArrayList<>();
                ViewingPlayersSession viewingSession = (ViewingPlayersSession) session;

                viewingSession.currentPage = viewingSession.currentPage + 1;
                eb.setDescription(viewingSession.pages.get(viewingSession.currentPage));

                if(viewingSession.pages.size() >= viewingSession.currentPage + 1) {
                    buttons.add(Button.primary("squiz_next", "Next").asDisabled());
                } else {
                    buttons.add(Button.primary("squiz_next", "Next").asDisabled());
                }

                buttons.add(Button.primary("squiz_previous", "Previous").asEnabled());
                buttons.add(Button.secondary("squiz_page", viewingSession.currentPage + 1 + "/" + viewingSession.pages.size()).asDisabled());
            }
            case "squiz_previous" -> {
                buttons = new ArrayList<>();
                ViewingPlayersSession viewingSession = (ViewingPlayersSession) session;

                viewingSession.currentPage = viewingSession.currentPage - 1;
                eb.setDescription(viewingSession.pages.get(viewingSession.currentPage));

                buttons.add(Button.primary("squiz_next", "Next").asEnabled());

                if(viewingSession.currentPage == 0) {
                    buttons.add(Button.primary("squiz_previous", "Previous").asDisabled());
                } else {
                    buttons.add(Button.primary("squiz_previous", "Previous").asEnabled());
                }

                buttons.add(Button.secondary("squiz_page", viewingSession.currentPage + 1 + "/" + viewingSession.pages.size()).asDisabled());
            }
            case "squiz_confirm" -> {
                SquizLeaderboard squizLeaderboard = SquizHandler.squizLeaderboardHashMap.get(serverId);

                squizLeaderboard.leaderboard.clear();
                updateLeaderboard(serverId);
                try {
                    SquizHandler.updateSquizLeaderboard();
                    eb.setDescription("Leaderboard cleared");
                } catch (IOException e) {
                    e.printStackTrace();
                    eb.setDescription("Leaderboard cleared, but not updated on disk!");
                } finally {
                    squizSessionHashMap.remove(userId);
                }
            }
            case "squiz_cancel" -> {
                squizSessionHashMap.remove(userId);
                eb.setDescription("Leaderboard not cleared");
            }
        }

        MessageEditCallbackAction pending = event.editComponents().setEmbeds(eb.build());

        if(buttons != null)
            pending.setActionRow(buttons).complete();
        else
            pending.complete();
    }

    @Override
    @NonNull
    public Collection<ItemComponent> addButtons(@NonNull GenericCommandInteractionEvent event) {
        SquizSession session = squizSessionHashMap.get(event.getUser().getIdLong());
        States state = session.currentState;

        if(state == States.IGNORE) {
            return Collections.emptyList();
        } else if (state == States.IN_PROGRESS && !event.getSubcommandName().equalsIgnoreCase("play")) {
            return List.of(
                    buttonHashMap.get("A"),
                    buttonHashMap.get("B"),
                    buttonHashMap.get("C"),
                    buttonHashMap.get("D"),
                    buttonHashMap.get("End")
            );
        } else if (state == States.COMPLETE) {
            return List.of(buttonHashMap.get("Review"));
        } else if (event.getSubcommandName().equalsIgnoreCase("play") && state == States.IN_PROGRESS && session.currentQuestion != 0) {
            return List.of(buttonHashMap.get("End"));
        } else if (state == States.VIEWING_LEADERBOARD) {
            return Collections.emptyList();
        } else if (state == States.GETTING_PLAYERS) {
            if(((ViewingPlayersSession) session).pages.size() > 1) {
                return List.of(
                        buttonHashMap.get("Previous"),
                        buttonHashMap.get("1/?").withLabel("1/" + ((ViewingPlayersSession) session).pages.size()),
                        buttonHashMap.get("Next")
                );
            } else {
                return Collections.emptyList();
            }
        } else if(state == States.PENDING_CONFIRMATION) {
            return List.of(
                    buttonHashMap.get("Confirm"),
                    buttonHashMap.get("Cancel")
            );
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
        session.epochInMillisecondsWhenPosted = Instant.now().toEpochMilli();
        ServerCache.addNewMessage(message.getGuild().getIdLong(), message.getChannel().getIdLong(), message.getIdLong());
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

    public void randomSquizExpires(@NonNull RandomSquizSession session) {
        session.expiringThread = new Thread(() -> {
            try {
                Thread.sleep(15000);
            } catch (InterruptedException ignored) { // needs to be interrupted to indicate it hit the 4th response and stop waiting

            } finally {
                Map<Long, SquizLeaderboard> squizLeaderboardMap = SquizHandler.squizLeaderboardHashMap;
                SquizLeaderboard leaderboard = squizLeaderboardMap.getOrDefault(session.serverId, new SquizLeaderboard());
                EmbedBuilder eb = new EmbedBuilder();
                EmbedBuilder touchGrass = new EmbedBuilder();
                StringBuilder correctUsers = new StringBuilder();
                StringBuilder wrongUsers = new StringBuilder();
                StringBuilder usersWhoNeedToTouchGrass = new StringBuilder();
                JDA api = Launcher.api;

                RandomSquizSession.consecutiveAnswers.keySet().removeIf(
                        id -> (!session.responses.containsKey(id))
                );

                // check who got the correct answer
                for(Long userId: session.responses.keySet()) {
                    Integer consecutiveCounter = RandomSquizSession.consecutiveAnswers.getOrDefault(userId, 0);

                    if(checkIfCorrectAnswer(session.responses.get(userId), session.correctCurrentAnswer, session)) {
                        correctUsers.append(api.retrieveUserById(userId).complete().getAsMention()).append(" ");
                        leaderboard.addPoint(userId);
                    } else {
                        wrongUsers.append(api.retrieveUserById(userId).complete().getAsMention()).append(" ");
                    }
                    RandomSquizSession.consecutiveAnswers.put(userId, ++consecutiveCounter);
                    if(RandomSquizSession.consecutiveAnswers.get(userId) >= 5) {
                        usersWhoNeedToTouchGrass.append(api.retrieveUserById(userId).complete().getAsMention()).append(" ");
                    }
                }

                squizLeaderboardMap.putIfAbsent(session.serverId, leaderboard);

                if(usersWhoNeedToTouchGrass.length() != 0) {
                    touchGrass.setTitle("Please Touch Grass");
                    touchGrass.setDescription(usersWhoNeedToTouchGrass + " have played Squiz at least 5 times consecutively.\n\nYou should definitely drink water, rest, and touch grass");
                    session.message.getChannel().asTextChannel().sendMessageEmbeds(touchGrass.build()).queue(
                            (response) -> letMessageExpire(this, response), Throwable::printStackTrace
                    );
                }

                try {
                    updateLeaderboard(session.serverId);
                    SquizHandler.updateSquizLeaderboard();
                    ServerDataHandler.updateServerData();
                    SquizHandler.updateSquizTracking();
                } catch (IOException e) {
                    e.printStackTrace();
                    eb.addField("Warning", "Leaderboard and/or other data not saved to disk!", false);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    eb.addField("Warning", "Leaderboard channel isn't set properly!", false);
                }

                eb.setTitle("Random Squiz!");
                eb.addField("Question", session.questions.get(0).question, false);
                eb.addField("Correct Answer", session.questions.get(0).answer, false);
                if(correctUsers.length() != 0) {
                    eb.addField("Users Who Got It Correct", correctUsers.toString(), false);
                }
                if(wrongUsers.length() != 0) {
                    eb.addField("Users Who Got It Wrong", wrongUsers.toString(), false);
                }

                session.message.editMessageComponents().setEmbeds(eb.build()).complete();

                SquizHandler.scheduleServer(session.serverId);
                randomSquizSessionsHashMap.remove(session.serverId); // Removes the reference
                letMessageExpire(this, session.message);
            }
        });

        session.expiringThread.start();
    }

    private boolean checkIfCorrectAnswer(char answer, char correctAnswer, @NonNull SquizSession session) {
        boolean response = answer == correctAnswer;

        if(!response) {
            session.missedQuestions.put(session.missedQuestions.size(), session.questions.get(session.currentQuestion - 1));
        }

        return response;
    }

    @NonNull
    private List<ItemComponent> handleNextQuestion(@NonNull SquizSession session, @NonNull EmbedBuilder eb) {
        if(session.currentQuestion == session.questions.size()) {
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

    @NonNull
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

    private String getPastebinUrl(@NonNull String contentToPaste) throws Exception {
        HttpsURLConnection con = (HttpsURLConnection) new URL("https://pastebin.com/api/api_post.php").openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        String url;

        OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
        writer.write("api_dev_key=" + SquizHandler.pastebinApiKey + "&api_paste_code=" + contentToPaste + "&api_option=paste");
        writer.flush();
        writer.close();

        int responseCode = con.getResponseCode();

        if(responseCode == HttpURLConnection.HTTP_ACCEPTED || responseCode == HttpURLConnection.HTTP_OK) {
            url = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
        } else {
            throw new Exception("Failed to get URL\n\n" + new BufferedReader(new InputStreamReader(con.getInputStream())).readLine());
        }

        return url;
    }

    private static void generateLeaderboard(@NonNull EmbedBuilder eb, long serverId) {
        eb.setTitle("Squiz Leaderboard");
        eb.setDescription("Shows the top 5 people on the leaderboard for the server plus where you are currently in the leaderboard");
        SquizLeaderboard leaderboard = SquizHandler.squizLeaderboardHashMap.getOrDefault(serverId, new SquizLeaderboard());
        ArrayList<Long> topFive = leaderboard.getTopFive();
        Guild guild = Launcher.api.getGuildById(serverId);

        for(int i = 0; i < 5 && i < topFive.size(); i++) {
            eb.addField(String.format("%d. %s", i + 1, guild.retrieveMemberById(topFive.get(i)).complete().getUser().getAsTag()), String.format("%d", leaderboard.leaderboard.get(topFive.get(i))), false);
        }
    }

    private static void generateLeaderboard(@NonNull EmbedBuilder eb, long serverId, long userId) {
        eb.setTitle("Squiz Leaderboard");
        eb.setDescription("Shows the top 5 people on the leaderboard for the server plus where you are currently in the leaderboard");
        SquizLeaderboard leaderboard = SquizHandler.squizLeaderboardHashMap.getOrDefault(serverId, new SquizLeaderboard());
        ArrayList<Long> topFive = leaderboard.getTopFive();
        Guild guild = Launcher.api.getGuildById(serverId);

        if(topFive.contains(userId)) {
            for(int i = 0; i < 5 && i < topFive.size(); i++) {
                eb.addField(String.format("%d. %s", i + 1, guild.retrieveMemberById(topFive.get(i)).complete().getUser().getAsTag()), String.format("%d", leaderboard.leaderboard.get(topFive.get(i))), false);
            }
        } else {
            for(int i = 0; i < 5 && i < topFive.size(); i++) {
                eb.addField(String.format("%d. %s", i + 1, guild.retrieveMemberById(topFive.get(i)).complete().getUser().getAsTag()), String.format("%d", leaderboard.leaderboard.get(topFive.get(i))), false);
            }
            if(topFive.size() == 5 && leaderboard.getPositionOfUser(userId) != -1) {
                eb.addField("...", "...", false);
                eb.addField(
                        String.format(
                                "%d. %s",
                                leaderboard.getPositionOfUser(userId),
                                guild.retrieveMemberById(userId).complete().getUser().getAsTag()
                        ),
                        String.format(
                                "%d",
                                leaderboard.leaderboard.get(userId)
                        ), false);
            }
        }
    }

    private static char getAnswerChar(@NonNull String buttonId) {
        switch(buttonId) {
            case "squiz_answer_a" -> {
                return 'A';
            }
            case "squiz_answer_b" -> {
                return 'B';
            }
            case "squiz_answer_c" -> {
                return 'C';
            }
            case "squiz_answer_d" -> {
                return 'D';
            }
            case "squiz_answer_true" -> {
                return 'T';
            }
            case "squiz_answer_false" -> {
                return 'F';
            }
        }
        return 0;
    }

    private static boolean verifyManageServerPerms(@NonNull Member member) {
        return member.hasPermission(Permission.MANAGE_SERVER);
    }
}
