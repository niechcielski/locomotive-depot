package parowozownia;

import java.util.Random;

public class Locomotive implements Runnable {
    private final int id;
    private final DepotManager depotManager;
    private final Random random = new Random();
    private String stateDescription = "Praca na szlaku";

    public Locomotive(int id, DepotManager depotManager) {
        this.id = id;
        this.depotManager = depotManager;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {

                stateDescription = "Praca na szlaku";
                Thread.sleep(4000 + random.nextInt(6000));

                stateDescription = "Czeka przed nastawnią";
                int trackId = depotManager.acquireTrack(this);

                stateDescription = "Wjeżdża Torem " + (trackId + 1);
                Thread.sleep(1500);

                stateDescription = "Czeka na otwarcie bram (Remont/Brak miejsc)";
                int assignedSpot = depotManager.enterDepotFromTrack(this, trackId);

                stateDescription = "W trakcie naprawy";
                Thread.sleep(3000 + random.nextInt(4000));

                stateDescription = "Opuszcza parowozownię";
                depotManager.exitDepot(assignedSpot);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int getLocoId() { return id; }
    public synchronized String getStateDescription() { return stateDescription; }
}