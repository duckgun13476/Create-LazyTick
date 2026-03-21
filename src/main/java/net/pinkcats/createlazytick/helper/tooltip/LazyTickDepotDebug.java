package net.pinkcats.createlazytick.helper.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.config.ClientConfig;

import java.util.HashMap;
import java.util.Map;

public final class LazyTickDepotDebug {
    private static final Map<String, Long> LAST_LOG_TICKS = new HashMap<>();

    private LazyTickDepotDebug() {
    }

    public static boolean enabled() {
        return ClientConfig.enableDepotDebug();
    }

    public static void log(Minecraft mc, String stage, String detail) {
        if (!enabled()) {
            return;
        }

        long gameTime = resolveGameTime(mc);
        String throttleKey = stage + "|" + detail;
        Long lastTick = LAST_LOG_TICKS.get(throttleKey);
        if (lastTick != null && gameTime - lastTick < 20) {
            return;
        }
        LAST_LOG_TICKS.put(throttleKey, gameTime);
        CreateLazyTick.LOGGER.info("[CreateLazyTick][DepotDebug][{}] {}", stage, detail);
    }

    public static void logBlockEntity(Minecraft mc, String stage, BlockPos pos, BlockEntity blockEntity, String detail) {
        String beName = blockEntity == null ? "null" : blockEntity.getClass().getName();
        log(mc, stage, "pos=" + pos + ", be=" + beName + ", " + detail);
    }

    private static long resolveGameTime(Minecraft mc) {
        if (mc != null && mc.level != null) {
            return mc.level.getGameTime();
        }
        return System.currentTimeMillis() / 50L;
    }
}
