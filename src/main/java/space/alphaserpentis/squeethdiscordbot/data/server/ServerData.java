// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data.server;

import java.util.ArrayList;

public class ServerData {

    private boolean onlyEphemeral = true;
    private boolean doRandomSquizQuestions = false;
    private ArrayList<Long> randomSquizQuestionsChannels = new ArrayList<>();
    private long randomSquizBaseIntervals = 1800; // default is 30 minutes + random amount
    private long leaderboardChannelId = 0;
    private long lastLeaderboardMessage = 0;
    private long crabAuctionChannelId = 0;
    private boolean listenToCrabAuctions = false;

    public void setOnlyEphemeral(boolean value) {
        onlyEphemeral = value;
    }
    public void setLeaderboardChannelId(long value) {
        leaderboardChannelId = value;
    }
    public void setDoRandomSquizQuestions(boolean value) {
        doRandomSquizQuestions = value;
    }
    public void setRandomSquizQuestionsChannels(ArrayList<Long> value) {
        randomSquizQuestionsChannels = value;
    }
    public void setRandomSquizBaseIntervals(long seconds) {
        randomSquizBaseIntervals = seconds;
    }
    public void setLastLeaderboardMessage(long messageId) {
        lastLeaderboardMessage = messageId;
    }
    public void setCrabAuctionChannelId(long crabAuctionChannelId) {
        this.crabAuctionChannelId = crabAuctionChannelId;
    }
    public void setListenToCrabAuctions(boolean listenToCrabAuctions) {
        this.listenToCrabAuctions = listenToCrabAuctions;
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
    public ArrayList<Long> getRandomSquizQuestionsChannels() {
        return randomSquizQuestionsChannels;
    }

    public long getRandomSquizBaseIntervals() {
        return randomSquizBaseIntervals;
    }
    public long getLastLeaderboardMessage() {
        return lastLeaderboardMessage;
    }
    public long getCrabAuctionChannelId() {
        return crabAuctionChannelId;
    }
    public boolean getListenToCrabAuctions() {
        return listenToCrabAuctions;
    }
}
