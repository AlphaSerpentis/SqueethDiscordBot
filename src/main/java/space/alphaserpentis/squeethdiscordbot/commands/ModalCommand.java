package space.alphaserpentis.squeethdiscordbot.commands;

import io.reactivex.annotations.NonNull;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public interface ModalCommand {
    void runModalInteraction(@NonNull ModalInteractionEvent event);
}
