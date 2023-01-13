// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.Collection;
import java.util.HashMap;

public abstract class ButtonCommand<T> extends BotCommand<T> {
    protected final HashMap<String, Button> buttonHashMap = new HashMap<>();

    public ButtonCommand() {
        super();
    }

    public ButtonCommand(@NonNull BotCommandOptions options) {
        super(options);
    }

    abstract public void runButtonInteraction(@NonNull ButtonInteractionEvent event);
    @NonNull
    abstract public Collection<ItemComponent> addButtons(@NonNull GenericCommandInteractionEvent event);
    @Nullable
    public Button getButton(String key) {
        return buttonHashMap.get(key);
    }
    @NonNull
    public HashMap<String, Button> getButtonHashMap() {
        return buttonHashMap;
    }
}
