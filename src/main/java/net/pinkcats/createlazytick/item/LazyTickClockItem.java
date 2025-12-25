package net.pinkcats.createlazytick.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import org.jetbrains.annotations.NotNull;

public class LazyTickClockItem extends Item {

    public LazyTickClockItem(Properties properties) {
        super(properties);
    }

    public static final byte StateDirection = 4;

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();
        Player player = context.getPlayer();

        //Is Server Logic
        if (level.isClientSide || player == null)
            return InteractionResult.SUCCESS;


        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof ISmartBlockEntityControl control) {

            byte ControlState = control.createLazyTick$ControlState();

            System.out.println(ControlState);
            System.out.println(context.getClickedPos());
            System.out.println(control.createLazyTick$getUserName());



            ControlState ++;
            if (ControlState >= StateDirection)
                ControlState = 0;

            SetControlState(control, ControlState);
            SetOperatorName(control, player);


        }

        return InteractionResult.PASS;
    }




    //Tool Func

    private static void SetOperatorName(ISmartBlockEntityControl control, Player player) {
        control.createLazyTick$setUserName(player.getName().getString());
    }
    private static void SetControlState(ISmartBlockEntityControl control, byte ControlState) {
        control.createLazyTick$SetForceControl(ControlState);
    }

}
