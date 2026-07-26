package net.pinkcats.createlazytick.adaptive.saw;

/**
 * Pure, loader-neutral frequency function for a Create saw's periodic input/output paths.
 *
 * <p>Callers keep independent state for input arrivals and output transfers. Each state learns
 * successful event spacing, probes shortly before the next predicted window, and falls back to a
 * bounded backoff when no prediction is available. Callers own event classification and applying
 * the returned interval.</p>
 */
public final class SawFrequencyFunction {

    private static final long NO_SUCCESS = Long.MIN_VALUE;

    private SawFrequencyFunction() {
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

    /**
     * Records an item becoming available to an otherwise idle saw. Input arrival and output
     * release must use separate {@link State} instances: their cadences are independent.
     */
    public static State onInputArrival(State state, long gameTick, int maxInterval, int currentInterval) {
        State observed = onOutputSuccess(state, gameTick, maxInterval);
        return new State(reduceAfterInput(currentInterval), observed.lastSuccessTick(), observed.learnedPeriod());
    }

    /**
     * Advances the bounded fallback after an idle probe found no input.
     */
    public static State onIdleProbe(State state, int maxInterval) {
        return onRetryFailure(state, maxInterval);
    }

    /**
     * One observed input makes the idle schedule more responsive without discarding the learned
     * low-frequency operating point. This is deliberately the inverse of the bounded 10% backoff.
     */
    public static int reduceAfterInput(int currentInterval) {
        int current = Math.max(1, currentInterval);
        return Math.max(1, current - Math.max(1, current / 10));
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

    /**
     * Input-idle prediction may cap a gradual probe curve, but must never jump directly to the
     * whole predicted wait. A large learned period is a target horizon, not one giant sleep.
     */
    public static int nextInputProbeInterval(State state, long gameTick, int maxInterval, int earlyGuardTicks) {
        int fallback = clamp(state.fallbackInterval(), 1, maxInterval);
        if (!state.hasLearnedPeriod())
            return fallback;

        long predictedTick = state.lastSuccessTick() + state.learnedPeriod() - Math.max(0, earlyGuardTicks);
        if (predictedTick <= gameTick)
            return fallback;
        return Math.min(fallback, clampLong(predictedTick - gameTick, 1, maxInterval));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clampLong(long value, int min, int max) {
        return (int) Math.max(min, Math.min((long) max, value));
    }
}
