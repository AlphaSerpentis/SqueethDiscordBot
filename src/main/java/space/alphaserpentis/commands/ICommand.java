package space.alphaserpentis.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public abstract class ICommand {

    protected String name, description;
    protected long commandId;
    protected boolean onlyEmbed;

    abstract public Object runCommand(long userId);

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

}
