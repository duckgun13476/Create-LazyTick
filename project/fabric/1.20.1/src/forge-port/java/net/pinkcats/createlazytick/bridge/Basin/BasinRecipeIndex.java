package net.pinkcats.createlazytick.bridge.Basin;

import com.simibubi.create.AllRecipeTypes;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.config.ServerConfig;

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

    public static boolean isBasinOptimizationSafe = true;

    // global basin(press/mixer) recipe index builder
    public static void rebuild(RecipeManager recipeManager) {
        Set<RecipeType<?>> relevantTypes = new HashSet<>();
        addBasinRelatedRecipeTypes(relevantTypes);

        Map<Item, IntSet> newThresholds = new HashMap<>();
        Set<Item> newNbtSensitive = new HashSet<>();

        for (Recipe<?> recipe : recipeManager.getRecipes()) {
            if (!relevantTypes.contains(recipe.getType())) continue;

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

    private static void addBasinRelatedRecipeTypes (Set<RecipeType<?>> relevantTypes) {
        try {
            relevantTypes.add(AllRecipeTypes.BASIN.getType());
            relevantTypes.add(AllRecipeTypes.MIXING.getType());
            relevantTypes.add(AllRecipeTypes.COMPACTING.getType());
            relevantTypes.add(AllRecipeTypes.PRESSING.getType());
        } catch (Throwable e) {
            mes.error("An error occurred while trying to add basin related recipe types:\n" + e.getMessage());
        }

        List<? extends String> extraTypes = ServerConfig.getExtraBasinRelatedRecipeTypes();
        for (String typeStr : extraTypes) {
            ResourceLocation id = ResourceLocation.tryParse(typeStr);
            if (id == null) {
                mes.warn("Invalid recipe type resource location: " + typeStr);
                continue;
            }
            RecipeType<?> type = ForgeRegistries.RECIPE_TYPES.getValue(id);
            if (type == null) {
                mes.warn("Unknown recipe type: " + typeStr);
                continue;
            }
            relevantTypes.add(type);
        }

        if (relevantTypes.isEmpty()) {
            mes.error("Basin related recipe type is empty!Something might went wrong.");
            mes.error("For safety reasons, basin-related optimizations have been disabled.");
            mes.error("The optimization won't work before next server restart or reload.");
            isBasinOptimizationSafe = false;
        }
    }
}