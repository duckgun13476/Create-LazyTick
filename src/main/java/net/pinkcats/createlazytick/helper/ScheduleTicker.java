package net.pinkcats.createlazytick.helper;

public class ScheduleTicker {

    private final int triggerInterval;
    private final Runnable targetFunction;
    private int counter;
    private final Object lock = new Object();

    public ScheduleTicker(int triggerInterval, Runnable targetFunction) {
        // Valid ticker
        if (triggerInterval <= 0) {
            throw new IllegalArgumentException("触发间隔必须是正整数，当前值：" + triggerInterval);
        }
        if (targetFunction == null) {
            throw new IllegalArgumentException("目标函数不能为null");
        }
        this.triggerInterval = triggerInterval;
        this.targetFunction = targetFunction;
        this.counter = 0;
    }

    public void Tick() {
        synchronized (lock) {
            counter++;
            if (counter % triggerInterval == 0) {
                targetFunction.run();
            }
        }
    }



}