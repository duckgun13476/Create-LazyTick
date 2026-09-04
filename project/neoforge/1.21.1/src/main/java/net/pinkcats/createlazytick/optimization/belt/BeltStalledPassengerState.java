package net.pinkcats.createlazytick.optimization.belt;

public final class BeltStalledPassengerState {

    private int stationaryAttempts;
    private long nextAttemptTick;

    public int stationaryAttempts() {
        return stationaryAttempts;
    }

    public long nextAttemptTick() {
        return nextAttemptTick;
    }

    public void recordCollision(long nextAttemptTick) {
        stationaryAttempts++;
        if (stationaryAttempts >= 2) {
            this.nextAttemptTick = nextAttemptTick;
        }
    }

    public void recordMovement(long currentTick) {
        stationaryAttempts = 0;
        nextAttemptTick = currentTick;
    }
}
