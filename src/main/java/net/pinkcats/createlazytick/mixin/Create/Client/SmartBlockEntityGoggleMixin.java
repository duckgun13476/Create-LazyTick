package net.pinkcats.createlazytick.mixin.Create.Client;

import com.simibubi.create.content.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipHelper;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickWhiteList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(value = SmartBlockEntity.class, remap = false)
public class SmartBlockEntityGoggleMixin implements IHaveHoveringInformation {

    @Unique
    private int createLazyTick$tick = 0;

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Minecraft mc = Minecraft.getInstance();
        if (!LazyTickTooltipHelper.shouldRender(mc)) return false;

        LazyTickWhiteList whiteItem = LazyTickWhiteList.getByEntity(this);
        if (whiteItem == null) return false;

        if ((Object) this instanceof ItemDrainBlockEntity) return false;

        if (this instanceof ISmartBlockEntityControl control) {
            int maxDelayTick = whiteItem.getMaxTick();
            this.createLazyTick$tick = LazyTickTooltipHelper.appendLazyTickInfo(control, tooltip, this.createLazyTick$tick, maxDelayTick);
            return true;
        }
        return false;
    }
}