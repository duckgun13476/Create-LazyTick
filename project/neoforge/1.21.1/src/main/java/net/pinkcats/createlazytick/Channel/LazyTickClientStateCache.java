package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class LazyTickClientStateCache {
    private static final int MAX_ENTRIES = 64;
    private static final Map<CacheKey, CompoundTag> CACHE = new LinkedHashMap<>();

    private LazyTickClientStateCache() {
    }

    public static void put(String dimension, BlockPos pos, CompoundTag tag) {
        if (dimension == null || pos == null || tag == null) {
            return;
        }
        CACHE.put(new CacheKey(dimension, pos), tag.copy());
        if (CACHE.size() > MAX_ENTRIES) CACHE.remove(CACHE.keySet().iterator().next());
    }

    public static CompoundTag get(String dimension, BlockPos pos) {
        if (dimension == null || pos == null) {
            return null;
        }
        CompoundTag tag = CACHE.get(new CacheKey(dimension, pos));
        return tag == null ? null : tag.copy();
    }

    private record CacheKey(String dimension, BlockPos pos) {
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CacheKey key)) {
                return false;
            }
            return Objects.equals(dimension, key.dimension) && Objects.equals(pos, key.pos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimension, pos);
        }
    }
}
