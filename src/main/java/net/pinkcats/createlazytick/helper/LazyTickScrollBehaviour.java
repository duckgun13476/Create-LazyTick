package net.pinkcats.createlazytick.helper;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.*;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickWhiteList;

import java.util.List;

public class LazyTickScrollBehaviour extends ScrollValueBehaviour {

    public LazyTickScrollBehaviour(Component label, SmartBlockEntity be) {
        super(label, be, new InvisibleSlot());
    }

    public static void addTo(SmartBlockEntity be, List<BlockEntityBehaviour> behaviours) {
        // 1. Check safety
        if (!(be instanceof ISmartBlockEntityControl control)) {
            return;
        }

        LazyTickWhiteList whiteItem = LazyTickWhiteList.getByEntity(be);
        if (whiteItem == null) {
            return;
        }

        // 2. new ScrollBehaviour
        LazyTickScrollBehaviour behaviour = new LazyTickScrollBehaviour(Component.literal("配置"), be);

        // 3. Set range (-100 ~ 100)
        behaviour.between(-100, 100);

        // 4.(Client-only) Judge item
        if (CreateLazyTick.isClient()) {
            // server side cannot reach here -> client-only class won't be loaded
            ClientSetup.configureActiveCondition(behaviour);
        }

        // 5. Set callback
        behaviour.withCallback(i -> {
            if (i > 0) {
                // 正数:动态调控 (Dynamic)(上限不断提高,直到动态调控的上限(动态调控上限为100%时则为配置项上限))
                control.createLazyTick$setDynamicValue(i);
                control.createLazyTick$setForcedValue(-1);
            } else if (i < 0) {
                // 负数:强制调控 (Forced)(必须间隔多少时间才会运行一次原逻辑)
                control.createLazyTick$setForcedValue(Math.abs(i));
                control.createLazyTick$setDynamicValue(-1);
            } else {
                // 0:强制活跃 (关闭优化)
                control.createLazyTick$setDynamicValue(-1);
                control.createLazyTick$setForcedValue(-1);
            }
        });

        // 6. Init ui renderer(value) (value of NBT/Memory -> UI)
        int dynamicValue = control.createLazyTick$getDynamicValue();
        int forcedValue = control.createLazyTick$getForcedValue();

        if (dynamicValue > 0) {
            behaviour.setValue(dynamicValue);
        } else if (forcedValue > 0) {
            behaviour.setValue(-forcedValue);
        } else {
            behaviour.setValue(0);
        }

        // 7. add to behaviour list
        behaviours.add(behaviour);
    }

    // Prevent server from loading client class
    // (加载大类时,小类如果未被使用则不加载(Java class lazy loading))
    private static class ClientSetup {
        static void configureActiveCondition(LazyTickScrollBehaviour behaviour) {
            behaviour.onlyActiveWhen(() -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                Player player = mc.player;
                return player != null && player.getMainHandItem().getItem() == CreateLazyTick.CLOCK.get();
            });
        }
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        // total row number >= 2 =>Multi-line
        return new ValueSettingsBoard(
                label,
                100,
                10,
                ImmutableList.of(
                        Component.literal("动态控制"), // Row 0
                        Component.literal("强制控制")  // Row 1
                ),
                new ValueSettingsFormatter(this::formatDualSettings)
        );
    }

    public MutableComponent formatDualSettings(ValueSettingsBehaviour.ValueSettings settings) {
        if (settings.value() == 0) return Component.literal("强制活跃");
        return Component.literal(settings.value() + "%");
    }

    // Read
    @Override
    public ValueSettings getValueSettings() {
        // if value >= 0 -> Row 0(动态)
        // if value < 0 -> Row 1(强制)，value用绝对值
        int row = value >= 0 ? 0 : 1;
        int displayValue = Math.abs(value);
        return new ValueSettings(row, displayValue);
    }

    // Write
    @Override
    public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
        int newValue = settings.value();

        // if control Row 1 (Forced) -> write as negative number
        if (settings.row() == 1) {
            newValue = -newValue;
        }

        // if control Row 0 (Dynamic) -> write as positive number
        // -> newValue = newValue

        // 这一步会自动处理互斥
        setValue(newValue);
        playFeedbackSound(this);
    }

    @Override
    public boolean testHit(Vec3 hit) {
        if (!isActive()) return false;
        Vec3 localHit = hit.subtract(Vec3.atLowerCornerOf(blockEntity.getBlockPos()));
        boolean insideBlock = localHit.x >= 0 && localHit.x <= 1 &&
                localHit.y >= 0 && localHit.y <= 1 &&
                localHit.z >= 0 && localHit.z <= 1;
        if (!insideBlock) return false;
        return localHit.y < 0.3;
        }

    // make value box disappear
    private static class InvisibleSlot extends ValueBoxTransform.Sided {
        @Override protected Vec3 getSouthLocation() { return Vec3.ZERO; }
        @Override protected boolean isSideActive(BlockState s, Direction d) { return true; }
        @Override public float getScale() { return 0f; }
    }
}