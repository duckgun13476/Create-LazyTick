package net.pinkcats.createlazytick.bridge.Basin;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class BasinRecipeIndex {
    private static volatile Map<Item, IntSet> ITEM_THRESHOLDS = Collections.emptyMap();
    private static volatile Set<Item> NBT_SENSITIVE_ITEMS = Collections.emptySet();
    private static final AtomicLong DATA_VERSION = new AtomicLong(0);

    public static IntSet getThresholds(Item item) {
        return ITEM_THRESHOLDS.getOrDefault(item, IntSets.emptySet());
    }

    public static boolean isNbtSensitive(Item item) {
        return NBT_SENSITIVE_ITEMS.contains(item);
    }

    // global basin(press/mixer) recipe index builder
    public static void rebuild(RecipeManager recipeManager) {
        Map<Item, IntSet> newThresholds = new HashMap<>();
        Set<Item> newNbtSensitive = new HashSet<>();

        for (Recipe<?> recipe : recipeManager.getRecipes()) {
            Map<Item, Integer> recipeItemCounts = new HashMap<>();

            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;

                ItemStack[] matchingStacks = ingredient.getItems();
                for (ItemStack stack : matchingStacks) {
                    if (stack.isEmpty()) continue;
                    Item item = stack.getItem();

                    recipeItemCounts.put(item, recipeItemCounts.getOrDefault(item, 0) + 1);

                    if (stack.hasTag()) {
                        newNbtSensitive.add(item);
                    }
                }
            }

            for (Map.Entry<Item, Integer> entry : recipeItemCounts.entrySet()) {
                newThresholds.computeIfAbsent(entry.getKey(), k -> new IntOpenHashSet()).add(entry.getValue().intValue());
            }
        }

        Map<Item, IntSet> finalizedThresholds = new HashMap<>();
        for (Map.Entry<Item, IntSet> entry : newThresholds.entrySet()) {
            finalizedThresholds.put(entry.getKey(), IntSets.unmodifiable(entry.getValue()));
        }

        ITEM_THRESHOLDS = Collections.unmodifiableMap(finalizedThresholds);
        NBT_SENSITIVE_ITEMS = Collections.unmodifiableSet(newNbtSensitive);
        DATA_VERSION.incrementAndGet();
    }
}
