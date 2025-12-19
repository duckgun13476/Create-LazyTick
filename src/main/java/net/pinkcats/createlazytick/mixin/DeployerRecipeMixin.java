package net.pinkcats.createlazytick.mixin;

import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.pinkcats.createlazytick.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;

@Mixin(DeployerBlockEntity.class)
public abstract class DeployerRecipeMixin {

    @Shadow(remap = false)
    public abstract DeployerFakePlayer getPlayer();

    // --- 缓存字段 ---
    @Unique
    private ItemStack lazytick$cachedTargetItem = ItemStack.EMPTY;
    @Unique
    private ItemStack lazytick$cachedHeldItem = ItemStack.EMPTY;
    @Unique
    private Recipe<? extends Container> lazytick$cachedRecipe = null;

    /**
     * 1. 快速失败：如果 Item 类型不同，直接 false
     * 2. 引用检查：如果 NBT 对象地址相同 (或都为 null)，直接 true (0ms 耗时)
     * 3. 兜底逻辑：调用原版 isSameItemSameTags 确保行为 100% 一致 (按照create原版处理方式来)
     */
    @Unique
    private boolean lazytick$fastCheck(ItemStack current, ItemStack cached) {
        if (current.isEmpty() && cached.isEmpty()) return true;
        if (current.isEmpty() || cached.isEmpty()) return false;

        // Level 1: Item 类型检查 (最快)
        if (current.getItem() != cached.getItem()) return false;

        // Level 2: NBT 引用检查 (针对无 NBT 物品或偶发的引用共享)
        // 注意：缓存通常是 copy，所以有 NBT 时这里很难命中，但 null == null 可以命中
        if (current.getTag() == cached.getTag()) return true;

        // Level 3: 原版标准比对 (模拟原版逻辑)
        // 忽略数量差异，只比对 Item 和 Tag 内容
        return ItemStack.isSameItemSameTags(current, cached);
    }

    @Inject(method = "getRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void lazytick$checkCache(ItemStack stack, CallbackInfoReturnable<Recipe<? extends Container>> cir) {
        if (!Config.enable_lazy_tick || !Config.enable_cache_deployer) {
            return;
        }

        if (IsServerReload) {
            this.lazytick$clearCache();
        }

        DeployerFakePlayer player = this.getPlayer();
        if (player == null) return;
        ItemStack currentHeld = player.getMainHandItem();

        // --- 缓存命中检查 ---
        // 只要 目标物品 和 手持物品 都匹配，就跳过 EventBus 直接返回
        if (lazytick$cachedRecipe != null &&
                lazytick$fastCheck(stack, lazytick$cachedTargetItem) &&
                lazytick$fastCheck(currentHeld, lazytick$cachedHeldItem)) {

            cir.setReturnValue(lazytick$cachedRecipe);
            cir.cancel();
        }
    }

    @Inject(method = "getRecipe", at = @At("RETURN"), remap = false)
    private void lazytick$saveCache(ItemStack stack, CallbackInfoReturnable<Recipe<? extends Container>> cir) {
        if (!Config.enable_lazy_tick || !Config.enable_cache_deployer) {
            return;
        }

        DeployerFakePlayer player = this.getPlayer();
        if (player == null) return;
        ItemStack currentHeld = player.getMainHandItem();

        Recipe<? extends Container> result = cir.getReturnValue();

        // 存入缓存
        // 必须使用 .copy(),因为原版逻辑后续可能会修改 stack 的数量或 NBT
        // 如果不 copy,缓存中的对象会跟着变，导致"永远命中"的严重 Bug
        this.lazytick$cachedTargetItem = stack.copy();
        this.lazytick$cachedHeldItem = currentHeld.copy();
        this.lazytick$cachedRecipe = result;
    }

    @Unique
    private void lazytick$clearCache() {
        this.lazytick$cachedTargetItem = ItemStack.EMPTY;
        this.lazytick$cachedHeldItem = ItemStack.EMPTY;
        this.lazytick$cachedRecipe = null;
    }
}

//通常需要注意的场景
//    场景	            缓存对象	       是否需要 .copy()	          原因
//Input (存入时)	       ItemStack	        必须	       输入物品随后会被消耗/修改，必须存快照。
//Input (存入时)	       Item (单例)	       不需要	       Item 是不可变的单例对象。
//Input (存入时)	    Signature (自定义)	否 (前提是...)   前提是 Signature 在构造时提取了数据，而不是持有 ItemStack 引用。
//Output (取出时)	   ItemStack	        必须	        防止外部逻辑修改缓存内的“母版”结果。
//Output (取出时)	   Recipe 对象	       不需要	     Recipe 对象通常是只读的配置数据。