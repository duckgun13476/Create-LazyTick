package net.pinkcats.createlazytick.mixin.Create;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.pinkcats.createlazytick.helper.util.SmartLazyTickStateHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SmartBlockEntity.class, remap = false)
public abstract class DepotStatePersistenceMixin {

    @Inject(method = "initialize", at = @At("RETURN"))
    private void lazytick$initializeState(CallbackInfo ci) {
        SmartLazyTickStateHelper.initialize((SmartBlockEntity) (Object) this);
    }

    @Inject(method = "invalidate", at = @At("HEAD"))
    private void lazytick$invalidateState(CallbackInfo ci) {
        SmartLazyTickStateHelper.invalidate((SmartBlockEntity) (Object) this);
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void lazytick$writeDepotState(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        SmartLazyTickStateHelper.write((SmartBlockEntity) (Object) this, tag);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void lazytick$readDepotState(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        SmartLazyTickStateHelper.read((SmartBlockEntity) (Object) this, tag);
    }
}
