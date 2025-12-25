package net.pinkcats.createlazytick.helper.Basin;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

// 新学的,叫做duck typing?
// 这玩意是给BBE和BOBE互通用的,不要删
// 工作盆的逻辑部分BasinOperatingBlockEntity和工作盆方块实体部分BasinBlockEntity分开了,导致注入也需要对应的两个类
// 而这注入的这两个类之间就需要它互通,一个实现一个用
public interface IBasinOptimization {
    long getInventoryVersion();

    /**
     * 暴露热量等级获取逻辑
     * 实现类中会复刻新版Create的缓存+防空逻辑
     */
    BlazeBurnerBlock.HeatLevel optimization$getHeatLevel();
}