package net.pinkcats.createlazytick.client;

import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipTool;

@EventBusSubscriber(modid = CreateLazyTick.MODID, value = Dist.CLIENT)
public class LazyTickCreateOverlaySuppressor {
    private static final int OFFSCREEN_OFFSET = 100000;

    private static boolean offsetsOverridden = false;
    private static int savedOffsetX = 0;
    private static int savedOffsetY = 0;

    private LazyTickCreateOverlaySuppressor() {
    }

    @SubscribeEvent
    public static void onPreHotbar(RenderGuiLayerEvent.Pre event) {
        if (!"hotbar".equals(event.getName().getPath())) {
            return;
        }
        restoreOffsetsIfNeeded();
    }

    @SubscribeEvent
    public static void onPostHotbar(RenderGuiLayerEvent.Post event) {
        if (!"hotbar".equals(event.getName().getPath())) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!LazyTickTooltipTool.shouldSuppressCreateOverlay(mc)) {
            return;
        }

        if (!offsetsOverridden) {
            savedOffsetX = AllConfigs.client().overlayOffsetX.get();
            savedOffsetY = AllConfigs.client().overlayOffsetY.get();
            offsetsOverridden = true;
        }

        AllConfigs.client().overlayOffsetX.set(OFFSCREEN_OFFSET);
        AllConfigs.client().overlayOffsetY.set(OFFSCREEN_OFFSET);
    }

    public static int getOverlayOffsetX() {
        return offsetsOverridden ? savedOffsetX : AllConfigs.client().overlayOffsetX.get();
    }

    public static int getOverlayOffsetY() {
        return offsetsOverridden ? savedOffsetY : AllConfigs.client().overlayOffsetY.get();
    }

    private static void restoreOffsetsIfNeeded() {
        if (!offsetsOverridden) {
            return;
        }

        AllConfigs.client().overlayOffsetX.set(savedOffsetX);
        AllConfigs.client().overlayOffsetY.set(savedOffsetY);
        offsetsOverridden = false;
    }
}
