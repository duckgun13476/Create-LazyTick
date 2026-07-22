package net.pinkcats.createlazytick.bridge.Crafter;

import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems;
import net.minecraft.world.item.Item; // 引入 Item 类
import net.minecraft.world.item.ItemStack;
import net.pinkcats.createlazytick.mixin.OptElement.crafter.CrafterAccessor;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class CrafterGridSignature {
    private final int calculatedHashCode; // 哈希码
    public final boolean isCacheable; // 是否可以缓存(目前无NBT就可以)
    public final String nbtCulpritName; // 记录含NBT导致熔断的物品名称

    private final List<SimpleItemInfo> sortedGrid; // 格子

    public CrafterGridSignature(GroupedItems items) {
        CrafterAccessor accessor = (CrafterAccessor) items;

        // 准备对在合成器内的物品格进行简单扫描并索引
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

            if (!stack.getComponentsPatch().isEmpty()) {
                safe = false;
                // 记录物品名称 (例如 "minecraft:diamond_sword")
                culprit = stack.getItem().getDescriptionId();
                break;
            }

            int relX = entry.getKey().getKey() - minX;
            int relY = entry.getKey().getValue() - minY;

            // 计算哈希时使用 Item 和 Count，确保一致性
            h += Objects.hash(relX, relY, stack.getItem(), stack.getCount());

            // 构造 SimpleItemInfo，内部会自动提取快照数据
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

    // 精简版槽位对象
    private static class SimpleItemInfo {
        final int x, y;
        final Item item;       // 只存 Item 单例引用
        final int count;       // 只存数量数值
                               // 不再持有可变的 ItemStack 引用,其可能造成风险

        public SimpleItemInfo(int x, int y, ItemStack stack) {
            this.x = x;
            this.y = y;
            // 在构造时提取数据,生成不可变的快照
            // 即使原 stack 后来被消耗或修改,这里的 item 和 count 永远不会变
            this.item = stack.getItem();
            this.count = stack.getCount();
        }

        public boolean isSame(SimpleItemInfo other) {
            return this.x == other.x &&
                    this.y == other.y &&
                    this.item == other.item && // Item 是单例,可以用 == 比较
                    this.count == other.count; // 比较 int 数值
        }
    }
}