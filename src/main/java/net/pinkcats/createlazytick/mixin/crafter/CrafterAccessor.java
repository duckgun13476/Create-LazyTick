package net.pinkcats.createlazytick.mixin.crafter;

import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(RecipeGridHandler.GroupedItems.class)
public interface CrafterAccessor {
    @Accessor(remap = false)
    Map<Pair<Integer, Integer>, ItemStack> getGrid();

    @Accessor(remap = false)
    int getMinX();

    @Accessor(remap = false)
    int getMinY();
}