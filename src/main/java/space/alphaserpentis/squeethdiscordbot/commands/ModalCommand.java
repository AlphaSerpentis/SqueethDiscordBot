package space.alphaserpentis.squeethdiscordbot.commands;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import javax.annotation.Nonnull;

public interface ModalCommand {
    void runModalInteraction(@Nonnull ModalInteractionEvent event);
}
