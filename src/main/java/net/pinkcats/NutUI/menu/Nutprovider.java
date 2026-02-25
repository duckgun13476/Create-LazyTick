package net.pinkcats.NutUI.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.pinkcats.NutUI.menu.Connect.Channel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Nutprovider implements MenuProvider {

    private final BlockPos blockPos;
    private final ResourceLocation menuID;


    /**
     * Used for provide menu
     */
    public Nutprovider(BlockPos pos, ResourceLocation menuID) {
        blockPos = pos;
        this.menuID = menuID;
    }


    @Override
    public Component getDisplayName() {
        return  Component.literal("Name");
    }


    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int id, @NotNull Inventory inv, @NotNull Player player) {
        NutKineticMenu.NutItemMenu menu = new NutKineticMenu.NutItemMenu(inv,id,blockPos,menuID);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            Channel.syncOpenedMenuNow(serverPlayer, menu);
        }
        return menu;   //player.getMainHandItem()   Using in Remote future
    }



}
