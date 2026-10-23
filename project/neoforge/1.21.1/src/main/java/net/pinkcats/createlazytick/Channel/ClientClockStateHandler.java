package net.pinkcats.createlazytick.Channel;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.ClockDisplayState;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipWhiteList;
import net.pinkcats.createlazytick.helper.util.SmartLazyTickStateHelper;

/** Loaded only by a clientbound handler; no client types occur in the packet's signature. */
public final class ClientClockStateHandler {
    private ClientClockStateHandler() {}

    public static void accept(String dimension, BlockPos pos, ClockDisplayState state) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().toString().equals(dimension) || !level.isLoaded(pos)) return;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (LazyTickTooltipWhiteList.getByEntity(blockEntity) == null) return;
        ISmartBlockEntityControl control = blockEntity instanceof ISmartBlockEntityControl smart ? smart : SmartLazyTickStateHelper.control(blockEntity);
        if (control != null) control.createLazyTick$applyDisplayState(state);
        LazyTickClientStateCache.put(dimension, pos, state.toTag());
    }
}
