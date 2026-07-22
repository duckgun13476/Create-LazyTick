package net.pinkcats.createlazytick.helper.tooltip;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.pinkcats.createlazytick.config.ClientConfig;
import net.pinkcats.createlazytick.item.LazyTickClockItem;

public class LazyTickTooltipTool {
    public static boolean shouldRender(Minecraft mc) {
        if (mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof LazyTickClockItem)) {
            return false;
        }

        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        double localY = hit.getLocation().y - hit.getBlockPos().getY();
        if (localY < 0.5D) {
            return false;
        }

        return ClientConfig.showModeTooltip() || ClientConfig.showTierTooltip();
    }

    public static String formatTime(int ticks) {
        if (ticks < 0) ticks = 0;

        return switch (ClientConfig.getTimeFormat()) {
            case SECONDS -> String.format("%.1fs", ticks / 20.0f); // 除以 20.0f 得到秒数，保留1位小数
            case BOTH -> String.format("%dt | %.1fs", ticks, ticks / 20.0f); // 60t | 3.0s
            default -> ticks + "t";  // 默认仅显示 tick
        };
    }
}
