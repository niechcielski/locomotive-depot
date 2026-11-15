package parowozownia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DepotManager {
    private final int totalSpots;
    private final Locomotive[] spots;
    private final Locomotive[] accessTracks = new Locomotive[2];
    private final List<Locomotive> waitingQueue = Collections.synchronizedList(new ArrayList<>());

    private boolean renovationIntent = false;
    private boolean renovationInProgress = false;
    private int locosInside = 0;

    public DepotManager(int totalSpots) {
        this.totalSpots = totalSpots;
        this.spots = new Locomotive[totalSpots];
    }

    public int acquireTrack(Locomotive locomotive) throws InterruptedException {
        waitingQueue.add(locomotive);
        synchronized (this) {
            while (accessTracks[0] != null && accessTracks[1] != null) {
                wait();
            }
            waitingQueue.remove(locomotive);
            if (accessTracks[0] == null) {
                accessTracks[0] = locomotive;
                return 0; // Wjazd na Tor 1
            } else {
                accessTracks[1] = locomotive;
                return 1; // Wjazd na Tor 2
            }
        }
    }

    public int enterDepotFromTrack(Locomotive locomotive, int trackId) throws InterruptedException {
        synchronized (this) {
            while (renovationIntent || getFreeSpotIndex() == -1) {
                wait();
            }

            int spotIndex = getFreeSpotIndex();
            spots[spotIndex] = locomotive;
            locosInside++;

            accessTracks[trackId] = null;
            notifyAll();

            return spotIndex;
        }
    }

    public synchronized void exitDepot(int spotIndex) {
        spots[spotIndex] = null;
        locosInside--;
        notifyAll();
    }

    public synchronized void startRenovation() throws InterruptedException {
        renovationIntent = true;
        while (locosInside > 0) {
            wait();
        }
        renovationInProgress = true;
    }

    public synchronized void endRenovation() {
        renovationInProgress = false;
        renovationIntent = false;
        notifyAll();
    }

    private int getFreeSpotIndex() {
        for (int i = 0; i < totalSpots; i++) {
            if (spots[i] == null) return i;
        }
        return -1;
    }

    public synchronized Locomotive getLocomotiveAtSpot(int index) { return spots[index]; }
    public synchronized Locomotive getLocomotiveOnTrack(int trackId) { return accessTracks[trackId]; }
    public List<Locomotive> getWaitingQueue() { return new ArrayList<>(waitingQueue); }
    public synchronized boolean isRenovationInProgress() { return renovationInProgress; }
    public synchronized boolean isRenovationIntent() { return renovationIntent; }
}