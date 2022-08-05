// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data.server.squiz;

import javax.annotation.Nonnull;
import java.util.*;

public class SquizLeaderboard {
    /**
     * Stores the userId/score pair
     */
    public Map<Long, Integer> leaderboard = new HashMap<>();

    public void addPoint(long userId) {
        leaderboard.put(userId, leaderboard.getOrDefault(userId, 0) + 1);
    }
    public void removePoint(long userId) {
        leaderboard.put(userId, leaderboard.getOrDefault(userId, 0) - 1);
    }
    public void setCustomPoint(long userId, int points) {
        leaderboard.put(userId, points);
    }

    @Nonnull
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

    public int getPositionOfUser(long userId) {
        int pos = -1;

        ArrayList<Long> listOfUsers = new ArrayList<>(leaderboard.keySet());

        listOfUsers.sort(
                (o1, o2) -> leaderboard.get(o2).compareTo(leaderboard.get(o1))
        );

        for(int i = 0; i < listOfUsers.size(); i++) {
            if(listOfUsers.get(i) == userId)
                return i + 1;
        }

        return pos;
    }
}
