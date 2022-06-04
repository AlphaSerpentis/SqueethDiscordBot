package space.alphaserpentis.squeethdiscordbot.main;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import space.alphaserpentis.squeethdiscordbot.handler.*;

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
        PositionsDataHandler.init(Path.of(args[4]), Path.of(args[5]));

        // Initialize the web3 instance
        EthereumRPCHandler.web3 = Web3j.build(new HttpService(args[6]));
        EthereumRPCHandler.url = new URL(args[6]);

        // Verify commands are up-to-date
        CommandsHandler.checkAndSetSlashCommands(Boolean.parseBoolean(args[7]));

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
     * @param args Requires 8 arguments for: (1) bot token, (2) Laevitas API key, (3) bot admin Discord user ID, (4) file path to server JSON file, (5) file path to transfers JSON file, (6) file path to prices JSON file, (7) HTTPS link to Ethereum RPC node, (8) update commands
     * @throws Exception If 8 arguments aren't passed exactly
     */
    public static void main(String[] args) throws Exception {

        if(args.length != 8)
            throw new Exception("Invalid arg count; Requires 8 arguments for: (1) bot token, (2) Laevitas API key, (3) bot admin Discord user ID, (4) file path to server JSON file, (5) file path to transfers JSON file, (6) file path to prices JSON file, (7) HTTPS link to Ethereum RPC node, (8) update commands");
        else {
            new Launcher(args);
        }


    }

}