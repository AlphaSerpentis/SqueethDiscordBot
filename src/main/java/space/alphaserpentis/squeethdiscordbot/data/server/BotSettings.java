// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data.server;

public class BotSettings {
    public BotSettings() {

    }

    public BotSettings(
        String discordBotKey,
        String laevitasKey,
        long botAdmin,
        String serverData,
        String transfersData,
        String pricesData,
        String squizQuestions,
        String squizLeaderboard,
        String ethereumRPC,
        boolean updateCommandsAtLaunch
    ) {
        this.discordBotKey = discordBotKey;
        this.laevitasKey = laevitasKey;
        this.botAdmin = botAdmin;
        this.serverData = serverData;
        this.transfersData = transfersData;
        this.pricesData = pricesData;
        this.squizQuestions = squizQuestions;
        this.squizLeaderboard = squizLeaderboard;
        this.ethereumRPC = ethereumRPC;
        this.updateCommandsAtLaunch = updateCommandsAtLaunch;
    }

    public String discordBotKey;
    public String laevitasKey;
    public long botAdmin;
    public String serverData;
    public String transfersData;
    public String pricesData;
    public String squizQuestions;
    public String squizLeaderboard;
    public String ethereumRPC;
    public boolean updateCommandsAtLaunch;
}