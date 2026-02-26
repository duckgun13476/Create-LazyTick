package net.pinkcats.createlazytick.item;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.pinkcats.NutUI.menu.Nutprovider;
import net.pinkcats.createlazytick.Gui.mes;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.util.LazyTickLogic;
import net.pinkcats.createlazytick.helper.LazyTickScrollBehaviour;
import net.pinkcats.createlazytick.helper.LazyTickScrollerOpenHelper;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickMode;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTooltipWhiteList;
import net.pinkcats.createlazytick.manager.ForcedActiveManager;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.pinkcats.NutUI.menu.architect.Helper.MenuHelper.CreateNutMenu;
import static net.pinkcats.NutUI.menu.architect.Helper.ResourceParse.Nut_Menu_ID;
import static net.pinkcats.createlazytick.Gui.Menu.MenuInit.LazyTickMenu;
import static net.pinkcats.createlazytick.Gui.Menu.MenuInit.LazyTickMenuScroller;

//需要翻译文本
public class LazyTickClockItem extends Item {

    public LazyTickClockItem(Properties properties) {
        super(properties);
    }


    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.createlazytick.clock.tooltip.line1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.createlazytick.clock.tooltip.line2")
                .withStyle(ChatFormatting.GRAY));
    }


    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();
        Player player = context.getPlayer();

        //Server Logic Only
        if (level.isClientSide || player == null)
            return InteractionResult.SUCCESS;


        BlockPos pos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);
        double localY = context.getClickLocation().y - pos.getY();

        BlockHitResult hitResult = new BlockHitResult(context.getClickLocation(), context.getClickedFace(), pos, false);
        if (LazyTickScrollerOpenHelper.tryOpen(level, pos, player, context.getHand(), hitResult)) {
            return InteractionResult.CONSUME;
        }

        // Lower half is reserved for the custom Depot UI path.
        // Original clock interaction should only work on upper half.

        if (localY < 0.5D) {
            return InteractionResult.PASS;
        }

        //if (be != null){
        //    CreateNutMenu(player,pos,WhatIsThis);

            //CreateNutMenu(player,pos,LazyTickMenu);
        //    return InteractionResult.CONSUME;
        //} else if (be == null) {
        //    CreateNutMenu(player,pos,LazyTickMenuScroller);
        //    return InteractionResult.CONSUME;
        //}

        if (be instanceof ISmartBlockEntityControl control) {

            LazyTickTooltipWhiteList whiteItem = LazyTickTooltipWhiteList.getByEntity(be);

            if (whiteItem == null) {
                return InteractionResult.PASS;
            }

            if (whiteItem == LazyTickTooltipWhiteList.PUMP ||
                    whiteItem == LazyTickTooltipWhiteList.PIPE ||
                    whiteItem == LazyTickTooltipWhiteList.BELT) {
                player.displayClientMessage(Component.translatable("createlazytick.clock.global_config_locked")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            /*System.out.println("UseOn:ControlState:" + "FAIL" + "ControlState");
            System.out.println("UseOn:clickPos" + context.getClickedPos());
            System.out.println("UseOn:UserName" + control.createLazyTick$getUserName());*/

            if (!ForcedActiveManager.canPlayerActivate(be, player)) {
                return InteractionResult.FAIL;
            }

            // 2. 获取清洗后的安全序列 (调用内部私有方法，不信任 ServerConfig 直接返回的数据)
            List<Integer> sequence = getSafeSequence();

            // 3. 读取配置倾向 (决定是调节 动态上限 还是 强制间隔)
            boolean targetIsDynamic = ServerConfig.getClockModeDefaultDynamic();

            // 4. 获取机器当前百分比 & 检查模式是否错位
            //    错位定义：想调动态但机器是强制，或想调强制但机器是动态
            int currentPercentage = getCurrentPercentage(control);
            boolean isMachineForced = (control.createLazyTick$getForcedValue() > 0);

            boolean typeMismatch = false;
            if (currentPercentage != 0) { // 0% (全速) 是公共起点，不算错位
                if (targetIsDynamic && isMachineForced) typeMismatch = true;
                if (!targetIsDynamic && !isMachineForced) typeMismatch = true;
            }

            // 5. 计算下一档
            int nextPercentage;
            if (typeMismatch) {
                // 如果模式错位 -> 强制归位到列表起点
                nextPercentage = sequence.get(0);
            } else {
                // 模式正确 -> 寻找下一个更大的值 (吸附/循环)
                nextPercentage = getNextPercentage(sequence, currentPercentage);
            }

            // 6. 登记操作者
            control.createLazyTick$setOwnerName(player.getName().getString());
            control.createLazyTick$setOwnerUUID(player.getUUID());

            // 7. 应用新状态 & 触发逻辑更新
            applyPercentage(control, nextPercentage, targetIsDynamic);
            LazyTickLogic.updateState(control);

            // 8. 反馈消息 & 添加冷却 (0.5秒)
            int maxDelayTick = whiteItem.getMaxTick();
            MutableComponent message = Component.translatable("createlazytick.clock.mode_changed");

            List<Component> infoList = LazyTickMode.getDisplayComponents(
                    control.createLazyTick$getDynamicValue(),
                    control.createLazyTick$getForcedValue(),
                    maxDelayTick
            );
            // 遍历列表，将它们依次拼接到消息后面
            for (Component c : infoList) {
                message.append(c);
            }

            player.displayClientMessage(message, true);

            player.getCooldowns().addCooldown(this, 10);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    //Tool Func

    private List<Integer> getSafeSequence() {
        // 直接从 ConfigValue 获取原始列表 (带通配符)
        List<? extends Integer> rawList = ServerConfig.getClockModeSequence();
        List<Integer> safeList = new ArrayList<>();

        // 过滤合法值 (0-100)
        if (rawList != null) {
            for (Integer val : rawList) {
                if (val >= 0 && val <= 100) {
                    safeList.add(val);
                }
            }
        }

        // 排序 (从小到大)
        Collections.sort(safeList);

        // 保底 (如果为空，塞个 0 进去)
        if (safeList.isEmpty()) {
            safeList.add(0);
        }

        return safeList;
    }

    /**
     * 获取机器当前的生效百分比 (0-100)
     */
    private int getCurrentPercentage(ISmartBlockEntityControl control) {
        int frc = control.createLazyTick$getForcedValue();
        if (frc != -1) return frc; // 强制模式值
        return control.createLazyTick$getDynamicValue(); // 动态模式值 // 全速/关闭优化
    }

    /**
     * 寻找列表中的下一个值 (吸附/循环)
     */
    private int getNextPercentage(List<Integer> sequence, int current) {
        for (Integer val : sequence) {
            if (val > current) return val;
        }
        // 没找到更大的 -> 回到起点 (Wrap around)
        return sequence.get(0);
    }

    /**
     * 将百分比应用到机器
     */
    private void applyPercentage(ISmartBlockEntityControl control, int percent, boolean isDynamicMode) {
        if (percent == 0) {
            // 0% -> 全速 (关闭优化)
            LazyTickLogic.switchMode(control, true, 0);
        } else if (isDynamicMode) {
            // 动态模式
            LazyTickLogic.switchMode(control, false, percent);
        } else {
            // 强制模式
            LazyTickLogic.switchMode(control, true, percent);
        }

        // 2. 更新 UI
        if (control instanceof SmartBlockEntity be) {
            LazyTickScrollBehaviour behaviour = LazyTickLogic.getBehaviour(be, LazyTickScrollBehaviour.class);

            if (behaviour != null) {
                // 根据刚才的操作计算 UI 应该显示的值
                // 逻辑与 ScrollBehaviour 初始化时一致
                int targetUiValue;
                if (percent == 0) {
                    targetUiValue = 0; // 0 = 强制活跃/全速
                } else if (isDynamicMode) {
                    targetUiValue = percent; // 正数 = 动态
                } else {
                    targetUiValue = -percent; // 负数 = 强制
                }
                behaviour.setValue(targetUiValue);
            }
        }
    }
}
