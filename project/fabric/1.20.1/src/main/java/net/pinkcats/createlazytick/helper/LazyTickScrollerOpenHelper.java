package net.pinkcats.createlazytick.helper;

import com.simibubi.create.content.fluids.drain.ItemDrainBlock;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlock;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.logistics.chute.ChuteBlock;
import com.simibubi.create.content.logistics.chute.SmartChuteBlock;
import com.simibubi.create.content.logistics.depot.DepotBlock;
import com.simibubi.create.content.logistics.funnel.AndesiteFunnelBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.BrassFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.pinkcats.NutUI.menu.extensions.NutMenuExtensionRegistry;
import net.pinkcats.createlazytick.Register.LazyTickItem;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;

import static net.pinkcats.NutUI.menu.architect.Helper.MenuHelper.CreateNutMenu;
import static net.pinkcats.createlazytick.Gui.Menu.MenuInit.LazyTickScrollerBase;

public final class LazyTickScrollerOpenHelper {
    private LazyTickScrollerOpenHelper() {
    }

    public static boolean tryOpen(Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return false;
        }
        if (hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        if (player.getMainHandItem().getItem() != LazyTickItem.CLOCK.get()) {
            return false;
        }
        BlockPos targetPos = resolveOpenTargetPos(level, pos, hit);
        Block block = level.getBlockState(targetPos).getBlock();
        if (!isLazyTickScrollerTarget(block)) {
            return false;
        }
        if (!(level.getBlockEntity(targetPos) instanceof ISmartBlockEntityControl)) {
            return false;
        }

        double localY = hit.getLocation().y - targetPos.getY();
        if (localY >= 0.5D) {
            return false;
        }

        CreateNutMenu(player, targetPos, LazyTickScrollerBase, (id, inv, p, menuPos, menuId) ->
                NutMenuExtensionRegistry.createMenu(inv, id, p, menuPos, menuId));
        return true;
    }

    private static BlockPos resolveOpenTargetPos(Level level, BlockPos pos, BlockHitResult hit) {
        Block block = level.getBlockState(pos).getBlock();
        double localY = hit.getLocation().y - pos.getY();
        if (block instanceof DepotBlock && localY >= 0.5D) {
            BlockPos above = pos.above();
            Block aboveBlock = level.getBlockState(above).getBlock();
            if (isFunnelVariant(aboveBlock)) {
                return above;
            }
        }
        return pos;
    }

    public static boolean isLazyTickScrollerTarget(Block block) {
        return block instanceof DepotBlock
                || block instanceof ArmBlock
                || block instanceof ChuteBlock
                || block instanceof SmartChuteBlock
                || block instanceof MechanicalCrafterBlock
                || block instanceof FunnelBlock
                || block instanceof AndesiteFunnelBlock
                || block instanceof BrassFunnelBlock
                || block instanceof BeltFunnelBlock
                || block instanceof ItemDrainBlock
                || block instanceof SawBlock;
    }

    public static boolean isFunnelVariant(Block block) {
        return block instanceof FunnelBlock
                || block instanceof AndesiteFunnelBlock
                || block instanceof BrassFunnelBlock
                || block instanceof BeltFunnelBlock;
    }

}
