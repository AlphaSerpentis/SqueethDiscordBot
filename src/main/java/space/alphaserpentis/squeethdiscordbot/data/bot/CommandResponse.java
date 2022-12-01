package space.alphaserpentis.squeethdiscordbot.data.bot;

import io.reactivex.annotations.Nullable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public record CommandResponse<T>(T messageResponse, Boolean messageIsEphemeral) {
    public CommandResponse(
            @Nullable T messageResponse,
            @Nullable Boolean messageIsEphemeral
            ) {
        if(messageResponse != null) {
            if (!(messageResponse instanceof MessageEmbed || messageResponse instanceof Message)) {
                throw new IllegalArgumentException("messageResponse must be of type MessageEmbed or Message");
            }
        } else if(messageIsEphemeral == null) {
            throw new IllegalArgumentException("messageResponse and messageIsEphemeral cannot both be null!");
        }

        this.messageResponse = messageResponse;
        this.messageIsEphemeral = messageIsEphemeral;
    }
}