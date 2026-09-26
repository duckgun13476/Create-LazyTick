package net.pinkcats.createlazytick.mixin.OptElement.fluid;


import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.pinkcats.createlazytick.config.ServerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = FluidTransportBehaviour.class, remap = false)
public class FluidLazyTickMixin extends BlockEntityBehaviour {


    @Shadow
    public static final BehaviourType<FluidTransportBehaviour> TYPE = new BehaviourType<>();


    public FluidLazyTickMixin(SmartBlockEntity be) {
        super(be);
    }

    @Unique
    int CLT$pipeTick = 0;

    @Shadow
    public BehaviourType<?> getType() {return TYPE;}

    @Inject(method = "tick" ,at=@At("HEAD" ),cancellable = true,remap = false)
    public void tick(CallbackInfo ci) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyFluid()) {
            return;
        }

        CLT$pipeTick++;
        if (CLT$pipeTick < ServerConfig.getFluidDelayMax())
        {
            ci.cancel();
            return;
        }
        CLT$pipeTick =0;
    }


}
