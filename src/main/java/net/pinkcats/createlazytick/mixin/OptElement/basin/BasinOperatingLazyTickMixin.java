package net.pinkcats.createlazytick.mixin.OptElement.basin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.pinkcats.createlazytick.bridge.Basin.BasinRecipeIndex;
import net.pinkcats.createlazytick.bridge.Basin.BasinStateSnapshot;
import net.pinkcats.createlazytick.bridge.Basin.IBasinOptimization;
import net.pinkcats.createlazytick.config.ServerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static net.pinkcats.createlazytick.bridge.Basin.BasinRecipeIndex.isBasinOptimizationSafe;

@Mixin(value = BasinOperatingBlockEntity.class)
public abstract class BasinOperatingLazyTickMixin {

    @Shadow(remap = false) protected abstract Optional<BasinBlockEntity> getBasin();
    @Shadow(remap = false) protected abstract boolean isRunning();

    @Shadow(remap = false) protected Recipe<?> currentRecipe;
    @Shadow(remap = false) public abstract void startProcessingBasin();

    @Unique
    private void clt$sendData() {
        ((SyncedBlockEntity)(Object)this).sendData();
    }

    @Unique private BasinStateSnapshot clt$cachedSnapshot;

    @Unique
    private boolean clt$couldTriggerNewRecipe(BasinStateSnapshot oldFp, BasinStateSnapshot newFp) {
        // 检查输出缓冲区变化
        if (oldFp.isOutputBufferEmpty() != newFp.isOutputBufferEmpty()) return true;
        // 检查热量变化
        if (oldFp.getHeatLevel() != newFp.getHeatLevel()) return true;
        // 检查是否有含NBT物品(且有NBT配方)进入
        if (newFp.hasNbtSensitiveItems()) return true;
        // 对比快照
        if (!ItemStack.matches(oldFp.getFilterSnapshot(), newFp.getFilterSnapshot())) return true;
        // 对比流体
        if (oldFp.getFluidHash() != newFp.getFluidHash()) return true;

        Object2IntMap<Item> oldItems = oldFp.getItemQuantities();
        Object2IntMap<Item> newItems = newFp.getItemQuantities();

        for (Object2IntMap.Entry<Item> entry : newItems.object2IntEntrySet()) {
            Item item = entry.getKey();
            int newQty = entry.getIntValue();
            int oldQty = oldItems.getInt(item);

            // 如果增加超过阈值,唤醒
            if (newQty > oldQty) {
                IntSet thresholds = BasinRecipeIndex.getThresholds(item);
                boolean crossedThreshold = false;
                for (int threshold : thresholds) {
                    if (oldQty < threshold && newQty >= threshold) {
                        crossedThreshold = true;
                        break;
                    }
                }
                if (crossedThreshold) return true;
            } else if (newQty < oldQty) {
                return true; // 物品少了也醒
            }
        }
        return false;
    }

    @Inject(remap = false, method = "updateBasin", at = @At("HEAD"), cancellable = true)
    private void clt$onUpdateBasin(CallbackInfoReturnable<Boolean> cir) {
        if (!ServerConfig.getEnableLazyTick()
                || !ServerConfig.getEnableLazyBasin()
                || !isBasinOptimizationSafe) return;

        // 1. 正在加工中,不干预
        if (isRunning()) return;

        Optional<BasinBlockEntity> basinOpt = getBasin();
        if (basinOpt.isEmpty()) return;
        BasinBlockEntity basin = basinOpt.get();

        // 2. 有东西要排,且为排出模式,但输出阻塞,则交给原版(尝试排出)
        if (!((IBasinOptimization) basin).clt$isOutputBufferEmpty()) {
            return;
        }

        //System.out.println("[" + basin.getBlockPos() + "] " + currentRecipe);
        //System.out.println(currentRecipe);

        // 3. 先复用老配方
        // ignore useless-check warning
        if (currentRecipe != null && BasinRecipe.match(basin, currentRecipe)) {
            // 复用老配方成功,直接启动处理并返回
            //System.out.println("ok");
            startProcessingBasin();
            clt$sendData();
            cir.setReturnValue(true); // 跳过原方法
            return;
        }
        //System.out.println("FAIL");

        // 4. 创建快照,并对比快照状态
        BasinStateSnapshot currentSnapshot = new BasinStateSnapshot(basin);

        if (this.clt$cachedSnapshot != null) {
            if (this.clt$cachedSnapshot.equals(currentSnapshot)) {
                //System.out.println("[BOBE-DEBUG] SLEEPING (Snapshots Match). OutHash: " + currentSnapshot.isOutputBufferEmpty());
                cir.setReturnValue(true); // 休眠
                return;
            }

            // 检查是否需要唤醒 (策略预判)
            boolean shouldWake = clt$couldTriggerNewRecipe(this.clt$cachedSnapshot, currentSnapshot);

            if (!shouldWake) {
                //System.out.println("[BOBE-DEBUG] SLEEPING (Prediction said No). \n   Old: " + clt$cachedSnapshot + "\n   New: " + currentSnapshot);
                this.clt$cachedSnapshot = currentSnapshot;
                cir.setReturnValue(true);
                return;
            }

            // [DEBUG] 正常唤醒
            //System.out.println("[BOBE-DEBUG] WAKING UP! \n   Old: " + clt$cachedSnapshot + "\n   New: " + currentSnapshot);

        }
        //System.out.println("[BOBE-DEBUG] First Run / Reset.");


        // 允许执行，更新缓存
        this.clt$cachedSnapshot = currentSnapshot;
    }


    @Inject(method = "getMatchingRecipes", at = @At("HEAD"), cancellable = true, remap = false)
    private void clt$onGetMatchingRecipes(CallbackInfoReturnable<List<Recipe<?>>> cir) {
        if (!ServerConfig.getEnableLazyTick()
                || !ServerConfig.getEnableLazyBasin()
                || !isBasinOptimizationSafe) return;

        if (currentRecipe == null) return;

        Optional<BasinBlockEntity> basinOpt = getBasin();
        if (basinOpt.isEmpty()) return;
        BasinBlockEntity basin = basinOpt.get();

        // 尝试复用老配方
        if (BasinRecipe.match(basin, currentRecipe)) {
            // 直接返回当前配方
            cir.setReturnValue(Collections.singletonList(currentRecipe));
        }
    }
}