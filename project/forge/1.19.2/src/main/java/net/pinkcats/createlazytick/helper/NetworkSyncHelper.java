package net.pinkcats.createlazytick.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.pinkcats.createlazytick.Channel.ClientData;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.util.LazyTickLogic;

import java.util.Iterator;
import java.util.Objects;

import static net.pinkcats.createlazytick.Channel.ClockSyncPacket.PacketCache;

public class NetworkSyncHelper {
    public static void createLazyTick$syncPacketData (
            ISmartBlockEntityControl control,
            Level level,
            BlockPos pos,
            int currentDelayTick,
            int maxDelayTick
    ) {
        //mes.error(PacketCache.size());
        if (level == null || level.isClientSide) return;
        if (PacketCache.isEmpty()) return;


        String currentDim = level.dimension().location().toString();

        for (Iterator<ClientData> it = PacketCache.iterator(); it.hasNext();) {
            ClientData data = it.next();

            if (!currentDim.equals(data.getDimension())) continue;
            if (!Objects.equals(pos, data.getPos())) continue;

            int cmd = data.getExtraData();
            if (cmd != 0) {
                control.CLT$onClientRequest(cmd);
            }
            //

            control.lazytick$setSyncedTier(currentDelayTick, maxDelayTick);

            it.remove();
            LazyTickLogic.updateState(control);
            break;
        }
            //System.out.println("---");
            //System.out.println(control.CLT$getMaxTicks());
            //control.CLT$setMaxTicks(CurrentDelayTick);
            //System.out.println(control.CLT$getMaxTicks());
    }
}
