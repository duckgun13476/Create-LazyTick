package net.pinkcats.createlazytick.mixin;

import com.simibubi.create.content.fluids.spout.FillingBySpout;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.helper.Spout.SpoutCacheKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;
import static net.pinkcats.createlazytick.helper.RecipeCacheTool.AMOUNT_CACHE;
import static net.pinkcats.createlazytick.helper.RecipeCacheTool.CAN_FILL_CACHE;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = FillingBySpout.class, remap = false)
public abstract class SpoutRecipeMixin {


    @Inject(method = "canItemBeFilled", at = @At("HEAD"), cancellable = true)
    private static void createLazyTick$checkCanFillCache(Level world, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (createLazyTick$NotEnable()) return;

        if (stack.hasTag()) return;

        Item item = stack.getItem();

        // Acquire cache.
        if (CAN_FILL_CACHE.containsKey(item)) {
            cir.setReturnValue(CAN_FILL_CACHE.get(item));
            cir.cancel();
        }
    }

    @Inject(method = "canItemBeFilled", at = @At("RETURN"))
    private static void createLazyTick$captureCanFillCache(Level world, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {

        if (createLazyTick$NotEnable()) return;

        if (stack.hasTag()) return;

        // Find and save cache.
        CAN_FILL_CACHE.put(stack.getItem(), cir.getReturnValue());
    }

    @Inject(method = "getRequiredAmountForItem", at = @At("HEAD"), cancellable = true)
    private static void createLazyTick$checkAmountCache(Level world, ItemStack stack, FluidStack availableFluid, CallbackInfoReturnable<Integer> cir) {

        if (createLazyTick$NotEnable()) return;

        if (stack.hasTag()) return;
        if (availableFluid.hasTag()) return;

        // Acquire cache.
        SpoutCacheKey key = new SpoutCacheKey(stack.getItem(), availableFluid.getFluid());
        if (AMOUNT_CACHE.containsKey(key)) {
            cir.setReturnValue(AMOUNT_CACHE.get(key));
            cir.cancel();
        }
    }


    @Inject(method = "getRequiredAmountForItem", at = @At("RETURN"))
    private static void createLazyTick$captureAmountCache(Level world, ItemStack stack, FluidStack availableFluid, CallbackInfoReturnable<Integer> cir) {

        if (createLazyTick$NotEnable()) return;

        if (stack.hasTag()) return;
        if (availableFluid.hasTag()) return;

        // 构建缓存(物品 + 流体 做键,流体数量做值)
        // Find and save cache.
        SpoutCacheKey key = new SpoutCacheKey(stack.getItem(), availableFluid.getFluid());
        AMOUNT_CACHE.put(key, cir.getReturnValue());
    }


    @Unique
    private static boolean createLazyTick$NotEnable() {
        return !Config.enable_lazy_tick || !Config.enable_cache_spout || IsServerReload;
    }
}