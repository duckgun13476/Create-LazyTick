package net.pinkcats.createlazytick.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;

@Mixin(value = BasinOperatingBlockEntity.class,remap = false)
public abstract class BasinLazyTickMixin extends KineticBlockEntity {

    @Shadow
    protected Recipe<?> currentRecipe;

    @Shadow
    protected abstract <C extends Container> boolean matchBasinRecipe(Recipe<C> recipe);

    @Shadow
    public abstract void startProcessingBasin();

    @Shadow
    protected abstract List<Recipe<?>> getMatchingRecipes();

    public BasinLazyTickMixin(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Unique
    int lazyTickCounter = 0;
    @Unique
    int currentDelayTick = 1;
    @Unique
    boolean mistake = false;

    /**
     * 懒加载更新逻辑
     * @param foundRecipe 是否找到了配方
     */
    @Unique
    private void lazyTickUpdate(boolean foundRecipe) {

        if (level != null && level.isClientSide && !isVirtual())
            return;

        // 真正找到配方,重置计数
        if (foundRecipe) {
            currentDelayTick = 1;
            mistake = false;
        } else {
            // 找不到配方(包括空盆/有物品但没配方),都进入退避模式
            if (currentDelayTick < Config.basin_delay_max) {
                if (mistake) {
                    if (currentDelayTick < 10) {
                        currentDelayTick += 1;
                    } else if (currentDelayTick < 30) {
                        currentDelayTick += 2;
                    } else if (currentDelayTick < 60) {
                        currentDelayTick += 3;
                    } else {
                        currentDelayTick += 5;
                    }
                }

                if (currentDelayTick == 1) {
                    mistake = true;
                }
            }
        }
    }

    @Inject(
            method = "updateBasin",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/processing/basin/BasinOperatingBlockEntity;getMatchingRecipes()Ljava/util/List;"
            ),
            cancellable = true,
            remap = false
    )
    private void createLazyTick$updateBasinLazy(CallbackInfoReturnable<Boolean> cir) {
        // 总开关检测
        if (!Config.enable_lazy_tick || !Config.enable_lazy_basin) {
            return;
        }

        // 1. 重载清理
        if (IsServerReload) {
            this.currentRecipe = null;
            return;
        }

        // 2. 如果有缓存命中
        if (this.currentRecipe != null && this.matchBasinRecipe(this.currentRecipe)) {
            lazyTickUpdate(true); // 成功匹配，重置延迟
            createLazyTick$StartAndSync();
            cir.setReturnValue(true); //拦截后续配方调用
            return;
        }

        // 3. 懒加载跳过
        if (currentDelayTick > 1 && lazyTickCounter < currentDelayTick) {
            lazyTickCounter++;
            cir.setReturnValue(true); // 跳过本次检查
            return;
        }

        // 倒计时结束，重置计数器
        lazyTickCounter = 0;

        // 3. 手动接管搜索,以查看是否有返回配方
        // BasinOperatingBlockEntity.getMatchingRecipes() 内部已经处理了空盆判断
        // 如果盆是空的，recipes 也会是 EmptyList，符合“未找到配方”
        List<Recipe<?>> recipes = getMatchingRecipes();


        if (recipes.isEmpty()) {
            // 搜索失败（空盆或无匹配），增加延迟
            lazyTickUpdate(false);
            this.currentRecipe = null;
            cir.setReturnValue(true);
        } else {
            // 搜索成功，重置延迟
            lazyTickUpdate(true);
            this.currentRecipe = recipes.get(0);
            createLazyTick$StartAndSync();
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void createLazyTick$StartAndSync() {
        this.startProcessingBasin();
        this.sendData();
    }


}