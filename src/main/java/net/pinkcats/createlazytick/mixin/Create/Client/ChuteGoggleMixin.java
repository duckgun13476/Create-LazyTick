package net.pinkcats.createlazytick.mixin.Create.Client;

import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ChuteBlockEntity.class ,remap = false)
public class ChuteGoggleMixin {
    @Unique
    private int createLazyTick$tick = 0;

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
    private void lazytick$appendChuteInfo(List<Component> tooltip, boolean isPlayerSneaking, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (!LazyTickTooltipHelper.shouldRender(mc)) return;

        if ((Object) this instanceof ISmartBlockEntityControl control) {
            int maxDelayTick = Config.chute_delay_max;
            this.createLazyTick$tick = LazyTickTooltipHelper.appendLazyTickInfo(control, tooltip, this.createLazyTick$tick, maxDelayTick);
            cir.setReturnValue(true);
        }
    }
}
