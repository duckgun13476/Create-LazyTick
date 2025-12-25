package net.pinkcats.createlazytick.bridge.Create;

import net.pinkcats.createlazytick.helper.LazyTickTier;

public interface ISmartBlockEntityControl {

    byte createLazyTick$ControlState();
    void createLazyTick$SetForceControl(byte value);

    String createLazyTick$getUserName();
    void createLazyTick$setUserName(String value);

    int CLT$getMaxTicks();
    void CLT$setMaxTicks(int value);


    // --- 状态显示功能 (新增) ---
    // 设置当前的档位 (传入数值，自动计算档位并决定是否发包)
    void lazytick$setSyncedTier(int currentTick, int maxTick);

    // 获取当前档位 (客户端渲染用)
    LazyTickTier lazytick$getSyncedTier();

}