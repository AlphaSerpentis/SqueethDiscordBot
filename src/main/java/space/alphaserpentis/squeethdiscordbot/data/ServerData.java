package space.alphaserpentis.squeethdiscordbot.data;

public class ServerData {

    private boolean onlyEphemeral = true;
    private boolean doRandomSquizQuestions = false;
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

    public boolean isOnlyEphemeral() {
        return onlyEphemeral;
    }
    public long getLeaderboardChannelId() {
        return leaderboardChannelId;
    }
    public boolean doRandomSquizQuestions() {
        return doRandomSquizQuestions;
    }
}
