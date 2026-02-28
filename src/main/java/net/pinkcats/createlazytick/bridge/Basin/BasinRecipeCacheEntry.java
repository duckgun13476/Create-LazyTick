package net.pinkcats.createlazytick.bridge.Basin;

import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BasinRecipeCacheEntry {
    private final BasinRecipeCacheKey key;
    private final List<Recipe<?>> recipes;

    public BasinRecipeCacheEntry(BasinRecipeCacheKey key, List<Recipe<?>> recipes) {
        this.key = key;
        this.recipes = new ArrayList<>(recipes);
    }

    public boolean matches(BasinRecipeCacheKey other) {
        return key.equals(other);
    }

    public BasinRecipeCacheKey key() {
        return key;
    }

    public List<Recipe<?>> recipes() {
        return Collections.unmodifiableList(recipes);
    }
}
