package net.pinkcats.NutUI.menu.architect.Helper;


import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class TextureSizeClient {
    private TextureSizeClient() {}

    private static final Map<ResourceLocation, TextureSize.Size> CACHE = new HashMap<>();

    static TextureSize.Size get(ResourceLocation texture) {
        TextureSize.Size cached = CACHE.get(texture);
        if (cached != null) return cached;

        TextureSize.Size size = read(texture).orElse(TextureSize.Size.ZERO);
        CACHE.put(texture, size);
        return size;
    }

    private static Optional<TextureSize.Size> read(ResourceLocation texture) {
        try {
            var rm = Minecraft.getInstance().getResourceManager();
            Optional<Resource> resOpt = rm.getResource(texture);
            if (resOpt.isEmpty()) return Optional.empty();

            try (InputStream in = resOpt.get().open();
                 NativeImage img = NativeImage.read(in)) {
                return Optional.of(new TextureSize.Size(img.getWidth(), img.getHeight()));
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}