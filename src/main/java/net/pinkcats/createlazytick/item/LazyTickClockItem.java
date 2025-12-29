package net.pinkcats.createlazytick.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickWhiteList;
import org.jetbrains.annotations.NotNull;

public class LazyTickClockItem extends Item {

    public LazyTickClockItem(Properties properties) {
        super(properties);
    }

    public static final byte StateDirection = 5;

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

            LazyTickWhiteList whiteItem = LazyTickWhiteList.getByEntity(be);

            if (whiteItem == null) {
                return InteractionResult.PASS;
            }

            if (whiteItem == LazyTickWhiteList.PUMP || whiteItem == LazyTickWhiteList.PIPE) {
                player.displayClientMessage(Component.literal("此元件受全局配置控制，不可手动调整")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            byte ControlState = control.createLazyTick$ControlState();

            System.out.println("UseOn:ControlState" + ControlState);
            System.out.println("UseOn:clickPos" + context.getClickedPos());
            System.out.println("UseOn:UserName" + control.createLazyTick$getUserName());

            ControlState ++;
            if (ControlState >= StateDirection)
                ControlState = 0;

            SetControlState(control, ControlState);
            if (ControlState == 0) {
                SetOperatorName(control,"");
            } else {
                SetOperatorName(control, player.getName().getString());
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
    //Tool Func

    private static void SetOperatorName(ISmartBlockEntityControl control, String player) {
        control.createLazyTick$setUserName(player);
    }
    private static void SetControlState(ISmartBlockEntityControl control, byte ControlState) {
        control.createLazyTick$SetForceControl(ControlState);
    }

}
