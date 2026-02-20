package net.pinkcats.NutUI.menu.architect;

import net.minecraft.resources.ResourceLocation;

import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

public class ResourceParse {

    public static ResourceLocation LoadTexture(String path) {
        return ResourceLocation.parse(path);
    }

    public static ResourceLocation LoadTextureN(String Path){
        return ResourceLocation.fromNamespaceAndPath(MODID,Path);
    }

    public static ResourceLocation BuildDefineFromNameSpace(String NameSpace, String Path){
        return ResourceLocation.fromNamespaceAndPath(NameSpace,Path);
    }


}
