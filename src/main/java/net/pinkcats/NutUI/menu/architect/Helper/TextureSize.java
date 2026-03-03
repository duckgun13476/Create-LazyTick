package net.pinkcats.NutUI.menu.architect.Helper;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public final class TextureSize {
    private TextureSize() {}

    public record Size(int w, int h) {
        public static final Size ZERO = new Size(0, 0);
    }

    public static Size get(ResourceLocation texture) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return TextureSizeClient.get(texture);
        }
        return Size.ZERO;
    }
}