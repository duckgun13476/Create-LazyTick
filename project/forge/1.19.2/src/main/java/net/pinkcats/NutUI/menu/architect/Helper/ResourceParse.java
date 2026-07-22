package net.pinkcats.NutUI.menu.architect.Helper;

import net.minecraft.resources.ResourceLocation;

import static net.pinkcats.NutUI.menu.architect.Helper.DefineHelper.safeDefine;
import static net.pinkcats.createlazytick.CreateLazyTick.MODID;

public class ResourceParse {

    public static ResourceLocation LoadTexture(String path) {
        return new ResourceLocation(path);
    }

    public static ResourceLocation LoadTextureN(String Path){
        return new ResourceLocation(MODID,Path);
    }

    public static ResourceLocation BuildDefine(String NameSpace, String Path){
        return new ResourceLocation(NameSpace,Path);
    }


    //Define
    public static ResourceLocation Nut_Menu_ID(String path) {
        return safeDefine(MODID, path, "Menu_ID");
    }

    public static ResourceLocation Nut_Texture(String path) {
        return safeDefine(MODID, path, "Texture_ID");
    }


}
