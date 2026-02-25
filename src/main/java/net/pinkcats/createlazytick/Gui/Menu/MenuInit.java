package net.pinkcats.createlazytick.Gui.Menu;

import net.minecraft.resources.ResourceLocation;
import net.pinkcats.NutUI.menu.architect.data.NutMenuInfo;

import static net.pinkcats.NutUI.menu.architect.Helper.ResourceParse.*;

public final class MenuInit {
    private MenuInit() {}


    public static final ResourceLocation LazyTickMenu = Nut_Menu_ID("transfer_menu");
    public static final ResourceLocation LazyTickMenuScroller = Nut_Menu_ID("LazyTickScroller");
    public static final ResourceLocation DemoRegionMenu = Nut_Menu_ID("demo_region_menu");

    public static final ResourceLocation WhatIsThis = Nut_Menu_ID("whatisthis");


    public static void init() {


        //lazytick menu

        //This definition additionally has a user inventory render
        NutMenuInfo.define(NutMenuInfo.data.EasyMenu(
                LazyTickMenu,
                Nut_Texture("gui/transfer_menu.png"),
                0, 0,
                0, 85 + 35
        ));// (>·<)


        //lazytick scroller
        NutMenuInfo.define(NutMenuInfo.data.EasyMenu(
                LazyTickMenuScroller,
                Nut_Texture("gui/scroller.png"),
                0, 0
        ));

        //what is this
        NutMenuInfo.define(NutMenuInfo.data.EasyMenu(
                WhatIsThis,
                Nut_Texture("gui/whatisthis.png"),
                0,0,
                0,85
        ));












        /// /////////////////////Examples
        // Menu factory sample:
        // - x/y: offset from screen center
        // - w/h: null or <=0 means use full texture size
        // - textureStartX/Y: render start point inside the texture
        // This is a definition sample only and is intentionally not registered via NutMenuInfo.define(...)
        NutMenuInfo.data demoRegionMenuDefinition = NutMenuInfo.data.Menu(
                DemoRegionMenu,
                Nut_Texture("gui/scroller.png"),
                0, 0,
                120, 80,
                16, 32,
                0, 85 + 35
        );


    }



}
