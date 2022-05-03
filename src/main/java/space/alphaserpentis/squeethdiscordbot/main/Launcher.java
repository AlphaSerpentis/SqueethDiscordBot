package space.alphaserpentis.squeethdiscordbot.main;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import space.alphaserpentis.squeethdiscordbot.handler.CommandsHandler;
import space.alphaserpentis.squeethdiscordbot.handler.LaevitasHandler;
import space.alphaserpentis.squeethdiscordbot.handler.ServerDataHandler;
import space.alphaserpentis.squeethdiscordbot.handler.StatusHandler;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public class Launcher {

    /**
     * The JDA instance being utilized
     */
    public static JDA api;

    public Launcher(String[] args) throws LoginException, InterruptedException, IOException {
        JDABuilder builder = JDABuilder.createDefault(args[0]);

        // Set variables
        LaevitasHandler.API_URL = new URL("https://gateway.laevitas.ch/");
        LaevitasHandler.KEY = args[1];
        CommandsHandler.adminUserID = Long.parseLong(args[2]);

        // Configure the bot
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.ONLINE_STATUS, CacheFlag.CLIENT_STATUS, CacheFlag.EMOTE);
        builder.disableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.GUILD_MESSAGE_REACTIONS);
        builder.enableIntents(GatewayIntent.GUILD_MEMBERS);
        builder.addEventListeners(new CommandsHandler());
        builder.addEventListeners(new ServerDataHandler());

        // Build and Set the API
        api = builder.build();

        // Wait
        api.awaitReady();

        // Initialize the server data and load them
        ServerDataHandler.init(Path.of(args[3]));

        // Verify commands are up-to-date
        CommandsHandler.checkAndSetSlashCommands(Boolean.parseBoolean(args[4]));

        // Start the StatusHandler
        new StatusHandler();
    }

    /**
     * Shuts down the program
     */
    public static void shutdown() {
        api.shutdown();
        System.exit(0);
    }

    /**
     *
     * @param args Requires 4 arguments for: (1) bot token, (2) Laevitas API, (3) bot admin Discord user ID, (4) file path to server JSON file, (5) update commands
     * @throws Exception If 4 arguments aren't passed exactly
     */
    public static void main(String[] args) throws Exception {

        if(args.length != 5)
            throw new Exception("Invalid arg count; requires 4 arguments for: (1) bot token, (2) Laevitas API, (3) bot admin Discord user ID, (4) file path to server JSON file, (5) update commands");
        else {
            new Launcher(args);
        }


    }

}