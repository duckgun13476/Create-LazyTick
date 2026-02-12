package net.pinkcats.createlazytick.helper.util;

import java.util.Random;

public class ScheduleTicker {

    private final int triggerInterval;
    private final Runnable targetFunction;
    private int counter;
    private final Object lock = new Object();
    private final int randomOffset;
    private static final Random RANDOM = new Random();

    public ScheduleTicker(int triggerInterval, Runnable targetFunction) {
        // 原有参数校验
        if (triggerInterval <= 0) {
            throw new IllegalArgumentException("Trigger interval must be a positive integer, current value: " + triggerInterval);
        }
        if (targetFunction == null) {
            throw new IllegalArgumentException("Target function cannot be null");
        }
        this.triggerInterval = triggerInterval;
        this.targetFunction = targetFunction;
        this.counter = 0;
        this.randomOffset = RANDOM.nextInt(triggerInterval);
    }

    public void RandomTick() {
        synchronized (lock) {
            counter++;
            if ((counter + randomOffset) % triggerInterval == 0) {
                targetFunction.run();
            }
        }
    }

}