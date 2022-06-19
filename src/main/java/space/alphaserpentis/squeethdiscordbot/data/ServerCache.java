// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data;

import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.HashMap;

public class ServerCache {
    public static HashMap<Long, ArrayList<Message>> cachedMessages = new HashMap<>();

    public static void addNewMessage(Long guildId, Message message) {
        ArrayList<Message> messages = cachedMessages.get(guildId);

        if(messages == null) {
            messages = new ArrayList<>();

            messages.add(message);

            cachedMessages.put(guildId, messages);
        } else if(messages.size() == 10){
            messages.remove(0);
            messages.add(message);
        } else {
            messages.add(message);

        }
    }

    public static void removeMessages(Long guildId) {
        for(Message msg: cachedMessages.get(guildId)) {
            msg.delete().queue();
        }

        cachedMessages.get(guildId).clear();
    }

}
