package net.pinkcats.NutUI.menu.architect.data;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class NutMenuInfo {

    private NutMenuInfo() {}

    // ===== One object contains all params (pass once) =====
    public static final class data {
        private final ResourceLocation menuId;
        private final ResourceLocation texture;
        private final int x, y;
        private final Integer w, h;
        private final int textureStartX, textureStartY;
        private final boolean hasPlayerInventory;
        private final int playerInventoryX, playerInventoryY;

        private data(ResourceLocation menuId, ResourceLocation texture,
                     int x, int y,
                     Integer w, Integer h,
                     int textureStartX, int textureStartY,
                     boolean hasPlayerInventory,
                     int playerInventoryX, int playerInventoryY) {
            this.menuId = Objects.requireNonNull(menuId, "menuId");
            this.texture = Objects.requireNonNull(texture, "texture");
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.textureStartX = textureStartX;
            this.textureStartY = textureStartY;
            this.hasPlayerInventory = hasPlayerInventory;
            this.playerInventoryX = playerInventoryX;
            this.playerInventoryY = playerInventoryY;
        }

        public ResourceLocation menuId() { return menuId; }
        public ResourceLocation texture() { return texture; }
        public int x() { return x; }
        public int y() { return y; }
        public Integer w() { return w; }
        public Integer h() { return h; }
        public int textureStartX() { return textureStartX; }
        public int textureStartY() { return textureStartY; }
        public boolean hasPlayerInventory() { return hasPlayerInventory; }
        public int playerInventoryX() { return playerInventoryX; }
        public int playerInventoryY() { return playerInventoryY; }

        // EasyMenu: render full texture size, only center offset.
        public static data EasyMenu(ResourceLocation menuId, ResourceLocation texture, int x, int y) {
            return new data(menuId, texture, x, y, null, null, 0, 0, false, 0, 0);
        }

        // EasyMenu with player inventory anchor.
        public static data EasyMenu(ResourceLocation menuId, ResourceLocation texture, int x, int y,
                                    int playerInventoryX, int playerInventoryY) {
            return new data(menuId, texture, x, y, null, null, 0, 0, true, playerInventoryX, playerInventoryY);
        }

        // Menu: configurable render region + texture start point.
        // w/h: if null or <=0, fallback to full texture size.
        public static data Menu(ResourceLocation menuId, ResourceLocation texture,
                                int x, int y,
                                Integer w, Integer h,
                                int textureStartX, int textureStartY) {
            return new data(menuId, texture, x, y, w, h, textureStartX, textureStartY, false, 0, 0);
        }

        // Menu with player inventory anchor.
        public static data Menu(ResourceLocation menuId, ResourceLocation texture,
                                int x, int y,
                                Integer w, Integer h,
                                int textureStartX, int textureStartY,
                                int playerInventoryX, int playerInventoryY) {
            return new data(menuId, texture, x, y, w, h, textureStartX, textureStartY,
                    true, playerInventoryX, playerInventoryY);
        }
    }

    // ===== registry =====
    private static final Map<ResourceLocation, data> REGISTRY = new HashMap<>();

    /** Define with one object (pass params once). */
    public static void define(data data) {
        Objects.requireNonNull(data, "info");
        REGISTRY.put(data.menuId(), data);
    }

    public static data get(ResourceLocation menuId) {
        return REGISTRY.get(menuId);
    }

    public static data require(ResourceLocation menuId) {
        data data = REGISTRY.get(menuId);
        if (data == null) throw new IllegalStateException("NutMenuInfo missing entry for: " + menuId);
        return data;
    }
}
