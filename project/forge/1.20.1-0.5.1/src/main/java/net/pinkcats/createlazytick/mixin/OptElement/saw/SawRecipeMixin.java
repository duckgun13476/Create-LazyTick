package net.pinkcats.createlazytick.mixin.OptElement.saw;


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
import net.pinkcats.createlazytick.config.ServerConfig;
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

import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = SawBlockEntity.class,remap = false)
public class SawRecipeMixin extends BlockBreakingKineticBlockEntity {

    @Shadow(remap = false)
    private static final Object cuttingRecipesKey = new Object();

    @Shadow(remap = false)
    private FilteringBehaviour filtering;

    @Shadow(remap = false)
    public ProcessingInventory inventory;

    @Unique private ItemStack lazytick$lastFilterStackSnapshot = ItemStack.EMPTY;

    // Address
    @Unique private ItemStack lazytick$lastFilterInstance = null;

    @Unique private Item lazytick$lastInputItem = null;

    @Unique private List<? extends Recipe<?>> lazytick$lastFilteredResult = null;

    public SawRecipeMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "getRecipes",at=@At("HEAD" ),cancellable = true,remap = false)
    private void getRecipes(CallbackInfoReturnable<List<? extends Recipe<?>>> cir) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableCacheSaw()) {
            return;
        }
        if (IsServerReload)
            createLazyTick$ClearCache();

        ItemStack HandleItem = inventory.getStackInSlot(0);
        if (HandleItem.isEmpty()) return;
        //System.out.println(inventory.getStackInSlot(0));

        boolean hasSequenced = HandleItem.getTag() != null && HandleItem.getTag().contains("SequencedAssembly");

        if (hasSequenced) {
            return;
        }

        // 1.get current status
        ItemStack currentFilter = filtering.getFilter();
        Item currentInputItem = HandleItem.getItem();

        // 2.If address(filter) is same & input item is same
        if (lazytick$lastFilteredResult != null &&
                currentInputItem == lazytick$lastInputItem &&
                currentFilter == lazytick$lastFilterInstance) {

            cir.setReturnValue(lazytick$lastFilteredResult);
            cir.cancel();
            return;
        }

        // 3.If address changed but content isn't
        if (lazytick$lastFilteredResult != null &&
                currentInputItem == lazytick$lastInputItem &&
                ItemStack.isSameItemSameTags(currentFilter, lazytick$lastFilterStackSnapshot)) {

            this.lazytick$lastFilterInstance = currentFilter;

            cir.setReturnValue(lazytick$lastFilteredResult);
            cir.cancel();
            return;
        }

        // 4.If both of address & content changed
        // assemblyRecipes -> (old name: A,type: ImmutableList
        //                <com.simibubi.create.content.kinetics.saw.CuttingRecipe>)
        // check assembly recipe first
        Optional<CuttingRecipe> assemblyRecipe = Optional.empty();
        if (level != null) {
            assemblyRecipe = SequencedAssemblyRecipe.getRecipe(level, HandleItem,
                    AllRecipeTypes.CUTTING.getType(), CuttingRecipe.class);
        }

        if (assemblyRecipe.isPresent() && filtering.test(assemblyRecipe.get().getResultItem(level.registryAccess()))) {
            // 直接返回序列装配配方，不更新任何缓存
            cir.setReturnValue(ImmutableList.of(assemblyRecipe.get()));
            cir.cancel();
            return;
        }

        // check normal cutting recipe then
        // cachedAllRecipes -> (old name: V,type: List<? extends Recipe<?>>)
        List<? extends Recipe<?>> cachedAllRecipes = createLazyTick$GetRecipeCache(HandleItem);

        // here fix the bug
        List<Recipe<?>> filteredRecipes = cachedAllRecipes.stream()
                .filter(RecipeConditions.outputMatchesFilter(filtering))
                .collect(Collectors.toList());

        createLazyTick$UpdateSnapshot(currentInputItem, currentFilter, filteredRecipes);

        cir.setReturnValue(filteredRecipes);
        cir.cancel();
    }


    @Unique
    private void createLazyTick$UpdateSnapshot(Item input, ItemStack filter, List<? extends Recipe<?>> result) {
        this.lazytick$lastInputItem = input;

        this.lazytick$lastFilterStackSnapshot = filter.copy(); // deep copy for compare when filter changed
        this.lazytick$lastFilterInstance = filter;             // instance for address compare when not changed

        this.lazytick$lastFilteredResult = result;
    }

    @Override
    protected BlockPos getBreakingPos() {
        return getBlockPos().relative(getBlockState().getValue(SawBlock.FACING));
    }

    @Unique
    private Map<Item, List<? extends Recipe<?>>> createLazyTick$recipeCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Item, List<? extends Recipe<?>>> eldest) {
            return size() > ServerConfig.getSawCacheMax(); // max cache count
        }
    };

    @Unique
    private List<? extends Recipe<?>> createLazyTick$GetRecipeCache(ItemStack itemStack) {

        // check cache if it has then return
        if (createLazyTick$recipeCache.containsKey(itemStack.getItem())) {
            return createLazyTick$recipeCache.get(itemStack.getItem());
        }

        boolean hasTag = itemStack.hasTag();
        //System.out.println("not use cache "+itemStack+createLazyTick$recipeCache.size());
        Predicate<Recipe<?>> types = RecipeConditions.isOfType(AllRecipeTypes.CUTTING.getType(),
                AllConfigs.server().recipes.allowStonecuttingOnSaw.get() ? RecipeType.STONECUTTING : null);

        List<Recipe<?>> startedSearch = RecipeFinder.get(cuttingRecipesKey, level, types);

        List<? extends Recipe<?>> recipes = startedSearch.stream()
                .filter(RecipeConditions.firstIngredientMatches(inventory.getStackInSlot(0)))
                .filter(r -> !AllRecipeTypes.shouldIgnoreInAutomation(r))
                .collect(Collectors.toList());

        // has recipe
        if (!recipes.isEmpty()) {
            // has no tag -> cache
            if (!hasTag) {
                createLazyTick$recipeCache.put(itemStack.getItem(), recipes);
            }
            return recipes;
        } else {
            // has no recipe
            if (!hasTag) {
                // has no tag -> blacklist and return
                createLazyTick$recipeCache.put(itemStack.getItem(), Collections.emptyList());
                return Collections.emptyList();
            } else {
                // has tag -> clean test
                ItemStack cleanStack = new ItemStack(itemStack.getItem());

                boolean cleanHasRecipe = startedSearch.stream()
                        .filter(RecipeConditions.firstIngredientMatches(cleanStack))
                        .anyMatch(r -> !AllRecipeTypes.shouldIgnoreInAutomation(r));

                // clean has no recipe -> blacklist and return
                if (!cleanHasRecipe) {
                    createLazyTick$recipeCache.put(itemStack.getItem(), Collections.emptyList());
                }

                // clean has recipe -> just return
                return Collections.emptyList();
            }
        }
    }

    @Unique
    private void createLazyTick$ClearCache() {
        createLazyTick$recipeCache.clear();
        lazytick$lastInputItem = null;
        lazytick$lastFilterStackSnapshot = ItemStack.EMPTY;
        lazytick$lastFilterInstance = null;
        lazytick$lastFilteredResult = null;
    }

}