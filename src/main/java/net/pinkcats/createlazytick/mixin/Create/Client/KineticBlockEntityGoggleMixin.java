package net.pinkcats.createlazytick.mixin.Create.Client;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipHelper;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickWhiteList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = KineticBlockEntity.class, remap = false)
public class KineticBlockEntityGoggleMixin {

    @Unique
    private int createLazyTick$tick = 0;

    @SuppressWarnings("ConstantConditions")  //压制instanceof警告
    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
    private void lazytick$appendKineticInfo(List<Component> tooltip, boolean isPlayerSneaking, CallbackInfoReturnable<Boolean> cir) {

        Minecraft mc = Minecraft.getInstance();
        if (!LazyTickTooltipHelper.shouldRender(mc)) return;

        LazyTickWhiteList whiteItem = LazyTickWhiteList.getByEntity(this);
        if (whiteItem == null || !whiteItem.isKinetic()) return;

        if (whiteItem == LazyTickWhiteList.PUMP) {
            LazyTickTooltipHelper.appendSimpleConfigInfo(this, tooltip);
            cir.setReturnValue(true);
            return;
        }

        if ((Object) this instanceof ISmartBlockEntityControl control) {
            int maxDelayTick = whiteItem.getMaxTick();
            this.createLazyTick$tick = LazyTickTooltipHelper.appendLazyTickInfo(control, tooltip, this.createLazyTick$tick, maxDelayTick);
            cir.setReturnValue(true);
        }
    }
}