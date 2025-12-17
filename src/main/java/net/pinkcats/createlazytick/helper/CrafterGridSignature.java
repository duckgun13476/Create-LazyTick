package net.pinkcats.createlazytick.helper;

import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import net.minecraft.world.item.ItemStack;
import net.pinkcats.createlazytick.mixin.crafter.CrafterAccessor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class CrafterGridSignature {
    private final int calculatedHashCode; // 哈希码
    public final boolean isCacheable; // 是否可以缓存(目前无NBT就可以)
    public final String nbtCulpritName; // 记录含NBT导致熔断的物品名称

    private final List<SimpleItemInfo> sortedGrid; // 格子

    public CrafterGridSignature(GroupedItems items) {
        CrafterAccessor accessor = (CrafterAccessor) items;

        // 准备对在合成器内的物品格进行简单扫描并索引(或许可以规避超大合成器配方造成的负优化?)
        Map<Pair<Integer, Integer>, ItemStack> grid = accessor.getGrid();
        int minX = accessor.getMinX();
        int minY = accessor.getMinY();

        int h = 0;
        // 默认没有NBT物品
        boolean safe = true;
        String culprit = null;

        List<SimpleItemInfo> tempGrid = new ArrayList<>(grid.size());

        for (Map.Entry<Pair<Integer, Integer>, ItemStack> entry : grid.entrySet()) {
            ItemStack stack = entry.getValue();
            if (stack.isEmpty()) continue;

            if (stack.hasTag()) {
                safe = false;
                // 记录物品名称 (例如 "minecraft:diamond_sword")
                culprit = stack.getItem().getDescriptionId();
                break;
            }

            int relX = entry.getKey().getKey() - minX;
            int relY = entry.getKey().getValue() - minY;

            h += Objects.hash(relX, relY, stack.getItem(), stack.getCount());
            tempGrid.add(new SimpleItemInfo(relX, relY, stack));
        }

        // 先按 Y 坐标（行）排序，如果 Y 相同，再按 X 坐标（列）排序
        if (safe) {
            tempGrid.sort(Comparator.comparingInt((SimpleItemInfo i) -> i.y).thenComparingInt(i -> i.x));
        }

        this.isCacheable = safe;
        this.nbtCulpritName = culprit;
        this.calculatedHashCode = h;
        this.sortedGrid = safe ? tempGrid : null;
    }

    @Override
    public int hashCode() {
        return calculatedHashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CrafterGridSignature other = (CrafterGridSignature) obj;

        if (this.calculatedHashCode != other.calculatedHashCode) return false;
        if (this.sortedGrid == null || other.sortedGrid == null) return false;
        if (this.sortedGrid.size() != other.sortedGrid.size()) return false;

        // 开始对比物品
        for (int i = 0; i < this.sortedGrid.size(); i++) {
            if (!this.sortedGrid.get(i).isSame(other.sortedGrid.get(i))) {
                return false;
            }
        }
        return true;
    }

    // 精简版槽位中的物品对象
    private static class SimpleItemInfo {
        final int x, y;
        final ItemStack stack;

        public SimpleItemInfo(int x, int y, ItemStack stack) {
            this.x = x;
            this.y = y;
            this.stack = stack;
        }

        public boolean isSame(SimpleItemInfo other) {
            return this.x == other.x &&
                    this.y == other.y &&
                    this.stack.getItem() == other.stack.getItem() &&
                    this.stack.getCount() == other.stack.getCount();
        }
    }
}