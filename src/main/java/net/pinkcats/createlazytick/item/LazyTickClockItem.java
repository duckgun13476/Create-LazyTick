package net.pinkcats.createlazytick.item;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.LazyTickLogic;
import net.pinkcats.createlazytick.helper.LazyTickScrollBehaviour;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickMode;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickWhiteList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
//需要翻译文本
public class LazyTickClockItem extends Item {

    public LazyTickClockItem(Properties properties) {
        super(properties);
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

            /*System.out.println("UseOn:ControlState:" + "FAIL" + "ControlState");
            System.out.println("UseOn:clickPos" + context.getClickedPos());
            System.out.println("UseOn:UserName" + control.createLazyTick$getUserName());*/

            // 2. 获取清洗后的安全序列 (调用内部私有方法，不信任 Config 直接返回的数据)
            List<Integer> sequence = getSafeSequence();

            // 3. 读取配置倾向 (决定是调节 动态上限 还是 强制间隔)
            boolean targetIsDynamic = Config.clock_mode_default_dynamic;

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
            SetOperatorName(control, player.getName().getString());

            // 7. 应用新状态 & 触发逻辑更新
            applyPercentage(control, nextPercentage, targetIsDynamic);
            LazyTickLogic.updateState(control);

            // 8. 反馈消息 & 添加冷却 (0.5秒)
            int maxDelayTick = whiteItem.getMaxTick();
            player.displayClientMessage(Component.literal("懒加载模式已切换: ")
                    .append(LazyTickMode.getDisplayComponent(control.createLazyTick$getDynamicValue(),
                            control.createLazyTick$getForcedValue(),maxDelayTick)), true);

            player.getCooldowns().addCooldown(this, 10);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    //Tool Func
    private static void SetOperatorName(ISmartBlockEntityControl control, String player) {
        control.createLazyTick$setUserName(player);
    }
    private List<Integer> getSafeSequence() {
        // 直接从 ConfigValue 获取原始列表 (带通配符)
        List<? extends Integer> rawList = Config.clock_mode_sequence;
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
