package net.pinkcats.NutUI.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.pinkcats.NutUI.menu.architect.NutItemMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Nutprovider implements MenuProvider {

    private final BlockPos blockPos;


    /**
     * Used for provide menu
     */
    public Nutprovider(BlockPos pos) {
        blockPos = pos;
    }


    @Override
    public Component getDisplayName() {
        return  Component.literal("Name");
    }


    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int id, @NotNull Inventory inv, @NotNull Player player) {
        return new NutItemMenu(inv,id,blockPos);   //player.getMainHandItem()   Using in Remote future
    }

}
