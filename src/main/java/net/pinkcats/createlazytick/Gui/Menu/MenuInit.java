package net.pinkcats.createlazytick.Gui.Menu;

import net.minecraft.resources.ResourceLocation;
import net.pinkcats.NutUI.menu.architect.data.NutMenuInfo;

import static net.pinkcats.NutUI.menu.architect.Helper.ResourceParse.*;

public final class MenuInit {
    private MenuInit() {}


    public static final ResourceLocation LazyTickMenu = Nut_Menu_ID("transfer_menu");
    public static final ResourceLocation LazyTickMenuScroller = Nut_Menu_ID("LazyTickScroller");

    public static void init() {


        //lazytick menu
        NutMenuInfo.define(NutMenuInfo.Info.of(
                LazyTickMenu,
                Nut_Texture("gui/transfer_menu.png"),
                0, 0, 176, 166
        ));


        //lazytick scroller
        NutMenuInfo.define(NutMenuInfo.Info.of(
                LazyTickMenuScroller,
                Nut_Texture("gui/scroller.png"),
                0, 0, 176, 166
        ));



    }



}