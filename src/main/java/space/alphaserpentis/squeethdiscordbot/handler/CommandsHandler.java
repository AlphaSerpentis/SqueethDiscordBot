package space.alphaserpentis.squeethdiscordbot.handler;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;
import space.alphaserpentis.squeethdiscordbot.commands.*;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class CommandsHandler extends ListenerAdapter {
    public static final HashMap<String, ICommand> mappingOfCommands = new HashMap<>() {{
        put("ping", new Ping());
        put("shutdown", new Shutdown());
        put("about", new About());
        put("greeks", new Greeks());
        put("stats", new Stats());
        put("help", new Help());
    }};

    public static long adminUserID;

    public static void checkAndSetSlashCommands() {
        JDA api = Launcher.api;
        List<Command> listOfActiveCommands = api.retrieveCommands().complete();
        List<String> detectedCommandNames = new ArrayList<>();

        // Checks for the detected commands
        for (Iterator<Command> it = listOfActiveCommands.iterator(); it.hasNext(); ) {
            Command cmd = it.next();
            if(mappingOfCommands.containsKey(cmd.getName())) {
                mappingOfCommands.get(cmd.getName()).setCommandId(cmd.getIdLong());
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
                Command cmd = api.upsertCommand(cmdName, mappingOfCommands.get(cmdName).getDescription()).complete();

                mappingOfCommands.get(cmdName).setCommandId(cmd.getIdLong());
            }
        }

    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        ICommand cmd = mappingOfCommands.get(event.getName());
        Object response = cmd.runCommand(event.getUser().getIdLong());

        if(cmd.isOnlyEmbed()) {
            event.replyEmbeds((MessageEmbed) response).setEphemeral(true).queue();
        } else {
            event.reply((Message) response).setEphemeral(true).queue();
        }
    }
}
