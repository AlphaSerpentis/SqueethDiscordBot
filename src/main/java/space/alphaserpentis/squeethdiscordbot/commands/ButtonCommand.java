package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public abstract class ButtonCommand extends BotCommand {
    abstract public void runButtonInteraction(@NotNull ButtonInteractionEvent event);
    abstract public Collection<ItemComponent> addButtons();
}
