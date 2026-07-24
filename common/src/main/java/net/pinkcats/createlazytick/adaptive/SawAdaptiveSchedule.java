package net.pinkcats.createlazytick.adaptive;

/**
 * Pure, loader-neutral retry scheduling for a Create saw's output path.
 *
 * <p>The predictor learns only from successful output transfers. A failed retry before the next
 * predicted transfer window waits for that window; a miss after that window uses bounded fallback
 * backoff. Callers own event classification and applying the returned interval.</p>
 */
public final class SawAdaptiveSchedule {

    private static final long NO_SUCCESS = Long.MIN_VALUE;

    private SawAdaptiveSchedule() {
    }

    public record State(int fallbackInterval, long lastSuccessTick, int learnedPeriod) {
        public static State initial() {
            return new State(1, NO_SUCCESS, 0);
        }

        public boolean hasLearnedPeriod() {
            return lastSuccessTick != NO_SUCCESS && learnedPeriod > 0;
        }
    }

    public static State onRetryFailure(State state, int maxInterval) {
        int current = clamp(state.fallbackInterval(), 1, maxInterval);
        int next = Math.min(maxInterval, current + Math.max(1, current / 10));
        return new State(next, state.lastSuccessTick(), state.learnedPeriod());
    }

    public static State onOutputSuccess(State state, long gameTick, int maxInterval) {
        int period = state.learnedPeriod();
        if (state.lastSuccessTick() != NO_SUCCESS && gameTick > state.lastSuccessTick()) {
            int observed = clampLong(gameTick - state.lastSuccessTick(), 1, maxInterval);
            period = period <= 0 ? observed : clamp((period * 3 + observed) / 4, 1, maxInterval);
        }
        return new State(1, gameTick, period);
    }

    public static State expireAfterEmptyIdle(State state, long gameTick, int resetAfterTicks) {
        if (state.lastSuccessTick() == NO_SUCCESS || gameTick - state.lastSuccessTick() < resetAfterTicks)
            return state;
        return State.initial();
    }

    public static int nextProbeInterval(State state, long gameTick, int maxInterval, int earlyGuardTicks) {
        int fallback = clamp(state.fallbackInterval(), 1, maxInterval);
        if (!state.hasLearnedPeriod())
            return fallback;

        long predictedTick = state.lastSuccessTick() + state.learnedPeriod() - Math.max(0, earlyGuardTicks);
        if (predictedTick <= gameTick)
            return fallback;
        return clampLong(predictedTick - gameTick, 1, maxInterval);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampLong(long value, int min, int max) {
        return (int) Math.max(min, Math.min((long) max, value));
    }
}
