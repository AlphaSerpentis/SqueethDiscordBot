// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.main;

import com.google.gson.Gson;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import space.alphaserpentis.squeethdiscordbot.data.BotSettings;
import space.alphaserpentis.squeethdiscordbot.handler.*;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Launcher {

    /**
     * The JDA instance being utilized
     */
    public static JDA api;

    public Launcher(String[] args) throws LoginException, InterruptedException, IOException {
        Gson gson = new Gson();

        BotSettings settings = gson.fromJson(Files.newBufferedReader(Paths.get(args[0])), BotSettings.class);

        JDABuilder builder = JDABuilder.createDefault(settings.discordBotKey);

        // Set variables
        LaevitasHandler.API_URL = new URL("https://gateway.laevitas.ch/");
        LaevitasHandler.KEY = settings.laevitasKey;
        CommandsHandler.adminUserID = settings.botAdmin;

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
        ServerDataHandler.init(Path.of(settings.serverData));
        PositionsDataHandler.init(Path.of(settings.transfersData), Path.of(settings.pricesData));

        // Initialize the web3 instance
        EthereumRPCHandler.web3 = Web3j.build(new HttpService(String.valueOf(settings.ethereumRPC)));
        EthereumRPCHandler.url = new URL(settings.ethereumRPC);

        // Verify commands are up-to-date
        CommandsHandler.checkAndSetSlashCommands(settings.updateCommandsAtLaunch);

        // Start the StatusHandler
        new StatusHandler();

        // Initialize SquizHandler
        SquizHandler.init(Path.of(settings.squizLeaderboard), Path.of(settings.squizQuestions));
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
     * @param args Requires 1 argument: path to the settings.json file
     * @throws Exception If 1 argument is not provided
     */
    public static void main(String[] args) throws Exception {
        if(args.length != 1)
            throw new Exception("Invalid arg count; Requires 1 argument: path to the settings.json file");
        else {
            new Launcher(args);
        }
    }
}