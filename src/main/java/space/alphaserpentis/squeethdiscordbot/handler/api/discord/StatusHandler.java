// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.handler.api.discord;

import net.dv8tion.jda.api.entities.Activity;
import space.alphaserpentis.squeethdiscordbot.handler.api.ethereum.LaevitasHandler;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import java.text.NumberFormat;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class StatusHandler {

    private int statusIndex;
    private final ScheduledExecutorService scheduledExecutor;

    public StatusHandler() {
        scheduledExecutor = new ScheduledThreadPoolExecutor(1);
        runStatusRotation();
    }

    /**
     * Runs a thread that rotates the status every 15 seconds.
     */
    public void runStatusRotation() {
        LaevitasHandler.timedPoller();
        scheduledExecutor.scheduleAtFixedRate(this::updateStatus, 0, 15, TimeUnit.SECONDS);
    }

    /**
     * Rotates the status
     */
    private void updateStatus() {
        String statusMessage = " | /help";

        if(statusIndex > 2)
            statusIndex = 0;

        switch(statusIndex++) {
            case 0 -> statusMessage = "oSQTH: $" + NumberFormat.getInstance().format(LaevitasHandler.latestSqueethData.data.getoSQTHPrice()) + statusMessage;
            case 1 -> statusMessage = "Impl. Funding: " + LaevitasHandler.latestSqueethData.data.getCurrentImpliedFundingValue() + "%" + statusMessage;
            case 2 -> statusMessage = "IV: " + LaevitasHandler.latestSqueethData.data.getCurrentImpliedVolatility() + "%" + statusMessage;
        }

        Launcher.api.getPresence().setActivity(Activity.watching(statusMessage));
    }

}
