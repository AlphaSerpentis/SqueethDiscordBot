// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data.server.squiz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SquizLeaderboard {
    /**
     * Key: userId
     * Value: score
     */
    public Map<Long, Integer> leaderboard = new HashMap<>();

    public void addPoint(long userId) {
        leaderboard.put(userId, leaderboard.getOrDefault(userId, 0) + 1);
    }

    public ArrayList<Long> getTopFive() {
        ArrayList<Long> topFive = new ArrayList<>();

        for(Long userId: leaderboard.keySet()) {
            if(topFive.size() == 5) {
                long userWithLowestScore = userId;

                for(Long idInTempArray: topFive) {
                    if(leaderboard.get(idInTempArray) < leaderboard.get(userWithLowestScore)) {
                        userWithLowestScore = idInTempArray;
                    }
                }

                if(userWithLowestScore != userId) {
                    topFive.remove(userWithLowestScore);
                    topFive.add(userId);
                }
            } else {
                topFive.add(userId);
            }
        }

        topFive.sort(
                (o1, o2) -> leaderboard.get(o2).compareTo(leaderboard.get(o1))
        );

        return topFive;
    }
}
