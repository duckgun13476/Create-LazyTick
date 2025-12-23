package net.pinkcats.createlazytick.helper.Spout;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import java.util.Objects;

// 注液器缓存键工具类
public class SpoutCacheKey {
    private final Item item;
    private final Fluid fluid;

    public SpoutCacheKey(Item item, Fluid fluid) {
        this.item = item;
        this.fluid = fluid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpoutCacheKey that = (SpoutCacheKey) o;
        return Objects.equals(item, that.item) && Objects.equals(fluid, that.fluid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, fluid);
    }
}