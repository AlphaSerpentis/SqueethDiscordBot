package space.alphaserpentis.main;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import space.alphaserpentis.handler.CommandsHandler;
import space.alphaserpentis.handler.LaevitasHandler;
import space.alphaserpentis.handler.StatusHandler;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URL;

public class Launcher {

    public static JDA api;

    public Launcher(String[] args) throws LoginException, InterruptedException, IOException {
        JDABuilder builder = JDABuilder.createDefault(args[0]);

        // Set variables
        LaevitasHandler.API_URL = new URL("https://gateway.laevitas.ch/");
        LaevitasHandler.KEY = args[1];
        CommandsHandler.adminUserID = Long.parseLong(args[2]);

        // Configure the bot
        builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE);
        builder.disableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.GUILD_MESSAGE_REACTIONS);
        builder.addEventListeners(new CommandsHandler());

        // Build and Set the API
        api = builder.build();

        // Wait
        api.awaitReady();

        // Verify commands are up-to-date
        CommandsHandler.checkAndSetSlashCommands();

        // Start the StatusHandler
        new StatusHandler();
    }

    public static void shutdown() {
        api.shutdown();
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {

        if(args.length != 3)
            throw new Exception("Invalid arg count; requires 3 arguments for: (1) bot token, (2) Laevitas API, (3) bot admin Discord user ID");
        else {
            new Launcher(args);
        }


    }

}