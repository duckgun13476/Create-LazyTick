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

    @FunctionalInterface
    public interface MenuBuilder {
        AbstractContainerMenu create(int id, @NotNull Inventory inv, @NotNull Player player, BlockPos pos, ResourceLocation menuId);
    }

    private final BlockPos blockPos;
    private final ResourceLocation menuID;
    private final MenuBuilder menuBuilder;


    /**
     * Used for provide menu
     */
    public Nutprovider(BlockPos pos, ResourceLocation menuID) {
        this(pos, menuID, null);
    }

    public Nutprovider(BlockPos pos, ResourceLocation menuID, @Nullable MenuBuilder menuBuilder) {
        blockPos = pos;
        this.menuID = menuID;
        this.menuBuilder = menuBuilder;
    }


    @Override
    public Component getDisplayName() {
        return  Component.literal("Name");
    }


    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int id, @NotNull Inventory inv, @NotNull Player player) {
        AbstractContainerMenu menu = menuBuilder != null
                ? menuBuilder.create(id, inv, player, blockPos, menuID)
                : new NutKineticMenu.NutItemMenu(inv, id, blockPos, menuID);

        if (menu instanceof NutKineticMenu.NutItemMenu nutMenu
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            Channel.syncOpenedMenuNow(serverPlayer, nutMenu);
        }
        return menu;   //player.getMainHandItem()   Using in Remote future
    }



}
