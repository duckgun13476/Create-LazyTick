package net.pinkcats.createlazytick.mixin.saw;


import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.recipe.RecipeConditions;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.pinkcats.createlazytick.Config.saw_cache_max;
import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(SawBlockEntity.class)
public class SawRecipeMixin extends BlockBreakingKineticBlockEntity {

    @Shadow
    private static final Object cuttingRecipesKey = new Object();

    @Shadow
    private FilteringBehaviour filtering;

    @Shadow
    public ProcessingInventory inventory;

    public SawRecipeMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "getRecipes",at=@At("HEAD" ),cancellable = true,remap = false)
    private void getRecipes(CallbackInfoReturnable<List<? extends Recipe<?>>> cir) {
        if (!Config.enable_lazy_tick || !Config.enable_cache_saw) {
            return;
        }
        if (IsServerReload)
            createLazyTick$ClearCache();

        ItemStack HandleItem = inventory.getStackInSlot(0);
        //System.out.println(inventory.getStackInSlot(0));
        ImmutableList
                <com.simibubi.create.content.kinetics.saw.CuttingRecipe> A = createLazyTick$GetAssemblyRecipeCache(HandleItem);

        if (!A.isEmpty()) {
            cir.setReturnValue(A);
            cir.cancel();
            return;
        }

        List<? extends Recipe<?>> V = createLazyTick$GetRecipeCache(HandleItem);
        cir.setReturnValue(V);
        cir.cancel();
    }




    @Unique
    private ImmutableList
            <com.simibubi.create.content.kinetics.saw.CuttingRecipe> createLazyTick$GetAssemblyRecipeCache(ItemStack itemStack) {

        if (createLazyTick$assemblyRecipeCache.containsKey(itemStack.getItem())) {
            return createLazyTick$assemblyRecipeCache.get(itemStack.getItem());

        } else {
            //System.out.println("not use cache original "+itemStack.getItem()+" "+createLazyTick$assemblyRecipeCache.size());
            Optional<CuttingRecipe> assemblyRecipe = Optional.empty();
            if (level != null) {
                assemblyRecipe = SequencedAssemblyRecipe.getRecipe(level, itemStack,
                        AllRecipeTypes.CUTTING.getType(), CuttingRecipe.class);
            }
            if (assemblyRecipe.isPresent() && filtering.test(assemblyRecipe.get()
                    .getResultItem(level.registryAccess()))) {

                createLazyTick$assemblyRecipeCache.put(itemStack.getItem(), ImmutableList.of(assemblyRecipe.get()));
                return ImmutableList.of(assemblyRecipe.get());
            } else  {
                //System.out.println("Is empty");
                createLazyTick$assemblyRecipeCache.put(itemStack.getItem(), ImmutableList.of());
                return ImmutableList.of();
            }


        }

    }



    @Override
    protected BlockPos getBreakingPos() {
        return getBlockPos().relative(getBlockState().getValue(SawBlock.FACING));
    }

    @Unique
    private Map<Item, List<? extends Recipe<?>>> createLazyTick$recipeCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Item, List<? extends Recipe<?>>> eldest) {
            return size() > saw_cache_max; // max cache count
        }
    };


    @Unique
    private Map<Item, ImmutableList<com.simibubi.create.content.kinetics.saw.CuttingRecipe>> createLazyTick$assemblyRecipeCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Item, ImmutableList<com.simibubi.create.content.kinetics.saw.CuttingRecipe>> eldest) {
            return size() > saw_cache_max; // max cache count
        }
    };

    @Unique
    private List<? extends Recipe<?>> createLazyTick$GetRecipeCache(ItemStack itemStack) {

        // check cache if it has then return
        if (createLazyTick$recipeCache.containsKey(itemStack.getItem())) {
            return createLazyTick$recipeCache.get(itemStack.getItem());
        }

        else {
            //System.out.println("not use cache "+itemStack+createLazyTick$recipeCache.size());
            Predicate<Recipe<?>> types = RecipeConditions.isOfType(AllRecipeTypes.CUTTING.getType(),
                    AllConfigs.server().recipes.allowStonecuttingOnSaw.get() ? RecipeType.STONECUTTING : null);

            List<Recipe<?>> startedSearch = RecipeFinder.get(cuttingRecipesKey, level, types);
            List<? extends Recipe<?>> recipes = startedSearch.stream()
                    .filter(RecipeConditions.outputMatchesFilter(filtering))
                    .filter(RecipeConditions.firstIngredientMatches(inventory.getStackInSlot(0)))
                    .filter(r -> !AllRecipeTypes.shouldIgnoreInAutomation(r))
                    .collect(Collectors.toList());

            createLazyTick$recipeCache.put(itemStack.getItem(), recipes);
            return recipes;

        }
    }

    @Unique
    private void createLazyTick$ClearCache() {
        createLazyTick$recipeCache.clear();
        createLazyTick$assemblyRecipeCache.clear();
    }

}
