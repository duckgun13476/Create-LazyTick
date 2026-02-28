package net.pinkcats.createlazytick.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.Gui.Menu.ModifyMenu.LazyTickScrollerScreen;
import net.pinkcats.createlazytick.Register.LazyTickItem;
import net.pinkcats.createlazytick.helper.LazyTickScrollerOpenHelper;
import net.minecraft.world.level.block.Block;

@EventBusSubscriber(modid = CreateLazyTick.MODID, value = Dist.CLIENT)
public class LazyTickClockHintOverlay {
    private static final float FADE_DURATION_SECONDS = 0.6F;
    // Font renderer may treat extremely low alpha as opaque; skip those tail frames.
    private static final int MIN_VISIBLE_ALPHA_BYTE = 4;
    private static float hintAlpha = 0.0F;
    private static long lastFrameTimeMs = -1L;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiLayerEvent.Post event) {
        if (!"hotbar".equals(event.getName().getPath())) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        long nowMs = System.currentTimeMillis();
        if (lastFrameTimeMs < 0L) {
            lastFrameTimeMs = nowMs;
        }
        float deltaSeconds = (nowMs - lastFrameTimeMs) / 1000.0F;
        lastFrameTimeMs = nowMs;
        if (deltaSeconds < 0.0F) {
            deltaSeconds = 0.0F;
        }
        if (deltaSeconds > 0.25F) {
            deltaSeconds = 0.25F;
        }

        boolean shouldShow = false;
        String targetBlockClassName = "none";
        if (!(mc.screen instanceof LazyTickScrollerScreen)
                && mc.player != null && mc.level != null && !mc.options.hideGui
                && mc.player.getMainHandItem().getItem() == LazyTickItem.CLOCK.get()
                && mc.hitResult instanceof BlockHitResult bhr
                && bhr.getType() == HitResult.Type.BLOCK) {
            Block block = mc.level.getBlockState(bhr.getBlockPos()).getBlock();
            targetBlockClassName = block.getClass().getName();
            if (LazyTickScrollerOpenHelper.isLazyTickScrollerTarget(block)) {
            double localY = bhr.getLocation().y - bhr.getBlockPos().getY();
            shouldShow = localY < 0.5D;
            }
        }

        if (shouldShow) {
            hintAlpha = Math.min(1.0F, hintAlpha + deltaSeconds / FADE_DURATION_SECONDS);
        } else {
            hintAlpha = Math.max(0.0F, hintAlpha - deltaSeconds / FADE_DURATION_SECONDS);
        }

        int alphaByte = (int) (hintAlpha * 255.0F);
        if (alphaByte < MIN_VISIBLE_ALPHA_BYTE) {
            return;
        }

        Font font = mc.font;
        Component line1 = Component.translatable("createlazytick.hud.config");
        Component line2 = Component.translatable("createlazytick.hud.click_hold_edit");
       // Component line3 = Component.literal("Target: " + targetBlockClassName);

        int centerX = event.getGuiGraphics().guiWidth() / 2;
        int baseY = event.getGuiGraphics().guiHeight() / 2 + 14;

        int x1 = centerX - font.width(line1) / 2;
        int x2 = centerX - font.width(line2) / 2;
       // int x3 = centerX - font.width(line3) / 2;

        int alpha = (alphaByte & 0xFF) << 24;
        int line1Color = alpha | 0xE7CD73;
        int line2Color = alpha | 0xFFFFFF;
       // int line3Color = alpha | 0x9FD3FF;

        event.getGuiGraphics().drawString(font, line1, x1, baseY, line1Color, false);
        event.getGuiGraphics().drawString(font, line2, x2, baseY + 11, line2Color, false);
       // event.getGuiGraphics().drawString(font, line3, x3, baseY + 22, line3Color, false);
    }
}
