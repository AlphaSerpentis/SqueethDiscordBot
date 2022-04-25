package space.alphaserpentis.squeethdiscordbot.handler;

import net.dv8tion.jda.api.entities.Activity;
import space.alphaserpentis.squeethdiscordbot.main.Launcher;

import java.io.IOException;

public class StatusHandler {

    private final String statusMessage = "oSQTH: $"; // Might change in the future to use a list instead for more statuses

    public StatusHandler() {
        runStatusRotation();
    }

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

    private void updateStatus() throws IOException {
        LaevitasHandler.pollForData("analytics/defi/squeeth");

        Launcher.api.getPresence().setActivity(Activity.watching(statusMessage + LaevitasHandler.latestSqueethData.getoSQTHPrice() + " | Current Implied Funding: " + LaevitasHandler.latestSqueethData.getDelta() + " | IV: " + LaevitasHandler.latestSqueethData.getCurrentImpliedVolatility() + "%"));
    }

}
