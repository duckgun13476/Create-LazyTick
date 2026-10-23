package net.pinkcats.createlazytick;

import net.pinkcats.createlazytick.adaptive.saw.SawFrequencyFunction;
import net.pinkcats.createlazytick.helper.TickIntervalMath;

/** Dependency-free arithmetic regression; no Minecraft runtime or test framework required. */
public final class TickIntervalMathRegression {
    private static int checks;

    public static void main(String[] args) {
        for (int maximum : new int[]{20, 60, 100, 600, 10000}) {
            for (int current = 1; current <= maximum; current++) {
                equal(Math.min(maximum, current + Math.max(1, current / 10)),
                        TickIntervalMath.growByTenPercent(current, maximum));
            }
            for (int percent = 0; percent <= 100; percent++) {
                equal(Math.max(1, maximum * percent / 100), TickIntervalMath.percentage(maximum, percent));
            }
            for (int period = 1; period <= maximum; period++) {
                int observed = maximum - period + 1;
                equal(Math.max(1, (period * 3 + observed) / 4),
                        SawFrequencyFunction.onOutputSuccess(
                                new SawFrequencyFunction.State(1, 0, period), observed, maximum).learnedPeriod());
            }
        }

        int max = Integer.MAX_VALUE;
        for (int current : new int[]{1, 60, 1000000000, 1952257862, 2000000000, max}) {
            equal((int) Math.min((long) max, current + Math.max(1L, current / 10)),
                    TickIntervalMath.growByTenPercent(current, max));
            equal(TickIntervalMath.growByTenPercent(current, max),
                    SawFrequencyFunction.onIdleProbe(new SawFrequencyFunction.State(current, 0, 0), max)
                            .fallbackInterval());
            for (int percent = 0; percent <= 100; percent++) {
                equal((int) Math.max(1L, (long) current * percent / 100),
                        TickIntervalMath.percentage(current, percent));
            }
        }
        for (int period : new int[]{1000000000, 2000000000, max}) {
            equal(period, SawFrequencyFunction.onOutputSuccess(
                    new SawFrequencyFunction.State(1, 0, period), period, max).learnedPeriod());
        }
        equal(1286870911, SawFrequencyFunction.onOutputSuccess(
                new SawFrequencyFunction.State(1, 0, 1000000000), max, max).learnedPeriod());
        equal(1, TickIntervalMath.percentage(max, 0));
        equal(max, TickIntervalMath.percentage(max, 100));
        equal(max, TickIntervalMath.percentage(max, 200));
        System.out.println("Tick interval arithmetic: " + checks + " checks passed");
    }

    private static void equal(int expected, int actual) {
        checks++;
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
