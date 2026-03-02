package net.pinkcats.createlazytick.mixin.OptElement;

import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.helper.RecipeCacheTool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;

/**
 * * 主要功能：
 * 1. [极速缓存]：针对普通配方（如打磨、物品应用）进行缓存，大幅降低 Tick 耗时。
 * 2. [状态同步]：缓存命中时手动同步内部容器状态，修复耐久不消耗和部分配方判定失效的问题。
 * 3. [序列封锁]：通过三重防御（类型、序列化器、产物基因）彻底拦截“序列组装”配方，防止单例污染导致的跳步 BUG。
 * 4. [熔断机制]：对检测到的危险配方启用黑名单，后续直接跳过处理，零开销运行。<p>
 * 你知道吗,**的,序列组装这玩意谁存配方谁倒霉,直接给你跳步骤,得防住它的同时把普通配方存了
 */
@Mixin(DeployerBlockEntity.class)
public abstract class DeployerRecipeMixin {

    @Shadow(remap = false)
    public abstract DeployerFakePlayer getPlayer();

    @Shadow(remap = false)
    ItemStackHandler recipeInv;

    // --- 缓存相关字段 ---
    @Unique private boolean lazytick$hasCached = false;
    @Unique private Item lazytick$cachedTargetItem = null; // 缓存的目标物品（传送带上的）
    @Unique private Item lazytick$cachedHeldItem = null;   // 缓存的手持物品（机械手里的）
    @Unique private RecipeHolder<? extends Recipe<? extends RecipeInput>> lazytick$cachedRecipe = null; // 缓存的配方结果
    @Unique private boolean lazytick$isBlacklisted = false; // 黑名单标记（熔断开关）
    @Unique private boolean lazytick$cachedTargetItemHasNbt = false;

    //注入到 getRecipe 方法头部，尝试直接返回缓存的配方。
    @Inject(method = "getRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void lazytick$checkCache(ItemStack stack, CallbackInfoReturnable<RecipeHolder<? extends Recipe<? extends RecipeInput>>> cir) {
        // 配置检查
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableCacheDeployer()) return;

        // 服务器重载时清空缓存
        if (IsServerReload) {
            this.lazytick$clearCache();
        }

        // 1. 基础缓存有效性检查
        if (!lazytick$hasCached) return;

        // 2. 物品变更检查 (这是缓存逻辑的前提)
        if (stack.getItem() != lazytick$cachedTargetItem) return;

        boolean hasTag = !stack.getComponentsPatch().isEmpty();
        if (hasTag != lazytick$cachedTargetItemHasNbt) return;

        // 3. 黑名单检查
        // 如果该物品之前已被判定为导致序列组装的“危险物品”，直接跳过处理，走原版逻辑。
        if (lazytick$isBlacklisted) {
            return;
        }

        // 4. 手持物品一致性检查
        DeployerFakePlayer player = this.getPlayer();
        if (player == null) return;
        ItemStack currentHeld = player.getMainHandItem();

        if (currentHeld.getItem() != lazytick$cachedHeldItem) return;

        // 5. 防止之前的缓存中混入了脏数据
        DeployerBlockEntity be = (DeployerBlockEntity)(Object)this;
        Recipe<?> recipeEntity = lazytick$cachedRecipe == null ? null : lazytick$cachedRecipe.value();
        if (RecipeCacheTool.isDangerousSARecipe(stack, recipeEntity, be.getLevel())) {
            this.lazytick$clearCache();
            return;
        }

        // 6. 状态同步
        // 即使直接返回配方，也必须将物品放入容器，否则 Create 后续逻辑（如砂纸打磨）无法正确扣除耐久。
        if (this.recipeInv != null) {
            this.recipeInv.setStackInSlot(0, stack);
            this.recipeInv.setStackInSlot(1, currentHeld);
        }

        // 7. 缓存命中，直接返回，跳过原版查找
        cir.setReturnValue(lazytick$cachedRecipe);
        cir.cancel();
    }

    /**
     * 注入到 getRecipe 方法尾部，将原版计算出的配方存入缓存。
     */
    @Inject(method = "getRecipe", at = @At("RETURN"), remap = false)
    private void lazytick$saveCache(ItemStack stack, CallbackInfoReturnable<RecipeHolder<? extends Recipe<? extends RecipeInput>>> cir) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableCacheDeployer()) return;

        DeployerFakePlayer player = this.getPlayer();
        if (player == null) return;

        RecipeHolder<? extends Recipe<? extends RecipeInput>> result = cir.getReturnValue();

        // 配方危险性检查
        // 检查输出是否会导致序列组装
        DeployerBlockEntity be = (DeployerBlockEntity)(Object)this;
        Recipe<?> recipeEntity = result == null ? null : result.value();
        boolean recipeIsDangerous = RecipeCacheTool.isDangerousSARecipe(stack, recipeEntity, be.getLevel());

        // --- 黑名单判定 ---
        if (recipeIsDangerous) {
            this.lazytick$hasCached = true;
            this.lazytick$cachedTargetItem = stack.getItem();
            this.lazytick$cachedHeldItem = player.getMainHandItem().getItem();

            this.lazytick$cachedTargetItemHasNbt = !stack.getComponentsPatch().isEmpty();
            this.lazytick$isBlacklisted = true; // 标记为黑名单，下次 checkCache 直接熔断
            this.lazytick$cachedRecipe = null;  // 不缓存危险配方
            return;
        }

        // --- 白名单存入 ---
        this.lazytick$hasCached = true;
        this.lazytick$cachedTargetItem = stack.getItem();
        this.lazytick$cachedHeldItem = player.getMainHandItem().getItem();

        this.lazytick$cachedTargetItemHasNbt = !stack.getComponentsPatch().isEmpty();
        this.lazytick$isBlacklisted = false;
        this.lazytick$cachedRecipe = result;
    }

    @Unique
    private void lazytick$clearCache() {
        this.lazytick$hasCached = false;
        this.lazytick$cachedTargetItem = null;
        this.lazytick$cachedHeldItem = null;
        this.lazytick$cachedRecipe = null;
        this.lazytick$isBlacklisted = false;
        this.lazytick$cachedTargetItemHasNbt = false;
    }
}