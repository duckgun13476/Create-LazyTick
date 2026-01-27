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

import static net.pinkcats.createlazytick.config.ServerConfig.saw_cache_max;
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
        if (!ServerConfig.enable_lazy_tick || !ServerConfig.enable_cache_saw) {
            return;
        }
        if (IsServerReload)
            createLazyTick$ClearCache();

        ItemStack HandleItem = inventory.getStackInSlot(0);
        if (HandleItem.isEmpty()) return;
        //System.out.println(inventory.getStackInSlot(0));

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
        List<? extends Recipe<?>> assemblyRecipes = createLazyTick$GetAssemblyRecipeCache(HandleItem);

        if (!assemblyRecipes.isEmpty()) {
            Recipe<?> recipe = assemblyRecipes.get(0);
            // check filter here
            if (level != null && filtering.test(recipe.getResultItem(level.registryAccess()))) {
                createLazyTick$UpdateSnapshot(currentInputItem, currentFilter, assemblyRecipes);
                cir.setReturnValue(assemblyRecipes);
                cir.cancel();
                return;
            }
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

    @Unique
    private Map<Item, ImmutableList<com.simibubi.create.content.kinetics.saw.CuttingRecipe>> createLazyTick$assemblyRecipeCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Item, ImmutableList<com.simibubi.create.content.kinetics.saw.CuttingRecipe>> eldest) {
            return size() > saw_cache_max; // max cache count
        }
    };

    @Unique
    private ImmutableList
            <com.simibubi.create.content.kinetics.saw.CuttingRecipe> createLazyTick$GetAssemblyRecipeCache(ItemStack itemStack) {

        if (createLazyTick$assemblyRecipeCache.containsKey(itemStack.getItem())) {
            return createLazyTick$assemblyRecipeCache.get(itemStack.getItem());
        }

        boolean hasTag = itemStack.hasTag();

        //System.out.println("not use cache original "+itemStack.getItem()+" "+createLazyTick$assemblyRecipeCache.size());
        Optional<CuttingRecipe> assemblyRecipe = Optional.empty();
        if (level != null) {
            assemblyRecipe = SequencedAssemblyRecipe.getRecipe(level, itemStack,
                    AllRecipeTypes.CUTTING.getType(), CuttingRecipe.class);
        }
        // has A recipe
        if (assemblyRecipe.isPresent()) {
            if (!hasTag) {
                // has no NBT -> cache?(A?) and return
                createLazyTick$assemblyRecipeCache.put(itemStack.getItem(), ImmutableList.of(assemblyRecipe.get()));
            }
            return ImmutableList.of(assemblyRecipe.get());
        } else {
            //has no A recipe
            if (!hasTag) {
                // has no NBT -> blacklist and return
                createLazyTick$assemblyRecipeCache.put(itemStack.getItem(), ImmutableList.of());
                return ImmutableList.of();
                //System.out.println("Is empty");
            } else {
                // has NBT
                if (level == null) return ImmutableList.of();
                Optional<CuttingRecipe> cleanCheck = SequencedAssemblyRecipe.getRecipe(level, new ItemStack(itemStack.getItem()),
                        AllRecipeTypes.CUTTING.getType(), CuttingRecipe.class);
                // clean item has no recipe -> blacklist and return
                if (cleanCheck.isEmpty()) {
                    createLazyTick$assemblyRecipeCache.put(itemStack.getItem(), ImmutableList.of());
                }
                // clean item has recipe -> just return
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
        createLazyTick$assemblyRecipeCache.clear();
        lazytick$lastInputItem = null;
        lazytick$lastFilterStackSnapshot = ItemStack.EMPTY;
        lazytick$lastFilterInstance = null;
        lazytick$lastFilteredResult = null;
    }

}
