package net.pinkcats.NutUI.menu.architect.Helper;


import net.minecraft.resources.ResourceLocation;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public final class TextureSize {
    private TextureSize() {}

    public record Size(int w, int h) {
        public static final Size ZERO = new Size(0, 0);
    }


    public static Size get(ResourceLocation texture) {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
                ? TextureSizeClient.get(texture) : Size.ZERO;
    }
}
