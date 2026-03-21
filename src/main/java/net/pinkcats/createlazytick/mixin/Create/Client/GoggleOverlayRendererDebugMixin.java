package net.pinkcats.createlazytick.mixin.Create.Client;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.logistics.depot.DepotBlock;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.pinkcats.createlazytick.Register.LazyTickItem;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickDepotDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GoggleOverlayRenderer.class, remap = false)
public class GoggleOverlayRendererDebugMixin {

    @Inject(method = "renderOverlay", at = @At("HEAD"))
    private static void createLazyTick$debugDepotOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!LazyTickDepotDebug.enabled() || mc == null || mc.player == null || mc.level == null) {
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (mc.player.getMainHandItem().getItem() != LazyTickItem.CLOCK.get()) {
            return;
        }

        ClientLevel world = mc.level;
        BlockPos originalPos = hit.getBlockPos();
        BlockState originalState = world.getBlockState(originalPos);
        BlockEntity originalBe = world.getBlockEntity(originalPos);

        BlockPos proxiedPos = GoggleOverlayRenderer.proxiedOverlayPosition(world, originalPos);
        BlockState proxiedState = world.getBlockState(proxiedPos);
        BlockEntity proxiedBe = world.getBlockEntity(proxiedPos);

        boolean depotTarget = originalState.getBlock() instanceof DepotBlock
                || proxiedState.getBlock() instanceof DepotBlock
                || originalBe instanceof DepotBlockEntity
                || proxiedBe instanceof DepotBlockEntity;

        boolean wearingGoggles = GogglesItem.isWearingGoggles(mc.player);
        boolean actualHelmetGoggles = mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GogglesItem;
        boolean proxiedHasGoggleInfo = proxiedBe instanceof IHaveGoggleInformation;
        boolean proxiedHasHoverInfo = proxiedBe instanceof IHaveHoveringInformation;

        LazyTickDepotDebug.log(mc, depotTarget ? "overlay_entry" : "overlay_probe",
                "originalPos=" + originalPos
                        + ", originalBlock=" + originalState.getBlock().getClass().getName()
                        + ", originalBe=" + describe(originalBe)
                        + ", proxiedPos=" + proxiedPos
                        + ", proxiedBlock=" + proxiedState.getBlock().getClass().getName()
                        + ", proxiedBe=" + describe(proxiedBe)
                        + ", depotTarget=" + depotTarget
                        + ", wearingGoggles=" + wearingGoggles
                        + ", actualHelmetGoggles=" + actualHelmetGoggles
                        + ", proxiedHasGoggleInfo=" + proxiedHasGoggleInfo
                        + ", proxiedHasHoverInfo=" + proxiedHasHoverInfo);
    }

    private static String describe(BlockEntity blockEntity) {
        return blockEntity == null ? "null" : blockEntity.getClass().getName();
    }
}
