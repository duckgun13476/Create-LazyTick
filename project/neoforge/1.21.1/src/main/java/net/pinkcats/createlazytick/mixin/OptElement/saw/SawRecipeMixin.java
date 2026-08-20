package net.pinkcats.createlazytick.mixin.OptElement.saw;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.foundation.recipe.RecipeConditions;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.diag.DiagnosticLog;
import net.pinkcats.createlazytick.helper.RecipeCacheTool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = SawBlockEntity.class, remap = false)
public class SawRecipeMixin extends BlockBreakingKineticBlockEntity {

    @Shadow(remap = false)
    public ProcessingInventory inventory;

    public SawRecipeMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Cache only Create's static cutting candidates.  The enclosing getRecipes()
     * method still executes normally, so return-stage extensions (for example
     * Create Central Kitchen's dynamic Cutting Board conversion) receive a fresh,
     * mutable result list on every invocation.
     */
    @Redirect(
            method = "getRecipes",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/recipe/RecipeFinder;get(Ljava/lang/Object;Lnet/minecraft/world/level/Level;Ljava/util/function/Predicate;)Ljava/util/List;"
            ),
            remap = false
    )
    private List<RecipeHolder<? extends Recipe<?>>> createLazyTick$getCachedCuttingCandidates(
            Object cacheKey, net.minecraft.world.level.Level recipeLevel,
            Predicate<RecipeHolder<? extends Recipe<?>>> recipeTypes) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableCacheSaw()) {
            DiagnosticLog.saw(DiagnosticLog.Event.SAW_CACHE_BYPASS, "pos=" + worldPosition.toShortString() + " reason=disabled");
            return RecipeFinder.get(cacheKey, recipeLevel, recipeTypes);
        }
        if (IsServerReload) {
            createLazyTick$ClearCache();
        }

        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty() || RecipeCacheTool.isSequencedAssemblyItem(input)) {
            DiagnosticLog.saw(DiagnosticLog.Event.SAW_CACHE_BYPASS,
                    "pos=" + worldPosition.toShortString() + " reason=" + (input.isEmpty() ? "empty" : "sequenced"));
            return RecipeFinder.get(cacheKey, recipeLevel, recipeTypes);
        }
        return createLazyTick$getRecipeCache(input, cacheKey, recipeLevel, recipeTypes);
    }

    @Override
    protected BlockPos getBreakingPos() {
        return getBlockPos().relative(getBlockState().getValue(SawBlock.FACING));
    }

    @Unique
    private Map<Item, List<RecipeHolder<? extends Recipe<?>>>> createLazyTick$recipeCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Item, List<RecipeHolder<? extends Recipe<?>>>> eldest) {
            return size() > ServerConfig.getSawCacheMax(); // max cache count
        }
    };

    @Unique
    private List<RecipeHolder<? extends Recipe<?>>> createLazyTick$getRecipeCache(
            ItemStack itemStack, Object cacheKey, net.minecraft.world.level.Level recipeLevel,
            Predicate<RecipeHolder<? extends Recipe<?>>> recipeTypes) {

        // check cache if it has then return
        if (createLazyTick$recipeCache.containsKey(itemStack.getItem())) {
            List<RecipeHolder<? extends Recipe<?>>> cached = createLazyTick$recipeCache.get(itemStack.getItem());
            DiagnosticLog.saw(DiagnosticLog.Event.SAW_CACHE_HIT, "pos=" + worldPosition.toShortString() + " candidates=" + cached.size());
            return cached;
        }

        boolean hasTag = !itemStack.getComponentsPatch().isEmpty();
        DiagnosticLog.saw(DiagnosticLog.Event.SAW_CACHE_MISS, "pos=" + worldPosition.toShortString() + " components=" + hasTag);
        //System.out.println("not use cache "+itemStack+createLazyTick$recipeCache.size());
        List<RecipeHolder<? extends Recipe<?>>> startedSearch = RecipeFinder.get(cacheKey, recipeLevel, recipeTypes);

        List<RecipeHolder<? extends Recipe<?>>> recipes = startedSearch.stream()
                .filter(RecipeConditions.firstIngredientMatches(inventory.getStackInSlot(0)))
                .filter(r -> !AllRecipeTypes.shouldIgnoreInAutomation(r))
                .collect(Collectors.toList());

        // has recipe
        if (!recipes.isEmpty()) {
            // has no tag -> cache
            if (!hasTag) {
                createLazyTick$recipeCache.put(itemStack.getItem(), List.copyOf(recipes));
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
    }

}
