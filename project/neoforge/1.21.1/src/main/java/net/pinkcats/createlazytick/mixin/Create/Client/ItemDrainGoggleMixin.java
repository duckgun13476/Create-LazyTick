package net.pinkcats.createlazytick.mixin.Create.Client;

import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipRenderer;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipTool;
import net.pinkcats.createlazytick.helper.util.SmartLazyTickStateHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ItemDrainBlockEntity.class, remap = false)
public class ItemDrainGoggleMixin {

    @Unique
    private int createLazyTick$tick = 0;

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
    private void lazytick$appendDrainInfo(List<Component> tooltip, boolean isPlayerSneaking, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (!LazyTickTooltipTool.shouldRender(mc)) return;

        ISmartBlockEntityControl control = (Object) this instanceof ISmartBlockEntityControl smart
                ? smart
                : SmartLazyTickStateHelper.control((ItemDrainBlockEntity) (Object) this);
        if (control != null) {
            int maxDelayTick = ServerConfig.getItemDrainDelayMax();
            this.createLazyTick$tick = LazyTickTooltipRenderer.appendLazyTickInfo(control, tooltip, this.createLazyTick$tick, maxDelayTick);
            cir.setReturnValue(true);
        }
    }
}
