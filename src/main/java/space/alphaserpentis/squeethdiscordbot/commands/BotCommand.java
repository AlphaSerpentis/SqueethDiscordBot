package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public abstract class BotCommand {

    protected String name, description;
    protected long commandId;
    protected boolean onlyEmbed, onlyEphemeral, isActive = true;
    protected HashMap<String, Button> buttonHashMap = new HashMap<>();

    abstract public Object runCommand(long userId, @NotNull SlashCommandInteractionEvent event);

    abstract public void addCommand(JDA jda);
    abstract public void updateCommand(JDA jda);

    public void setCommandId(long id) {
        commandId = id;
    }
    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public long getCommandId() {
        return commandId;
    }
    public boolean isOnlyEmbed() { return onlyEmbed; }
    public boolean isOnlyEphemeral() {
        return onlyEphemeral || !isActive;
    }
    public boolean isActive() {
        return isActive;
    }
    public Button getButton(String key) {
        return buttonHashMap.get(key);
    }
    public HashMap<String, Button> getButtonHashMap() {
        return buttonHashMap;
    }

}
