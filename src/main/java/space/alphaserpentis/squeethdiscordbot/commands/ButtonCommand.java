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

public abstract class ButtonCommand extends BotCommand {
    protected HashMap<String, Button> buttonHashMap = new HashMap<>();

    abstract public void runButtonInteraction(@Nonnull ButtonInteractionEvent event);
    abstract public Collection<ItemComponent> addButtons(@Nonnull GenericCommandInteractionEvent event);
    @Nullable
    public Button getButton(String key) {
        return buttonHashMap.get(key);
    }
    @Nullable
    public HashMap<String, Button> getButtonHashMap() {
        return buttonHashMap;
    }
}
