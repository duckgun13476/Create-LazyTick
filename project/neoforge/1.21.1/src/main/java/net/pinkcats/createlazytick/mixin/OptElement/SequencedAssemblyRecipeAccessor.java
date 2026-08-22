package net.pinkcats.createlazytick.mixin.OptElement;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = SequencedAssemblyRecipe.class, remap = false)
public interface SequencedAssemblyRecipeAccessor {

    @Invoker("appliesTo")
    boolean createLazyTick$appliesTo(ResourceLocation id, ItemStack input);

    @Invoker("getNextRecipe")
    SequencedRecipe<?> createLazyTick$getNextRecipe(ItemStack input);

    @Invoker("advance")
    ItemStack createLazyTick$advance(ResourceLocation id, ItemStack input, RandomSource random);
}
