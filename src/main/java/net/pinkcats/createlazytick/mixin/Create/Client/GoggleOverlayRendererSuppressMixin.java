package net.pinkcats.createlazytick.mixin.Create.Client;

import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipTool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GoggleOverlayRenderer.class, remap = false)
public class GoggleOverlayRendererSuppressMixin {

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private static void createLazyTick$suppressCreateOverlayWhenClockHeld(GuiGraphics guiGraphics,
                                                                          DeltaTracker deltaTracker,
                                                                          CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (LazyTickTooltipTool.shouldSuppressCreateOverlay(mc)) {
            ci.cancel();
        }
    }
}
