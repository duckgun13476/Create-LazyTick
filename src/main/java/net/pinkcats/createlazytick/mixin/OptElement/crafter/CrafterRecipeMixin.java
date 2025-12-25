package net.pinkcats.createlazytick.mixin.OptElement.crafter;

import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import net.minecraft.world.item.ItemStack;
import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.helper.CrafterCacheStats;
import net.pinkcats.createlazytick.helper.CrafterGridSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;
import static net.pinkcats.createlazytick.helper.RecipeCacheTool.CrafterRecipeCache;

@Mixin(RecipeGridHandler.class)
public abstract class CrafterRecipeMixin {

    @Inject(method = "tryToApplyRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lazytick$checkCache(net.minecraft.world.level.Level world, GroupedItems items, CallbackInfoReturnable<ItemStack> cir) {

        if (createLazyTick$NotEnable()) return;

        // 原版逻辑
        items.calcStats();
        CrafterGridSignature signature = new CrafterGridSignature(items);

        // 存在NBT,缓存不安全,退回原版处理
        if (!signature.isCacheable) {
            CrafterCacheStats.onNbtSkip(signature.nbtCulpritName);
            return;
        }

        // 如果存在缓存,读取缓存并返回
        if (CrafterRecipeCache.containsKey(signature)) {
            ItemStack cachedResult = CrafterRecipeCache.get(signature);
            CrafterCacheStats.onHit();
            cir.setReturnValue(cachedResult == null ? null : cachedResult.copy());
            cir.cancel();
        }
    }

    // 在方法Return时试图进行缓存
    @Inject(method = "tryToApplyRecipe", at = @At("RETURN"), remap = false)
    private static void lazytick$saveCache(net.minecraft.world.level.Level world, GroupedItems items, CallbackInfoReturnable<ItemStack> cir) {

        if (createLazyTick$NotEnable()) return;

        CrafterGridSignature signature = new CrafterGridSignature(items);

        if (!signature.isCacheable) return;

        ItemStack result = cir.getReturnValue();
        CrafterRecipeCache.put(signature, result == null ? null : result.copy());
        CrafterCacheStats.onMiss();
    }


    @Unique
    private static boolean createLazyTick$NotEnable() {
        return !Config.enable_lazy_tick || !Config.enable_cache_crafter || IsServerReload;
    }

}