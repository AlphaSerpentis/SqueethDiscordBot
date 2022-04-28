package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.List;

public abstract class BotCommand {

    protected String name, description;
    protected OptionData options;
    protected long commandId;
    protected boolean onlyEmbed;

    abstract public Object runCommand(long userId);
    abstract public Object runCommand(long userId, List<OptionMapping> optionMappingList);

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
    public OptionData getOptions() {
        return options;
    }
    public long getCommandId() {
        return commandId;
    }
    public boolean isOnlyEmbed() { return onlyEmbed; }

}
