package net.pinkcats.createlazytick.mixin.Create;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(value = ScrollOptionBehaviour.class, remap = false)
public abstract class ScrollOptionBehaviourGuardMixin {

    @Unique
    private static final Field createLazyTick$OPTIONS_FIELD = createLazyTick$findOptionsField();

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void createLazyTick$guardInvalidIndex(CallbackInfoReturnable<Enum> cir) {
        int corrected = createLazyTick$sanitizeIndex();
        cir.setReturnValue(createLazyTick$getOptions()[corrected]);
    }

    @Unique
    private int createLazyTick$sanitizeIndex() {
        Enum[] options = createLazyTick$getOptions();
        if (options == null || options.length == 0) {
            return 0;
        }

        ScrollValueBehaviour scrollValueBehaviour = (ScrollValueBehaviour) (Object) this;
        BlockEntityBehaviour blockEntityBehaviour = (BlockEntityBehaviour) (Object) this;
        int currentValue = scrollValueBehaviour.value;
        int maxIndex = options.length - 1;
        if (currentValue >= 0 && currentValue <= maxIndex) {
            return currentValue;
        }

        int corrected = Math.max(0, Math.min(currentValue, maxIndex));
        scrollValueBehaviour.value = corrected;

        SmartBlockEntity blockEntity = blockEntityBehaviour.blockEntity;
        if (blockEntity != null) {
            blockEntity.setChanged();
        }

        return corrected;
    }

    @Unique
    private Enum[] createLazyTick$getOptions() {
        if (createLazyTick$OPTIONS_FIELD == null) {
            return null;
        }

        try {
            return (Enum[]) createLazyTick$OPTIONS_FIELD.get(this);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static Field createLazyTick$findOptionsField() {
        try {
            Field field = ScrollOptionBehaviour.class.getDeclaredField("options");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
