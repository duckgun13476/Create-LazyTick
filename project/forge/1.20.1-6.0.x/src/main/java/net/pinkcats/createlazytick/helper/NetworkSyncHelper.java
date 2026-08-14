package net.pinkcats.createlazytick.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.pinkcats.createlazytick.Channel.ClientData;
import net.pinkcats.createlazytick.Channel.ClockSyncPacket;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.util.LazyTickLogic;

public class NetworkSyncHelper {
    public static void createLazyTick$syncPacketData(
            ISmartBlockEntityControl control,
            Level level,
            BlockPos pos,
            int currentDelayTick,
            int maxDelayTick
    ) {
        if (level == null || level.isClientSide) return;

        ClientData data = ClockSyncPacket.takePendingRequest(level.dimension().location().toString(), pos);
        if (data == null) return;

        int cmd = data.getExtraData();
        if (cmd != 0) {
            control.CLT$onClientRequest(cmd);
        }

        control.lazytick$setSyncedTier(currentDelayTick, maxDelayTick);
        LazyTickLogic.updateState(control);
    }
}
