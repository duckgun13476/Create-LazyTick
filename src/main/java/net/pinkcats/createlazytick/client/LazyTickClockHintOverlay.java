package net.pinkcats.createlazytick.client;

import com.simibubi.create.content.logistics.depot.DepotBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.Register.LazyTickItem;

@Mod.EventBusSubscriber(modid = CreateLazyTick.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LazyTickClockHintOverlay {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        if (mc.player.getMainHandItem().getItem() != LazyTickItem.CLOCK.get()) {
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) {
            return;
        }

        if (!(mc.level.getBlockState(bhr.getBlockPos()).getBlock() instanceof DepotBlock)) {
            return;
        }

        double localY = bhr.getLocation().y - bhr.getBlockPos().getY();
        if (localY >= 0.5D) {
            return;
        }

        Font font = mc.font;
        Component line1 = Component.translatable("createlazytick.hud.config");
        Component line2 = Component.translatable("createlazytick.hud.click_hold_edit");

        int centerX = event.getWindow().getGuiScaledWidth() / 2;
        int baseY = event.getWindow().getGuiScaledHeight() / 2 + 14;

        int x1 = centerX - font.width(line1) / 2;
        int x2 = centerX - font.width(line2) / 2;

        event.getGuiGraphics().drawString(font, line1, x1, baseY, 0xE7CD73, true);
        event.getGuiGraphics().drawString(font, line2, x2, baseY + 11, 0xFFFFFF, true);
    }
}
