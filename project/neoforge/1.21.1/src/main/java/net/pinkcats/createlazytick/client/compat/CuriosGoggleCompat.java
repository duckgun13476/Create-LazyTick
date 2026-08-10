package net.pinkcats.createlazytick.client.compat;

import com.simibubi.create.AllItems;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Optional Curios integration. Its API is used only after Curios has been
 * confirmed as loaded, and linkage mismatches fail closed.
 */
public final class CuriosGoggleCompat {
    private static final String CURIOS_MOD_ID = "curios";

    private CuriosGoggleCompat() {
    }

    public static boolean isWearingGoggles(Player player) {
        if (player == null || !ModList.get().isLoaded(CURIOS_MOD_ID)) {
            return false;
        }

        try {
            return CuriosApi.getCuriosInventory(player)
                    .flatMap(handler -> handler.findFirstCurio(AllItems.GOGGLES::isIn))
                    .isPresent();
        } catch (LinkageError ignored) {
            // A mismatched Curios runtime must not prevent the client from starting.
            return false;
        }
    }
}
