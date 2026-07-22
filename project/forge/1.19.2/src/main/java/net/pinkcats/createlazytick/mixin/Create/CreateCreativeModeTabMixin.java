package net.pinkcats.createlazytick.mixin.Create;

import com.simibubi.create.AllCreativeModeTabs;
import com.simibubi.create.infrastructure.item.CreateCreativeModeTab;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.pinkcats.createlazytick.Register.LazyTickItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreateCreativeModeTab.class,remap = false)
public class CreateCreativeModeTabMixin {

    @Inject(method = "addItems", at = @At("RETURN"))
    private void createlazytick$addClockToBaseTab(NonNullList<ItemStack> items, boolean renderSpecial, CallbackInfo ci) {
        CreateCreativeModeTab self = (CreateCreativeModeTab) (Object) this;
        if (self != AllCreativeModeTabs.BASE_CREATIVE_TAB || renderSpecial) {
            return;
        }

        for (ItemStack stack : items) {
            if (stack.is(LazyTickItem.CLOCK.get())) {
                return;
            }
        }

        items.add(new ItemStack(LazyTickItem.CLOCK.get()));
    }
}
