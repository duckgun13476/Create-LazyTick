package net.pinkcats.createlazytick.helper.tooltip;

import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import net.pinkcats.createlazytick.config.ServerConfig;

import java.util.function.Supplier;

public enum LazyTickTooltipWhiteList {
    // Kinetic
    ARM(ArmBlockEntity.class, ServerConfig::getArmDelayMax, Type.KINETIC),
    BELT(BeltBlockEntity.class, ServerConfig::getBeltDelayMax, Type.KINETIC),
    CRAFTER(MechanicalCrafterBlockEntity.class, ServerConfig::getCrafterRedstoneDelayMax, Type.KINETIC),
    PUMP(PumpBlockEntity.class, ServerConfig::getFluidDelayMax, Type.KINETIC),
    SAW(SawBlockEntity.class, ServerConfig::getSawDelayMax, Type.KINETIC),

    // Smart
    FUNNEL(FunnelBlockEntity.class, ServerConfig::getFunnelDelayMax, Type.SMART),
    DEPOT(DepotBlockEntity.class, ServerConfig::getDepotDelayMax, Type.SMART),
    PIPE(FluidPipeBlockEntity.class, ServerConfig::getFluidDelayMax, Type.SMART),

    // Special(have override)
    DRAIN(ItemDrainBlockEntity.class, ServerConfig::getItemDrainDelayMax, Type.SPECIAL),
    CHUTE(ChuteBlockEntity.class, ServerConfig::getChuteDelayMax, Type.SMART);

    private final Class<?> targetClass;
    private final Supplier<Integer> maxTickSupplier;
    private final Type type; // 新增字段

    public enum Type {
        KINETIC, // 继承自 KineticBlockEntity
        SMART,    // 仅继承自 SmartBlockEntity
        SPECIAL   // 必须单独处理的方块实体类
    }

    LazyTickTooltipWhiteList(Class<?> targetClass, Supplier<Integer> maxTickSupplier, Type type) {
        this.targetClass = targetClass;
        this.maxTickSupplier = maxTickSupplier;
        this.type = type;
    }

    public static LazyTickTooltipWhiteList getByEntity(Object be) {
        for (LazyTickTooltipWhiteList entry : values()) {
            if (entry.targetClass.isInstance(be)) {
                return entry;
            }
        }
        return null;
    }

    public int getMaxTick() {
        return maxTickSupplier.get();
    }


    public Type getType() {
        return type;
    }

    public boolean isKinetic() {
        return this.type == Type.KINETIC;
    }

    public boolean isSmart() {
        return this.type == Type.SMART;
    }

    public boolean isSpecial() {
        return this.type == Type.SPECIAL;
    }
}