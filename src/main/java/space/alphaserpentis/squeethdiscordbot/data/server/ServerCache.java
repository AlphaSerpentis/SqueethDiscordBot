// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data.server;

import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("CanBeFinal")
public class ServerCache {
    public static HashMap<Long, ArrayList<Long>> guildToChannelsMap = new HashMap<>();
    public static HashMap<Long, ArrayList<Long>> cachedMessages = new HashMap<>();

    public static void addNewMessage(@Nonnull Long guildId, @Nonnull Long channelId, @Nonnull Long messageId) {
        ArrayList<Long> messages = cachedMessages.get(channelId);
        ArrayList<Long> trackedChannels = guildToChannelsMap.getOrDefault(guildId, new ArrayList<>());

        if(messages == null) {
            messages = new ArrayList<>();

            messages.add(messageId);

            cachedMessages.put(channelId, messages);
        } else if(messages.size() == 10){
            messages.remove(0);
            messages.add(messageId);
        } else {
            messages.add(messageId);
        }

        if(!trackedChannels.contains(channelId))
            trackedChannels.add(channelId);

        guildToChannelsMap.put(guildId, trackedChannels);
    }

    public static void removeMessages(@Nonnull Long guildId) {
        if(cachedMessages.isEmpty()) {
            return;
        }

        for(Long channelId: guildToChannelsMap.get(guildId)) {
            for(Long msgId: cachedMessages.get(channelId)) {
                try {
                    Launcher.api.getGuildChannelById(channelId).getGuild().getTextChannelById(channelId).retrieveMessageById(msgId).queue(
                            (success) -> success.delete().queue(),
                            (ignored) -> {}
                    );
                } catch(NullPointerException ignored) {

                }
            }

            cachedMessages.get(channelId).clear();
        }
    }

}
