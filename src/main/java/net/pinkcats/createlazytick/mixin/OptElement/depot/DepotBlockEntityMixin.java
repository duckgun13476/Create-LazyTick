package net.pinkcats.createlazytick.mixin.OptElement.depot;

import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.helper.LazyTickScrollBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DepotBlockEntity.class)
public abstract class DepotBlockEntityMixin extends SmartBlockEntity {

    public DepotBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "addBehaviours", at = @At("RETURN"), remap = false)
    private void lazytick$addScrollBehaviour(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
        LazyTickScrollBehaviour.addTo(this, behaviours);
    }
}