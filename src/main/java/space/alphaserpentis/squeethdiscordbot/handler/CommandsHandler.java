package space.alphaserpentis.squeethdiscordbot.handler;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;
import space.alphaserpentis.squeethdiscordbot.commands.*;
import space.alphaserpentis.squeethdiscordbot.data.ServerCache;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import java.util.*;

public class CommandsHandler extends ListenerAdapter {
    public static final HashMap<String, BotCommand> mappingOfCommands = new HashMap<>() {{
        put("shutdown", new Shutdown());
        put("about", new About());
        put("greeks", new Greeks());
        put("stats", new Stats());
        put("help", new Help());
        put("funding", new Funding());
        put("settings", new Settings());
        put("resources", new Resources());
        put("clean", new Clean());
        put("crab", new Crab());
        put("position", new Position());
    }};

    public static long adminUserID;

    public static void checkAndSetSlashCommands(boolean updateCommands) {
        JDA api = Launcher.api;
        List<Command> listOfActiveCommands = api.retrieveCommands().complete();
        List<String> detectedCommandNames = new ArrayList<>();

        // Checks for the detected commands
        for (Iterator<Command> it = listOfActiveCommands.iterator(); it.hasNext(); ) {
            Command cmd = it.next();
            if(mappingOfCommands.containsKey(cmd.getName())) {
                BotCommand botCmd = mappingOfCommands.get(cmd.getName());
                botCmd.setCommandId(cmd.getIdLong());
                if(updateCommands)
                    botCmd.updateCommand(api);

                detectedCommandNames.add(cmd.getName());

                it.remove();
            }
        }

        // Fills in any gaps or removes any commands
        for(Command cmd: listOfActiveCommands) { // Removes unused commands
            System.out.println("[CommandsHandler] Removing slash command: " + cmd.getName());
            api.deleteCommandById(cmd.getId()).complete();
        }

        if(detectedCommandNames.size() < mappingOfCommands.size()) { // Adds new commands
            List<String> missingCommands = new ArrayList<>(mappingOfCommands.keySet());

            missingCommands.removeAll(detectedCommandNames);

            for(String cmdName: missingCommands) {
                System.out.println("[CommandsHandler] Adding new slash command: " + cmdName);
                BotCommand cmd = mappingOfCommands.get(cmdName);
                cmd.addCommand(api);
            }
        }

    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        BotCommand cmd = mappingOfCommands.get(event.getName());

        new Thread(() -> {
            Message message;
            message = BotCommand.handleReply(event, cmd);

            if(message != null && event.getGuild() != null) {
                ServerCache.addNewMessage(event.getGuild().getIdLong(), message);
            }
        }).start();

    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        for(String cmdName: mappingOfCommands.keySet()) {
            if(event.getButton().getId().contains(cmdName)) {
                ((ButtonCommand) mappingOfCommands.get(cmdName)).runButtonInteraction(event);
            }
        }
    }
}
