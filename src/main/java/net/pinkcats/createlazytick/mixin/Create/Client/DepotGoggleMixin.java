package net.pinkcats.createlazytick.mixin.Create.Client;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickDepotDebug;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipRenderer;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipTool;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipWhiteList;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = DepotBlockEntity.class, remap = false)
public class DepotGoggleMixin implements IHaveGoggleInformation {

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Minecraft mc = Minecraft.getInstance();
        DepotBlockEntity depot = (DepotBlockEntity) (Object) this;

        LazyTickDepotDebug.logBlockEntity(mc, "depot_goggle_entry", depot.getBlockPos(), depot,
                "entered direct Depot addToGoggleTooltip");

        if (!LazyTickTooltipTool.shouldRender(mc)) {
            LazyTickDepotDebug.logBlockEntity(mc, "depot_goggle_entry", depot.getBlockPos(), depot,
                    "blocked by shouldRender=false");
            return false;
        }

        if ((Object) this instanceof ISmartBlockEntityControl control) {
            int maxDelayTick = LazyTickTooltipWhiteList.DEPOT.getMaxTick();
            LazyTickTooltipRenderer.appendLazyTickInfo(control, tooltip, 0, maxDelayTick);
            LazyTickDepotDebug.logBlockEntity(mc, "depot_goggle_entry", depot.getBlockPos(), depot,
                    "tooltip appended directly for Depot");
            return true;
        }

        LazyTickDepotDebug.logBlockEntity(mc, "depot_goggle_entry", depot.getBlockPos(), depot,
                "blocked: Depot is not ISmartBlockEntityControl");
        return false;
    }
}
