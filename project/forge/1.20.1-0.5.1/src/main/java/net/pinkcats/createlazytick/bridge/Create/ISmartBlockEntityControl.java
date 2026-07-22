package net.pinkcats.createlazytick.bridge.Create;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.pinkcats.createlazytick.helper.tooltip.LazyTickTier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface ISmartBlockEntityControl {

    // 渲染/命令补全系统调用
    String createLazyTick$getOwnerName();
    void createLazyTick$setOwnerName(String value);

    // 权限系统调用
    UUID createLazyTick$getOwnerUUID();
    void createLazyTick$setOwnerUUID(UUID uuid);

    BlockPos CLT$getPos();
    ResourceKey<Level> CLT$getDimension();

    // --- 状态显示功能 (新增) ---
    // 设置当前的档位 (传入数值，自动计算档位并决定是否发包)
    void lazytick$setSyncedTier(int currentTick, int maxTick);

    // 获取当前档位 (客户端渲染用)
    LazyTickTier lazytick$getSyncedTier();

    // 是否为默认状态
    boolean lazytick$isDefaultState();

    // 动态控制百分比 (Dynamic)
    // 默认为 100(完全开启优化)
    default int createLazyTick$getDynamicValue() { return 100; }
    default void createLazyTick$setDynamicValue(int value) {}

    // 强制控制百分比 (Forced)
    // 默认为 -1 (关闭)
    default int createLazyTick$getForcedValue() { return -1; }
    default void createLazyTick$setForcedValue(int value) {}

    void createLazyTick$setDelayForced(boolean isForced);
    boolean createLazyTick$isDelayForced();

    // mixin的一般是private,如果需要让Helper使用,需要一个Interface
    void createLazyTick$setCurrentSuperTick(int tick);
    int createLazyTick$getCurrentSuperTick();

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

    // 纯净发包接口 (Server Only)
    void createLazyTick$sendBlockUpdated();

    boolean CLT$IsController();
}