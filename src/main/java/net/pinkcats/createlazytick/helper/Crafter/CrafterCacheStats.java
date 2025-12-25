package net.pinkcats.createlazytick.helper.Crafter;

import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.CreateLazyTick; // 引入主类以使用 LOGGER

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;

//记录缓存命中数据用,仅在debug模式开启情况下,按照一分钟发送一次的频率发送info
public class CrafterCacheStats {

    private static final AtomicLong hits = new AtomicLong(0);
    private static final AtomicLong misses = new AtomicLong(0);
    private static final AtomicLong nbtSkips = new AtomicLong(0);
    private static final AtomicLong cooldownSkips = new AtomicLong(0);

    private static final DecimalFormat df = new DecimalFormat("0.00");

    private static long lastPrintTime = 0;
    private static final long PRINT_INTERVAL_MS = 60 * 1000L;
    private static final AtomicLong intervalOps = new AtomicLong(0);

    public static void onHit() {
        hits.incrementAndGet();
        checkPrint();
    }

    public static void onMiss() {
        misses.incrementAndGet();
        checkPrint();
    }

    public static void onNbtSkip(String itemName) {
        long count = nbtSkips.incrementAndGet();
        if (Config.enable_cache_crafter && Config.enable_cache_crafter_debugger) {
            CreateLazyTick.LOGGER.info("[LazyTick] [DEBUG] Skipped cache due to NBT item: {} (Total Skips: {})", itemName, count);
        }
        checkPrint();
    }

    public static void onCooldownSkip() {
        cooldownSkips.incrementAndGet();
        checkPrint();
    }

    public static void reset() {
        hits.set(0);
        misses.set(0);
        nbtSkips.set(0);
        cooldownSkips.set(0);
        intervalOps.set(0);
        lastPrintTime = System.currentTimeMillis();

        if (Config.enable_cache_crafter && Config.enable_cache_crafter_debugger) {
            CreateLazyTick.LOGGER.info("[LazyTick] Crafter Stats reset.");
        }
    }

    private static void checkPrint() {
        // 合成器缓存开关关闭 或 Debug开关关闭，均不输出
        if (!Config.enable_cache_crafter || !Config.enable_cache_crafter_debugger) return;

        intervalOps.incrementAndGet();
        long now = System.currentTimeMillis();

        if (now - lastPrintTime > PRINT_INTERVAL_MS) {
            synchronized (CrafterCacheStats.class) {
                if (now - lastPrintTime > PRINT_INTERVAL_MS) {
                    printOverview();
                    lastPrintTime = now;
                    intervalOps.set(0);
                }
            }
        }
    }

    private static void printOverview() {
        long h = hits.get();
        long m = misses.get();
        long n = nbtSkips.get();
        long c = cooldownSkips.get();
        long totalValid = h + m;

        double hitRate = totalValid == 0 ? 0 : (double) h / totalValid * 100.0;

        StringBuilder sb = new StringBuilder();
        sb.append("\n[LazyTick] [DEBUG] Crafter Cache Report (Last 1 Minute)\n");
        sb.append(String.format(" > Total Hits : %d (Efficiency: %s%%)\n", h, df.format(hitRate)));
        sb.append(String.format(" > New Recipes: %d\n", m));
        sb.append(String.format(" > NBT Skips  : %d\n", n));
        sb.append(String.format(" > Cool Skips : %d\n", c));
        sb.append("--------------------------------------------------");

        CreateLazyTick.LOGGER.info(sb.toString());
    }
}