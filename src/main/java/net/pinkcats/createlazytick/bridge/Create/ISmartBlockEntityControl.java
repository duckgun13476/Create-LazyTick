package net.pinkcats.createlazytick.bridge.Create;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTier;

import java.util.ArrayList;
import java.util.List;

public interface ISmartBlockEntityControl {

    byte createLazyTick$ControlState();
    void createLazyTick$SetForceControl(byte value);

    String createLazyTick$getUserName();
    void createLazyTick$setUserName(String value);

    BlockPos CLT$getPos();
    ResourceKey<Level> CLT$getDimension();

    // --- 状态显示功能 (新增) ---
    // 设置当前的档位 (传入数值，自动计算档位并决定是否发包)
    void lazytick$setSyncedTier(int currentTick, int maxTick);

    // 获取当前档位 (客户端渲染用)
    LazyTickTier lazytick$getSyncedTier();

    // mixin的一般是private,如果需要让Helper使用,需要一个Interface
    void createLazyTick$setLazyTickInterval(int tick);
    int createLazyTick$getLazyTickInterval();

    void createLazyTick$setDelayForced(boolean isForced);
    boolean createLazyTick$isDelayForced();

    default List<Component> createLazyTick$getCustomTooltipInfo() {
        return new ArrayList<>();
    }

    default boolean createLazyTick$shouldRenderTier() {
        return true;
    }

    default boolean createLazyTick$shouldRenderMode() {
        return true;
    }

    // C Request S to do something(Interface)
    default void CLT$onClientRequest(int extraData) {}

    // Server Only
    void lazytick$setExtraData(int data);

    // Client Only
    int lazytick$getExtraData();
}