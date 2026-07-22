package net.pinkcats.createlazytick.mixin.Create.Client;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickDepotDebug;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipRenderer;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipTool;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipWhiteList;
import net.pinkcats.createlazytick.helper.util.SmartLazyTickStateHelper;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = SmartBlockEntity.class, remap = false)
public class SmartBlockEntityGoggleMixin implements IHaveGoggleInformation {

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        Minecraft mc = Minecraft.getInstance();
        LazyTickTooltipWhiteList whiteItem = LazyTickTooltipWhiteList.getByEntity(this);
        boolean isDepot = whiteItem == LazyTickTooltipWhiteList.DEPOT;
        if (isDepot) {
            LazyTickDepotDebug.logBlockEntity(mc, "smart_goggle_entry", ((SmartBlockEntity) (Object) this).getBlockPos(),
                    (SmartBlockEntity) (Object) this, "entered addToGoggleTooltip");
        }

        if (!LazyTickTooltipTool.shouldRender(mc)) {
            if (isDepot) {
                LazyTickDepotDebug.logBlockEntity(mc, "smart_goggle_entry", ((SmartBlockEntity) (Object) this).getBlockPos(),
                        (SmartBlockEntity) (Object) this, "blocked by shouldRender=false");
            }
            return false;
        }

        if (whiteItem == null || !whiteItem.isSmart()) {
            if (isDepot) {
                LazyTickDepotDebug.logBlockEntity(mc, "smart_goggle_entry", ((SmartBlockEntity) (Object) this).getBlockPos(),
                        (SmartBlockEntity) (Object) this, "blocked by whitelist check, whiteItem=" + whiteItem);
            }
            return false;
        }

        if (whiteItem == LazyTickTooltipWhiteList.PIPE) {
            LazyTickTooltipRenderer.appendSimpleConfigInfo(this, tooltip);
            return true;
        }

        ISmartBlockEntityControl control = this instanceof ISmartBlockEntityControl smart
                ? smart
                : SmartLazyTickStateHelper.control((SmartBlockEntity) (Object) this);
        if (control != null) {
            int maxDelayTick = whiteItem.getMaxTick();
            int CLT$tick = 0;
            LazyTickTooltipRenderer.appendLazyTickInfo(control, tooltip, CLT$tick, maxDelayTick);
            if (isDepot) {
                LazyTickDepotDebug.logBlockEntity(mc, "smart_goggle_entry", ((SmartBlockEntity) (Object) this).getBlockPos(),
                        (SmartBlockEntity) (Object) this,
                        "tooltip appended, dynamic=" + control.createLazyTick$getDynamicValue()
                                + ", forced=" + control.createLazyTick$getForcedValue()
                                + ", current=" + control.createLazyTick$getCurrentSuperTick());
            }
            return true;
        }
        if (isDepot) {
            LazyTickDepotDebug.logBlockEntity(mc, "smart_goggle_entry", ((SmartBlockEntity) (Object) this).getBlockPos(),
                    (SmartBlockEntity) (Object) this, "blocked: not ISmartBlockEntityControl");
        }
        return false;
    }
}
