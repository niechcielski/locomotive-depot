package parowozownia;

import java.util.Random;

public class Boss implements Runnable {
    private final DepotManager depotManager;
    private String status = "Nadzoruje pracę parowozowni";
    private final Random random = new Random();

    public Boss(DepotManager depotManager) {
        this.depotManager = depotManager;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                status = "Nadzoruje pracę parowozowni";
                Thread.sleep(15000 + random.nextInt(15000));

                status = "ZARZĄDZIŁ REMONT! Czeka na opuszczenie hali";
                depotManager.startRenovation();

                status = "TRWA GENERALNY REMONT!";
                Thread.sleep(6000);

                depotManager.endRenovation();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public synchronized String getStatus() {
        return status;
    }
}