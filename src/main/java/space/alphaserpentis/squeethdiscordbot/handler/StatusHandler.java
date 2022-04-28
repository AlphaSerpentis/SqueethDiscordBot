package space.alphaserpentis.squeethdiscordbot.handler;

import net.dv8tion.jda.api.entities.Activity;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import java.io.IOException;

public class StatusHandler {

    /**
     * @implNote May change in the future to use a list instead for more statuses
     */
    private final String statusMessage = "oSQTH: $";

    public StatusHandler() {
        runStatusRotation();
    }

    /**
     * Runs a thread that rotates the status every 5 minutes.
     */
    public void runStatusRotation() {
        new Thread(() -> {
            while(true) {
                try {
                    updateStatus();
                    Thread.sleep(300000);
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    /**
     * Calls {@link space.alphaserpentis.squeethdiscordbot.handler.LaevitasHandler#pollForData(String)} and updates {@link space.alphaserpentis.squeethdiscordbot.handler.LaevitasHandler#latestSqueethData} as a side effect
     * @throws IOException
     */
    private void updateStatus() throws IOException {
        LaevitasHandler.pollForData("analytics/defi/squeeth");

        Launcher.api.getPresence().setActivity(Activity.watching(statusMessage + LaevitasHandler.latestSqueethData.getoSQTHPrice() + " | Current Implied Funding: " + LaevitasHandler.latestSqueethData.getCurrentImpliedFundingValue() + "% | IV: " + LaevitasHandler.latestSqueethData.getCurrentImpliedVolatility() + "%"));
    }

}
