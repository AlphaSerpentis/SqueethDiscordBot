package space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.reactivex.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import space.alphaserpentis.squeethdiscordbot.data.bot.ConditionsCarrier;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.data.SqueethVolatility;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.data.TrackedData;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.data.VaultCollateralization;
import space.alphaserpentis.squeethdiscordbot.handler.serialization.ConditionsDeserializer;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class NotifyHandler {

    public static final ArrayList<TrackedData<?>> trackedData = new ArrayList<>() {{
        add(new SqueethVolatility());
        add(new VaultCollateralization());
    }};
    public static Map<String, ConditionsCarrier> conditions = new HashMap<>();
    private static ScheduledExecutorService scheduledExecutor;
    private static final ExecutorService cachedExecutor = Executors.newCachedThreadPool();
    private static final EmbedBuilder defaultEmbedBuilder = new EmbedBuilder() {{
        setTitle("User Notification");
        setColor(Color.WHITE);
    }};
    private static Path dataPath;

    public static void init(@NonNull Path dataPath) throws IOException {
        scheduledExecutor = new ScheduledThreadPoolExecutor(trackedData.size());
        NotifyHandler.dataPath = dataPath;

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Map.class, new ConditionsDeserializer())
                .create();

        Reader reader = Files.newBufferedReader(dataPath);
        conditions = gson.fromJson(
                reader, new TypeToken<Map<String, ConditionsCarrier>>(){}.getType()
        );

        if(conditions == null)
            conditions = new HashMap<>();

        scheduledExecutor.scheduleAtFixedRate(NotifyHandler::updateAndNotify, 0, 2, TimeUnit.MINUTES);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void updateAndNotify() {
        for(TrackedData<?> data : trackedData) {
            data.update();
            ConditionsCarrier carrier = conditions.get(data.getShortName());
            if(carrier == null)
                continue;

            for(Long userId : carrier.getUsersSubscribed()) {
                for(Condition condition : carrier.getConditions(userId)) {
                    if(data.checkCondition(condition)) {
                        if(!condition.isConditionActive) {
                            notifyUser(userId, data, condition);
                            condition.isConditionActive = true;
                        }
                        break;
                    } else {
                        condition.isConditionActive = false;
                    }
                }
            }
        }
    }

    /**
     * Adds a user to the list of users to be notified when a condition is met
     * @param userId long id of the user
     * @param trackedData TrackedData object to subscribe to
     * @param condition Condition object to subscribe to
     */
    public static void addUserWithCondition(long userId, TrackedData<?> trackedData, Condition<?> condition) {
        if(!conditions.containsKey(trackedData.getShortName()))
            conditions.put(trackedData.getShortName(), new ConditionsCarrier(new HashMap<>()));

        for(Condition<?> existingCondition : conditions.get(trackedData.getShortName()).getConditions(userId)) {
            if(existingCondition.equals(condition)) {
                return;
            }
        }

        conditions.get(trackedData.getShortName()).addCondition(userId, condition);

        try {
            writeDataToFile(conditions);
        } catch (IOException ignored) {
        }
    }

    public static void removeUserWithCondition(long userId, TrackedData<?> trackedData, Condition<?> condition) {
        if(!conditions.containsKey(trackedData.getShortName()))
            return;

        conditions.get(trackedData.getShortName()).removeCondition(userId, condition);

        try {
            writeDataToFile(conditions);
        } catch (IOException ignored) {
        }
    }

    public static ArrayList<Condition<?>> getConditionsForDataFromUser(long userId, TrackedData<?> trackedData) {
        if(!conditions.containsKey(trackedData.getShortName()))
            return new ArrayList<>();

        return conditions.get(trackedData.getShortName()).getConditions(userId);
    }

    public static void editUserCondition(long userId, TrackedData<?> trackedData, Condition<?> condition, Condition<?> newCondition) {
        if(!conditions.containsKey(trackedData.getShortName()))
            return;

        conditions.get(trackedData.getShortName()).editCondition(userId, condition, newCondition);

        try {
            writeDataToFile(conditions);
        } catch (IOException ignored) {
        }
    }

    public static void notifyUser(long userId, TrackedData<?> trackedData, Condition<?> condition) {
        cachedExecutor.execute(() -> Launcher.api.getUserById(userId).openPrivateChannel().queue((channel) -> {
            EmbedBuilder eb = new EmbedBuilder(defaultEmbedBuilder);
            eb.setDescription("The condition you set for " + trackedData.getName() + " has been met.");
            eb.addField("Current", trackedData.getFormattedData(), false);
            eb.addField("Condition", condition.getFormattedCondition(trackedData), false);

            channel.sendMessageEmbeds(eb.build()).queue();
        }));
    }

    public static ArrayList<TrackedData<?>> getSubscribedData(long idLong) {
        ArrayList<TrackedData<?>> subscribedData = new ArrayList<>();
        for (TrackedData<?> data : trackedData) {
            if(conditions.containsKey(data.getShortName())) {
                if(conditions.get(data.getShortName()).getUsersSubscribed().contains(idLong))
                    subscribedData.add(data);
            }
        }
        return subscribedData;
    }

    public static void writeDataToFile(Object data) throws IOException {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        Writer writer = Files.newBufferedWriter(dataPath);
        gson.toJson(data, writer);
        writer.close();
    }
}
