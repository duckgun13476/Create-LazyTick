package net.pinkcats.createlazytick.mixin.OptElement.basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.pinkcats.createlazytick.bridge.Basin.BasinRecipeCacheEntry;
import net.pinkcats.createlazytick.bridge.Basin.BasinRecipeCacheKey;
import net.pinkcats.createlazytick.bridge.Basin.BasinRecipeIndex;
import net.pinkcats.createlazytick.bridge.Basin.BasinStateSnapshot;
import net.pinkcats.createlazytick.bridge.Basin.IBasinOptimization;
import net.pinkcats.createlazytick.config.ServerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static net.pinkcats.createlazytick.bridge.Basin.BasinRecipeIndex.isBasinOptimizationSafe;

@Mixin(value = BasinOperatingBlockEntity.class)
public abstract class BasinOperatingLazyTickMixin {
    @Unique
    private static final int clt$MAX_RECIPE_CACHE = 5;
    @Unique
    private static final int clt$MAX_CANDIDATES_PER_KEY = 5;
    @Unique
    private static final int clt$STALE_RETRY_WINDOW = 5;

    @Shadow(remap = false) protected abstract Optional<BasinBlockEntity> getBasin();
    @Shadow(remap = false) protected abstract boolean isRunning();

    @Shadow(remap = false) protected Recipe<?> currentRecipe;
    @Shadow(remap = false) public abstract void startProcessingBasin();

    @Unique
    private BasinStateSnapshot clt$cachedSnapshot;

    @Unique
    private final List<BasinRecipeCacheEntry> clt$recipeCache = new ArrayList<>(clt$MAX_RECIPE_CACHE);
    @Unique
    private int clt$staleRetryBudget = 0;
    @Unique
    private boolean clt$awaitingStaleRetryResult = false;

    @Unique
    private void clt$sendData() {
        ((SyncedBlockEntity) (Object) this).sendData();
    }

    @Unique
    private void clt$rememberRecipe(BasinRecipeCacheKey key, Recipe<?> recipe) {
        if (key == null || recipe == null) return;

        for (int i = 0; i < clt$recipeCache.size(); i++) {
            BasinRecipeCacheEntry entry = clt$recipeCache.get(i);
            if (!entry.matches(key)) continue;

            List<Recipe<?>> recipes = new ArrayList<>(entry.recipes());
            recipes.remove(recipe);
            recipes.add(0, recipe);
            while (recipes.size() > clt$MAX_CANDIDATES_PER_KEY) {
                recipes.remove(recipes.size() - 1);
            }

            clt$recipeCache.remove(i);
            clt$recipeCache.add(0, new BasinRecipeCacheEntry(key, recipes));
            while (clt$recipeCache.size() > clt$MAX_RECIPE_CACHE) {
                clt$recipeCache.remove(clt$recipeCache.size() - 1);
            }
            return;
        }

        clt$recipeCache.add(0, new BasinRecipeCacheEntry(key, Collections.singletonList(recipe)));
        while (clt$recipeCache.size() > clt$MAX_RECIPE_CACHE) {
            clt$recipeCache.remove(clt$recipeCache.size() - 1);
        }
    }

    @Unique
    private void clt$rememberRecipes(BasinRecipeCacheKey key, List<Recipe<?>> recipes) {
        if (key == null || recipes == null || recipes.isEmpty()) return;
        int limit = Math.min(recipes.size(), clt$MAX_CANDIDATES_PER_KEY);
        for (int i = limit - 1; i >= 0; i--) {
            clt$rememberRecipe(key, recipes.get(i));
        }
    }

    @Unique
    private Recipe<?> clt$getCachedRecipe(BasinBlockEntity basin, BasinRecipeCacheKey key) {
        for (int i = 0; i < clt$recipeCache.size(); i++) {
            BasinRecipeCacheEntry entry = clt$recipeCache.get(i);
            if (!entry.matches(key)) continue;

            List<Recipe<?>> recipes = new ArrayList<>(entry.recipes());
            for (int recipeIndex = 0; recipeIndex < recipes.size(); recipeIndex++) {
                Recipe<?> recipe = recipes.get(recipeIndex);
                if (!BasinRecipe.match(basin, recipe)) continue;

                recipes.remove(recipeIndex);
                recipes.add(0, recipe);
                clt$recipeCache.remove(i);
                clt$recipeCache.add(0, new BasinRecipeCacheEntry(key, recipes));
                return recipe;
            }

            clt$recipeCache.remove(i);
            clt$recipeCache.add(0, new BasinRecipeCacheEntry(entry.key(), recipes));
            return null;
        }
        return null;
    }

