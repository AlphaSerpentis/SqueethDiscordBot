// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data;

public class ServerData {

    private boolean onlyEphemeral = true;
    private boolean doRandomSquizQuestions = false;
    private long[] randomSquizQuestionsChannels = new long[0];
    private long leaderboardChannelId = 0;

    public void setOnlyEphemeral(boolean value) {
        onlyEphemeral = value;
    }
    public void setLeaderboardChannelId(long value) {
        leaderboardChannelId = value;
    }
    public void setDoRandomSquizQuestions(boolean value) {
        doRandomSquizQuestions = value;
    }
    public void setRandomSquizQuestionsChannels(long[] value) {
        randomSquizQuestionsChannels = value;
    }

    public boolean isOnlyEphemeral() {
        return onlyEphemeral;
    }
    public long getLeaderboardChannelId() {
        return leaderboardChannelId;
    }
    public boolean doRandomSquizQuestions() {
        return doRandomSquizQuestions;
    }
    public long[] getRandomSquizQuestionsChannels() {
        return randomSquizQuestionsChannels;
    }
}
