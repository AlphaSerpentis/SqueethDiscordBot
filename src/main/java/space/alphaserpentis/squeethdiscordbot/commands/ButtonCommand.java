// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;

public abstract class ButtonCommand<T> extends BotCommand<T> {
    protected final HashMap<String, Button> buttonHashMap = new HashMap<>();

    public ButtonCommand() {
        super();
    }

    public ButtonCommand(@Nonnull BotCommandOptions options) {
        super(options);
    }

    abstract public void runButtonInteraction(@Nonnull ButtonInteractionEvent event);
    abstract public Collection<ItemComponent> addButtons(@Nonnull GenericCommandInteractionEvent event);
    @Nullable
    public Button getButton(String key) {
        return buttonHashMap.get(key);
    }
    @Nonnull
    public HashMap<String, Button> getButtonHashMap() {
        return buttonHashMap;
    }
}