    @Unique
    private boolean clt$recipeMayUseItem(Recipe<?> recipe, Item item) {
        if (recipe == null) return false;
        for (var ingredient : recipe.getIngredients()) {
            for (ItemStack stack : ingredient.getItems()) {
                if (!stack.isEmpty() && stack.getItem() == item) {
                    return true;
                }
            }
        }
        return false;
    }

    @Unique
    private boolean clt$cacheMayUseItem(Item item) {
        if (currentRecipe != null && clt$recipeMayUseItem(currentRecipe, item)) {
            return true;
        }
        for (BasinRecipeCacheEntry entry : clt$recipeCache) {
            for (Recipe<?> recipe : entry.recipes()) {
                if (clt$recipeMayUseItem(recipe, item)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Unique
    private boolean clt$couldTriggerNewRecipe(BasinStateSnapshot oldFp, BasinStateSnapshot newFp) {
        if (oldFp.isOutputBufferEmpty() != newFp.isOutputBufferEmpty()) return true;
        if (oldFp.getHeatLevel() != newFp.getHeatLevel()) return true;
        if (newFp.hasNbtSensitiveItems()) return true;
        if (!ItemStack.matches(oldFp.getFilterSnapshot(), newFp.getFilterSnapshot())) return true;
        if (oldFp.getFluidHash() != newFp.getFluidHash()) return true;

        Object2IntMap<Item> oldItems = oldFp.getItemQuantities();
        Object2IntMap<Item> newItems = newFp.getItemQuantities();

        for (Object2IntMap.Entry<Item> entry : newItems.object2IntEntrySet()) {
            Item item = entry.getKey();
            int newQty = entry.getIntValue();
            int oldQty = oldItems.getInt(item);

            if (newQty > oldQty) {
                // Bootstrap phase: we do not know any basin recipe yet, so any input growth
                // should get one chance to build the local recipe cache.
                if (currentRecipe == null && clt$recipeCache.isEmpty()) {
                    return true;
                }

                IntSet thresholds = BasinRecipeIndex.getThresholds(item);
                for (int threshold : thresholds) {
                    if (oldQty < threshold && newQty >= threshold) {
                        return true;
                    }
                }

                // Basin recipe thresholds are not always recoverable from the global index
                // (for example, some processing recipes encode counts in custom data rather
                // than as repeated vanilla ingredients). If a cached recipe may use this item,
                // any increase can make a previously invalid recipe become valid.
                if (clt$cacheMayUseItem(item)) {
                    return true;
                }
            } else if (newQty < oldQty) {
                return true;
            }
        }
        return false;
    }

    @Inject(remap = false, method = "updateBasin", at = @At("HEAD"), cancellable = true)
    private void clt$onUpdateBasin(CallbackInfoReturnable<Boolean> cir) {
        if (!ServerConfig.getEnableLazyTick()
                || !ServerConfig.getEnableLazyBasin()
                || !isBasinOptimizationSafe) return;

        if (isRunning()) return;

        Optional<BasinBlockEntity> basinOpt = getBasin();
        if (basinOpt.isEmpty()) return;
        BasinBlockEntity basin = basinOpt.get();

        if (!((IBasinOptimization) basin).clt$isOutputBufferEmpty()) {
            clt$cachedSnapshot = null;
            clt$staleRetryBudget = 0;
            clt$awaitingStaleRetryResult = false;
            return;
        }

        if (!basin.canContinueProcessing()) {
            clt$cachedSnapshot = null;
            clt$staleRetryBudget = 0;
            clt$awaitingStaleRetryResult = false;
            return;
        }

        BasinStateSnapshot currentSnapshot = new BasinStateSnapshot(basin);
        BasinRecipeCacheKey currentKey = new BasinRecipeCacheKey(basin);
        boolean staleCachedRecipe = false;
        boolean forceRetryAfterStale = false;

        if (currentRecipe != null) {
            if (BasinRecipe.match(basin, currentRecipe)) {
                clt$rememberRecipe(currentKey, currentRecipe);
                clt$cachedSnapshot = null;
                clt$staleRetryBudget = 0;
                clt$awaitingStaleRetryResult = false;
                startProcessingBasin();
                clt$sendData();
                cir.setReturnValue(true);
                return;
            }

            staleCachedRecipe = true;
            clt$staleRetryBudget = clt$STALE_RETRY_WINDOW;
            clt$awaitingStaleRetryResult = true;
            currentRecipe = null;
        }

        if (clt$cachedSnapshot != null) {
            if (!staleCachedRecipe && clt$cachedSnapshot.equals(currentSnapshot)) {
                if (clt$staleRetryBudget > 0) {
                    forceRetryAfterStale = true;
                } else {
                    cir.setReturnValue(true);
                    return;
                }
            }

            boolean shouldWake = clt$couldTriggerNewRecipe(clt$cachedSnapshot, currentSnapshot);
            if (!staleCachedRecipe && !shouldWake && !forceRetryAfterStale) {
                clt$cachedSnapshot = currentSnapshot;
                cir.setReturnValue(true);
                return;
            }
        }

        if (forceRetryAfterStale && clt$staleRetryBudget > 0) {
            clt$staleRetryBudget--;
        }
        clt$cachedSnapshot = currentSnapshot;
    }

    @Inject(method = "getMatchingRecipes", at = @At("HEAD"), cancellable = true, remap = false)
    private void clt$onGetMatchingRecipes(CallbackInfoReturnable<List<Recipe<?>>> cir) {
        if (!ServerConfig.getEnableLazyTick()
                || !ServerConfig.getEnableLazyBasin()
                || !isBasinOptimizationSafe) return;

        Optional<BasinBlockEntity> basinOpt = getBasin();
        if (basinOpt.isEmpty()) return;
        BasinBlockEntity basin = basinOpt.get();

        BasinStateSnapshot currentSnapshot = new BasinStateSnapshot(basin);
        BasinRecipeCacheKey currentKey = new BasinRecipeCacheKey(basin);

        if (currentRecipe != null && BasinRecipe.match(basin, currentRecipe)) {
            clt$staleRetryBudget = 0;
            clt$awaitingStaleRetryResult = false;
            clt$rememberRecipe(currentKey, currentRecipe);
            cir.setReturnValue(Collections.singletonList(currentRecipe));
            return;
        }

        Recipe<?> cachedRecipe = clt$getCachedRecipe(basin, currentKey);
        if (cachedRecipe != null) {
            clt$staleRetryBudget = 0;
            clt$awaitingStaleRetryResult = false;
            currentRecipe = cachedRecipe;
            cir.setReturnValue(Collections.singletonList(cachedRecipe));
            return;
        }
    }

    @Inject(method = "getMatchingRecipes", at = @At("RETURN"), remap = false)
    private void clt$afterGetMatchingRecipes(CallbackInfoReturnable<List<Recipe<?>>> cir) {
        if (!ServerConfig.getEnableLazyTick()
                || !ServerConfig.getEnableLazyBasin()
                || !isBasinOptimizationSafe) return;

        List<Recipe<?>> recipes = cir.getReturnValue();
        if (recipes == null || recipes.isEmpty()) {
            return;
        }

        Optional<BasinBlockEntity> basinOpt = getBasin();
        if (basinOpt.isEmpty()) return;

        BasinBlockEntity basin = basinOpt.get();
        BasinRecipeCacheKey currentKey = new BasinRecipeCacheKey(basin);
        Recipe<?> recipe = recipes.get(0);
        clt$staleRetryBudget = 0;
        clt$awaitingStaleRetryResult = false;
        currentRecipe = recipe;
        clt$rememberRecipes(currentKey, recipes);
    }
}
