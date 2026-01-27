package net.pinkcats.createlazytick.helper;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.bridge.Crafter.CrafterGridSignature;
import net.pinkcats.createlazytick.bridge.Spout.SpoutCacheKey;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RecipeCacheTool {

    // Spout Cache Class
    public static final Map<Item, Boolean> CAN_FILL_CACHE = Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Item, Boolean> eldest) {
            return size() > ServerConfig.spout_cache_max;
        }
    });


    public static final Map<SpoutCacheKey, Integer> AMOUNT_CACHE = Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<SpoutCacheKey, Integer> eldest) {
            return size() > ServerConfig.spout_cache_max;
        }
    });




    public static boolean IsCrafterCacheFull = false;
    // Crafter Cache Class
    public static final Map<CrafterGridSignature, ItemStack> CrafterRecipeCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CrafterGridSignature, ItemStack> eldest) {
            // 使用配置项 crafter_global_cache_max
            int maxCacheSize = ServerConfig.crafter_global_cache_max;
            if (size() > maxCacheSize) {
                // 日志需要检查 Debug 开关
                if (!IsCrafterCacheFull && ServerConfig.enable_cache_crafter_debugger) {
                    CreateLazyTick.LOGGER.info("[CreateLazyTick] Crafter Cache hit max capacity ({}). Eviction started.", maxCacheSize);
                    IsCrafterCacheFull = true;
                }
                return true;
            }
            return false;
        }
    };



}
