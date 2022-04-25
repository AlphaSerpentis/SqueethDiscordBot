package space.alphaserpentis.handler;

import net.dv8tion.jda.api.entities.Activity;
import space.alphaserpentis.main.Launcher;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class StatusHandler {

    private final String statusMessage = "oSQTH: $"; // Might change in the future to use a list instead for more statuses

    public StatusHandler() throws IOException {
        runStatusRotation();
    }

    public void runStatusRotation() {
        new Thread(() -> {
            while(true) {
                try {
                    updateStatus();
                    Thread.sleep(300000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void updateStatus() throws IOException {
        Instant epoch = Instant.now();
        ZonedDateTime time = ZonedDateTime.ofInstant(epoch, ZoneOffset.UTC);

//        LaevitasHandler.pollForData("historical/power_perp/squeeth/eth?start=" + time.getYear() + "-" + time.getMonth() + "-" + time.getDayOfMonth() +"&end=" + time.getYear() + "-" + time.getMonth() + "-" + time.getDayOfMonth());
        LaevitasHandler.pollForData("analytics/defi/squeeth");

        Launcher.api.getPresence().setActivity(Activity.watching(statusMessage + LaevitasHandler.latestSqueethData.getoSQTHPrice() + " | Current Implied Funding: " + LaevitasHandler.latestSqueethData.getDelta() + " | IV: " + LaevitasHandler.latestSqueethData.getCurrentImpliedVolatility() + "%"));
    }

}
