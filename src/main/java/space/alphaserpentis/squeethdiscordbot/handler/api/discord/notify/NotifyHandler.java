package space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify;

import net.dv8tion.jda.api.EmbedBuilder;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.data.SqueethVolatility;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.data.TrackedData;
import space.alphaserpentis.squeethdiscordbot.handler.api.discord.notify.data.VaultCollateralization;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.*;

public class NotifyHandler {
    private static ArrayList<TrackedData<?>> trackedData = new ArrayList<>() {{
        add(new SqueethVolatility());
        add(new VaultCollateralization());
    }};
    private static ScheduledExecutorService scheduledExecutor;
    private static ExecutorService cachedExecutor = Executors.newCachedThreadPool();
    private static final EmbedBuilder defaultEmbedBuilder = new EmbedBuilder() {{
        setTitle("User Notification");
        setColor(Color.WHITE);
    }};

    public static void init() {
        scheduledExecutor = new ScheduledThreadPoolExecutor(trackedData.size());

        for(TrackedData<?> data : trackedData) {
            scheduledExecutor.scheduleAtFixedRate(() -> {
                data.update();
                for(long userId : data.usersEligibleForNotification()) {
                    notifyUser(userId, data);
                }
            },0, 5, TimeUnit.MINUTES);
        }
    }

    public static void notifyUser(long userId, TrackedData<?> trackedData) {
        cachedExecutor.execute(() -> Launcher.api.getUserById(userId).openPrivateChannel().queue((channel) -> {
            EmbedBuilder eb = new EmbedBuilder(defaultEmbedBuilder);
            eb.setDescription("The condition you set for " + trackedData.getName() + " has been met.");
            eb.addField("Current", trackedData.getCurrentData().toString(), false);
        }));
    }
}
