package net.pinkcats.createlazytick.mixin.OptElement.basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.world.item.crafting.Recipe;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.bridge.Basin.IBasinOptimization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

//老实说这玩意真要重拳出击那也得加缓存,但是缓存有很多风险,只能取下策(版本号缓存,这只能节省掉一些无效的查询,对于有效的查询,卡顿是没法避免的,唯一的优点是稳定)
@Mixin(BasinOperatingBlockEntity.class)
public abstract class BasinOperatingLazyTickMixin {

    @Shadow(remap = false)
    protected abstract Optional<BasinBlockEntity> getBasin();

    // 缓存字段
    @Unique private long cachedBasinVersion = -1;
    @Unique private BlazeBurnerBlock.HeatLevel cachedHeatLevel = BlazeBurnerBlock.HeatLevel.NONE;
    @Unique private List<Recipe<?>> cachedRecipes = null;
    @Unique private BasinBlockEntity cachedBasinRef = null;

    @Inject(method = "getMatchingRecipes", at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetMatchingRecipes(CallbackInfoReturnable<List<Recipe<?>>> cir) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyBasin()) return;

        Optional<BasinBlockEntity> basinOpt = getBasin();

        if (basinOpt.isEmpty()) {
            return;
        }

        BasinBlockEntity basin = basinOpt.get();

        //Ensure cache can create use vanilla
        if (basin.isEmpty()) return;


        // 统一强转为接口
        IBasinOptimization optimizedBasin = (IBasinOptimization) basin;

        // 获取版本号 (通过接口)
        long currentVersion = optimizedBasin.getInventoryVersion();

        // 获取热量 (用接口中的复刻方法)
        BlazeBurnerBlock.HeatLevel currentHeat = optimizedBasin.optimization$getHeatLevel();

        // 如果对象一致、版本一致、热量一致，直接返回缓存
        if (cachedRecipes != null &&
                cachedBasinRef == basin &&
                currentVersion == cachedBasinVersion &&
                currentHeat == cachedHeatLevel) {

            cir.setReturnValue(cachedRecipes);
            cir.cancel();
        }
    }

    @Inject(method = "getMatchingRecipes", at = @At("RETURN"), remap = false)
    private void captureMatchingRecipes(CallbackInfoReturnable<List<Recipe<?>>> cir) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyBasin()) return;

        Optional<BasinBlockEntity> basinOpt = getBasin();
        if (basinOpt.isPresent()) {
            BasinBlockEntity basin = basinOpt.get();
            IBasinOptimization optimizedBasin = (IBasinOptimization) basin;

            // 记录当前状态，供下一次检查使用
            this.cachedBasinRef = basin;
            this.cachedBasinVersion = optimizedBasin.getInventoryVersion();
            this.cachedHeatLevel = optimizedBasin.optimization$getHeatLevel();

            List<Recipe<?>> ret = cir.getReturnValue();
            this.cachedRecipes = (ret == null) ? Collections.emptyList() : ret;
        } else {
            this.cachedRecipes = null;
            this.cachedBasinRef = null;
            this.cachedBasinVersion = -1;
            this.cachedHeatLevel = BlazeBurnerBlock.HeatLevel.NONE;
        }
    }
}