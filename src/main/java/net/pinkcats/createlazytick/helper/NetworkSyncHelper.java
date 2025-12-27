package net.pinkcats.createlazytick.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.pinkcats.createlazytick.Channel.ClientData;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;

import java.util.Objects;

import static net.pinkcats.createlazytick.Channel.ClockSyncPacket.PacketCache;
import static net.pinkcats.createlazytick.item.LazyTickClockItem.StateDirection;

public class NetworkSyncHelper {
    public static void createLazyTick$processUserControl(ISmartBlockEntityControl control,int maxDelayTick) {
        // Force Control
        byte CLTState = control.createLazyTick$ControlState();
        if (CLTState != 0){
            int CurrentDelayTick = maxDelayTick * (CLTState - 1) / Math.max(1, StateDirection - 2);
            control.createLazyTick$setLazyTickInterval(CurrentDelayTick);
            control.createLazyTick$setDelayForced(true);
            return;
        }
        control.createLazyTick$setDelayForced(false);
        //System.out.println(createLazyTick$CurrentDelayTick);
    }

    public static void createLazyTick$syncPacketData (ISmartBlockEntityControl control,
                                                      Level level, BlockPos pos, int currentDelayTick, int maxDelayTick) {
        if (level != null && !level.isClientSide) {
            if (!PacketCache.isEmpty()){
                for (ClientData data : PacketCache) {
                    if (data.getDimension() == level.dimension().hashCode()) {
                        if (Objects.equals(pos, data.getPos())) {
                            control.lazytick$setSyncedTier(currentDelayTick, maxDelayTick);
                            PacketCache.remove(data);
                            ForcedActiveManager.register(level, pos);
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
