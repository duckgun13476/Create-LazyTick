package net.pinkcats.createlazytick.helper;

/** Integer tick calculations with widened intermediates before applying their bounds. */
public final class TickIntervalMath {
    private TickIntervalMath() {
    }

    public static int growByTenPercent(int current, int maximum) {
        return (int) Math.min((long) maximum, (long) current + Math.max(1, current / 10));
    }

    public static int percentage(int maximum, int percentage) {
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, (long) maximum * percentage / 100));
    }
}
