package net.pinkcats.createlazytick.helper.tooltip;

import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import net.pinkcats.createlazytick.Config;

import java.util.function.Supplier;

public enum LazyTickWhiteList {
    //PUMP 和 PIPE 要特殊处理,勿动
    ARM(ArmBlockEntity.class, () -> Config.arm_delay_max),
    FUNNEL(FunnelBlockEntity.class, () -> Config.funnel_delay_max),
    DEPOT(DepotBlockEntity.class, () -> Config.depot_delay_max),
    BELT(BeltBlockEntity.class, () -> Config.belt_delay_max),
    CRAFTER(MechanicalCrafterBlockEntity.class, () -> Config.crafter_redstone_delay_max),
    PUMP(PumpBlockEntity.class, () -> Config.fluid_delay_max),
    PIPE(FluidPipeBlockEntity.class, () -> Config.fluid_delay_max),
    DRAIN(ItemDrainBlockEntity.class, () -> Config.item_drain_delay_max),
    CHUTE(ChuteBlockEntity.class, () -> Config.chute_delay_max);

    private final Class<?> targetClass;
    private final Supplier<Integer> maxTickSupplier;

    LazyTickWhiteList(Class<?> targetClass, Supplier<Integer> maxTickSupplier) {
        this.targetClass = targetClass;
        this.maxTickSupplier = maxTickSupplier;
    }

    public static LazyTickWhiteList getByEntity(Object be) {
        for (LazyTickWhiteList entry : values()) {
            if (entry.targetClass.isInstance(be)) {
                return entry;
            }
        }
        return null;
    }

    public int getMaxTick() {
        return maxTickSupplier.get();
    }
}