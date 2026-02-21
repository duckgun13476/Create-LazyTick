package net.pinkcats.NutUI.menu.architect.data;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class NutMenuInfo {

    private NutMenuInfo() {}

    // ===== One object contains all params (pass once) =====
    public static final class Info {
        private final ResourceLocation menuId;
        private final ResourceLocation texture;
        private final int x, y, w, h;

        private Info(ResourceLocation menuId, ResourceLocation texture, int x, int y, int w, int h) {
            this.menuId = Objects.requireNonNull(menuId, "menuId");
            this.texture = Objects.requireNonNull(texture, "texture");
            this.x = x; this.y = y; this.w = w; this.h = h;
        }

        public ResourceLocation menuId() { return menuId; }
        public ResourceLocation texture() { return texture; }
        public int x() { return x; }
        public int y() { return y; }
        public int w() { return w; }
        public int h() { return h; }

        // Factory method: create once, pass once
        public static Info of(ResourceLocation menuId, ResourceLocation texture, int x, int y, int w, int h) {
            return new Info(menuId, texture, x, y, w, h);
        }
    }

    // ===== registry =====
    private static final Map<ResourceLocation, Info> REGISTRY = new HashMap<>();

    /** Define with one object (pass params once). */
    public static void define(Info info) {
        Objects.requireNonNull(info, "info");
        REGISTRY.put(info.menuId(), info);
    }

    public static Info get(ResourceLocation menuId) {
        return REGISTRY.get(menuId);
    }

    public static Info require(ResourceLocation menuId) {
        Info info = REGISTRY.get(menuId);
        if (info == null) throw new IllegalStateException("NutMenuInfo missing entry for: " + menuId);
        return info;
    }
}