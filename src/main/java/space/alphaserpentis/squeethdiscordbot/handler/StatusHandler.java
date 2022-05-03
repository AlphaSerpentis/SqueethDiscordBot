package space.alphaserpentis.squeethdiscordbot.handler;

import net.dv8tion.jda.api.entities.Activity;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import java.io.IOException;
import java.text.NumberFormat;

public class StatusHandler {

    private int statusIndex;

    public StatusHandler() {
        runStatusRotation();
    }

    /**
     * Runs a thread that rotates the status every 15 seconds.
     */
    public void runStatusRotation() {
        LaevitasHandler.timedPoller();
        new Thread(() -> {
            while(true) {
                try {
                    updateStatus();
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    /**
     * Rotates the status
     */
    private void updateStatus() {
        String statusMessage = " | /help";

        if(statusIndex > 2)
            statusIndex = 0;

        switch(statusIndex++) {
            case 0 -> statusMessage = "oSQTH: $" + NumberFormat.getInstance().format(LaevitasHandler.latestSqueethData.getoSQTHPrice()) + statusMessage;
            case 1 -> statusMessage = "Implied Funding: " + LaevitasHandler.latestSqueethData.getCurrentImpliedFundingValue() + "%" + statusMessage;
            case 2 -> statusMessage = "IV: " + LaevitasHandler.latestSqueethData.getCurrentImpliedVolatility() + "%" + statusMessage;
        }

        Launcher.api.getPresence().setActivity(Activity.watching(statusMessage));
    }

}
