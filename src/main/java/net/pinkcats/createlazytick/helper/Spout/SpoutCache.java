package net.pinkcats.createlazytick.helper.Spout;

import net.minecraft.world.item.Item;
import net.pinkcats.createlazytick.Config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SpoutCache {

    // 缓存定义
    public static final Map<Item, Boolean> CAN_FILL_CACHE = Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Item, Boolean> eldest) {
            return size() > Config.spout_cache_max;
        }
    });

    public static final Map<SpoutCacheKey, Integer> AMOUNT_CACHE = Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<SpoutCacheKey, Integer> eldest) {
            return size() > Config.spout_cache_max;
        }
    });




}
