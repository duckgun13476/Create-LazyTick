package net.pinkcats.createlazytick.helper.tooltip;

import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipWhiteList;
import net.pinkcats.createlazytick.config.ClientConfig;
import net.pinkcats.createlazytick.item.LazyTickClockItem;

public class LazyTickTooltipTool {
    public static boolean isHoldingClock(Minecraft mc) {
        return mc != null
                && mc.player != null
                && mc.player.getMainHandItem().getItem() instanceof LazyTickClockItem;
    }

    public static boolean shouldSuppressCreateOverlay(Minecraft mc) {
        if (mc == null || mc.level == null || !isHoldingClock(mc)) {
            return false;
        }

        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        BlockPos originalPos = hit.getBlockPos();
        BlockPos targetPos = GoggleOverlayRenderer.proxiedOverlayPosition(mc.level, originalPos);
        BlockEntity blockEntity = mc.level.getBlockEntity(targetPos);
        return LazyTickTooltipWhiteList.getByEntity(blockEntity) != null;
    }

    public static boolean shouldRender(Minecraft mc) {
        if (mc == null) {
            return false;
        }

        if (mc.player == null) {
            LazyTickDepotDebug.log(mc, "tooltip_gate", "blocked: player=null");
            return false;
        }

        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            LazyTickDepotDebug.log(mc, "tooltip_gate", "blocked: hitResult is not a block");
            return false;
        }

        BlockPos pos = hit.getBlockPos();
        BlockEntity blockEntity = mc.level == null ? null : mc.level.getBlockEntity(pos);
        boolean depotTarget = LazyTickTooltipWhiteList.getByEntity(blockEntity) == LazyTickTooltipWhiteList.DEPOT;
        if (!isHoldingClock(mc)) {
            if (depotTarget) {
                LazyTickDepotDebug.logBlockEntity(mc, "tooltip_gate", pos, blockEntity,
                        "blocked: main hand is not LazyTick clock, mainHand=" + mc.player.getMainHandItem());
            }
            return false;
        }

        double localY = hit.getLocation().y - hit.getBlockPos().getY();
        if (localY < 0.5D) {
            if (depotTarget) {
                LazyTickDepotDebug.logBlockEntity(mc, "tooltip_gate", pos, blockEntity,
                        "blocked: localY<0.5 (" + localY + ")");
            }
            return false;
        }

        boolean result = ClientConfig.showModeTooltip() || ClientConfig.showTierTooltip();
        if (depotTarget) {
            LazyTickDepotDebug.logBlockEntity(mc, "tooltip_gate", pos, blockEntity,
                    "pass=" + result + ", localY=" + localY
                            + ", showMode=" + ClientConfig.showModeTooltip()
                            + ", showTier=" + ClientConfig.showTierTooltip());
        }
        return result;
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
