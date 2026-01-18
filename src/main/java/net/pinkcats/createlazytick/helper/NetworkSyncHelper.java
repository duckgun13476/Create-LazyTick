package net.pinkcats.createlazytick.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.pinkcats.createlazytick.Channel.ClientData;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;

import java.util.Objects;

import static net.pinkcats.createlazytick.Channel.ClockSyncPacket.PacketCache;

public class NetworkSyncHelper {
    public static void createLazyTick$syncPacketData (ISmartBlockEntityControl control,
                                                      Level level, BlockPos pos, int currentDelayTick, int maxDelayTick) {
        if (level != null && !level.isClientSide) {
            if (!PacketCache.isEmpty()){
                for (ClientData data : PacketCache) {
                    String currentDim = level.dimension().location().toString();
                    if (data.getDimension().equals(currentDim)) {
                        if (Objects.equals(pos, data.getPos())) {
                            int cmd = data.getExtraData();
                            if (cmd != 0) {
                                control.CLT$onClientRequest(cmd);
                            }
                            control.lazytick$setSyncedTier(currentDelayTick, maxDelayTick);
                            PacketCache.remove(data);
                            LazyTickLogic.updateState(control);
                            break;
                        }
                    }
                }
            }

            //System.out.println("---");
            //System.out.println(control.CLT$getMaxTicks());
            //control.CLT$setMaxTicks(CurrentDelayTick);
            //System.out.println(control.CLT$getMaxTicks());
        }
    }
}
