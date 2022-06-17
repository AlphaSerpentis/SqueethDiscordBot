// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data;

import java.util.HashMap;

public class SquizLeaderboard {
    /**
     * Key: userId
     * Value: score
     */
    public HashMap<Long, Integer> leaderboard = new HashMap<>();

    public HashMap<Long, Integer> getTopFive() {
        HashMap<Long, Integer> topFive = new HashMap<>();
        int i = 0;
        for(Long userId : leaderboard.keySet()) {
            if(i >= 5) {
                break;
            }
            topFive.put(userId, leaderboard.get(userId));
            i++;
        }
        return topFive;
    }
}
