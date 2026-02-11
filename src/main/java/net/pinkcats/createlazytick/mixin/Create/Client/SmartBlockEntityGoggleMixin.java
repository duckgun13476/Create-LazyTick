package net.pinkcats.createlazytick.mixin.Create.Client;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipRenderer;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipTool;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipWhiteList;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = SmartBlockEntity.class, remap = false)
public class SmartBlockEntityGoggleMixin implements IHaveGoggleInformation {

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Minecraft mc = Minecraft.getInstance();
        if (!LazyTickTooltipTool.shouldRender(mc)) return false;

        LazyTickTooltipWhiteList whiteItem = LazyTickTooltipWhiteList.getByEntity(this);
        if (whiteItem == null || !whiteItem.isSmart()) return false;

        if (whiteItem == LazyTickTooltipWhiteList.PIPE) {
            LazyTickTooltipRenderer.appendSimpleConfigInfo(this, tooltip);
            return true;
        }

        if (this instanceof ISmartBlockEntityControl control) {
            int maxDelayTick = whiteItem.getMaxTick();
            int CLT$tick = 0;
            LazyTickTooltipRenderer.appendLazyTickInfo(control, tooltip, CLT$tick, maxDelayTick);
            return true;
        }
        return false;
    }
}