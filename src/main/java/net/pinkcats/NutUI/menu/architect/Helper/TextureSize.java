package net.pinkcats.NutUI.menu.architect.Helper;


import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.DistExecutor;

public final class TextureSize {
    private TextureSize() {}

    public record Size(int w, int h) {
        public static final Size ZERO = new Size(0, 0);
    }


    public static Size get(ResourceLocation texture) {
        return DistExecutor.unsafeRunForDist(
                () -> () -> TextureSizeClient.get(texture),
                () -> () -> Size.ZERO
        );
    }
}