package net.pinkcats.createlazytick.mixin.crafter;

import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import net.minecraft.world.item.ItemStack;
import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.CreateLazyTick; // 引入主类以使用 LOGGER
import net.pinkcats.createlazytick.helper.CrafterCacheStats;
import net.pinkcats.createlazytick.helper.CrafterGridSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;

@Mixin(RecipeGridHandler.class)
public abstract class CrafterRecipeMixin {

    @Unique
    private static boolean lazytick$hasLoggedFullCache = false;

    @Unique
    private static long lazytick$cooldownEndTime = 0;

    @Unique
    private static final Map<CrafterGridSignature, ItemStack> lazytick$recipeCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CrafterGridSignature, ItemStack> eldest) {
            // 使用配置项 crafter_global_cache_max
            int maxCacheSize = Config.crafter_global_cache_max;
            if (size() > maxCacheSize) {
                // 日志需要检查 Debug 开关
                if (!lazytick$hasLoggedFullCache && Config.enable_cache_crafter_debugger) {
                    CreateLazyTick.LOGGER.info("[CreateLazyTick] Crafter Cache hit max capacity ({}). Eviction started.", maxCacheSize);
                    lazytick$hasLoggedFullCache = true;
                }
                return true;
            }
            return false;
        }
    };

    @Inject(method = "tryToApplyRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private static void lazytick$checkCache(net.minecraft.world.level.Level world, GroupedItems items, CallbackInfoReturnable<ItemStack> cir) {
        if (!Config.enable_lazy_tick || !Config.enable_cache_crafter) return;

        if (IsServerReload) {
            lazytick$recipeCache.clear();
            lazytick$hasLoggedFullCache = false;
            CrafterCacheStats.reset();

            // crafter_cache_record_delay(全局缓存需要再RELOAD彻底完成后才开始记录,否则可能记录到错误配方)
            long delayMillis = Config.crafter_cache_record_delay * 1000L;
            lazytick$cooldownEndTime = System.currentTimeMillis() + delayMillis;

            if (Config.enable_cache_crafter_debugger) {
                CreateLazyTick.LOGGER.info("[LazyTick] Server Reload Detected. Cache cleared. Cooldown: {}s", Config.crafter_cache_record_delay);
            }
        }

        // 检查是否处于重载冷却期
        if (System.currentTimeMillis() < lazytick$cooldownEndTime) {
            CrafterCacheStats.onCooldownSkip();
            return;
        }

        // 原版逻辑
        items.calcStats();
        CrafterGridSignature signature = new CrafterGridSignature(items);

        // 存在NBT,缓存不安全,退回原版处理
        if (!signature.isCacheable) {
            CrafterCacheStats.onNbtSkip(signature.nbtCulpritName);
            return;
        }

        // 如果存在缓存,读取缓存并返回
        if (lazytick$recipeCache.containsKey(signature)) {
            ItemStack cachedResult = lazytick$recipeCache.get(signature);
            CrafterCacheStats.onHit();
            cir.setReturnValue(cachedResult == null ? null : cachedResult.copy());
            cir.cancel();
        }
    }

    // 在方法Return时试图进行缓存
    @Inject(method = "tryToApplyRecipe", at = @At("RETURN"), remap = false)
    private static void lazytick$saveCache(net.minecraft.world.level.Level world, GroupedItems items, CallbackInfoReturnable<ItemStack> cir) {
        if (!Config.enable_lazy_tick || !Config.enable_cache_crafter) return;

        if (System.currentTimeMillis() < lazytick$cooldownEndTime) return;

        CrafterGridSignature signature = new CrafterGridSignature(items);

        if (!signature.isCacheable) return;

        ItemStack result = cir.getReturnValue();
        lazytick$recipeCache.put(signature, result == null ? null : result.copy());
        CrafterCacheStats.onMiss();
    }
}