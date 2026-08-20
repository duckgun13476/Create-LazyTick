package net.pinkcats.createlazytick.diag;

import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.config.ServerConfig;

/**
 * Bounded, opt-in diagnostics for short-lived compatibility and performance investigations.
 */
public final class DiagnosticLog {

    public enum Event {
        SAW_CACHE_HIT,
        SAW_CACHE_MISS,
        SAW_CACHE_BYPASS,
        SAW_INPUT_WAKE,
        SAW_IDLE_PROBE,
        SAW_OUTPUT_ATTEMPT,
        SAW_OUTPUT_BACKOFF,
        SAW_OUTPUT_SUCCESS
    }

    private static final int TOTAL_LIMIT = 100;
    private static final int PER_SECOND_LIMIT = 20;
    private static final State[] STATES = new State[Event.values().length];

    static {
        for (int index = 0; index < STATES.length; index++) {
            STATES[index] = new State();
        }
    }

    private DiagnosticLog() {
    }

    public static void saw(Event event, String detail) {
        if (!ServerConfig.getEnableDebugLog()) {
            return;
        }

        State state = STATES[event.ordinal()];
        synchronized (state) {
            if (state.printed >= TOTAL_LIMIT) {
                state.dropped++;
                return;
            }

            long second = System.currentTimeMillis() / 1_000L;
            if (state.second != second) {
                state.second = second;
                state.printedThisSecond = 0;
            }
            if (state.printedThisSecond >= PER_SECOND_LIMIT) {
                state.dropped++;
                return;
            }

            state.printed++;
            state.printedThisSecond++;
            CreateLazyTick.LOGGER.info("[CLT.diag] v=1 domain=saw event={} {}", event.name(), detail);
        }
    }

    private static final class State {
        private int printed;
        private int printedThisSecond;
        private int dropped;
        private long second = Long.MIN_VALUE;
    }
}
