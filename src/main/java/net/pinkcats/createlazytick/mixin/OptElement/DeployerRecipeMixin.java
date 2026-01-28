package net.pinkcats.createlazytick.mixin.OptElement;

import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe.SandPaperInv;
import com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.pinkcats.createlazytick.CreateLazyTick.DropResourceLocation;
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
    RecipeWrapper recipeInv;

    @Shadow(remap = false)
    SandPaperInv sandpaperInv;

    // --- 缓存相关字段 ---
    @Unique private boolean lazytick$hasCached = false;
    @Unique private Item lazytick$cachedTargetItem = null; // 缓存的目标物品（传送带上的）
    @Unique private Item lazytick$cachedHeldItem = null;   // 缓存的手持物品（机械手里的）
    @Unique private Recipe<? extends Container> lazytick$cachedRecipe = null; // 缓存的配方结果
    @Unique private boolean lazytick$isBlacklisted = false; // 黑名单标记（熔断开关）

    // 预定义 Create 序列组装的注册名常量，用于快速比对
    @Unique
    private static final ResourceLocation CREATE_SEQUENCED_ASSEMBLY = DropResourceLocation("create", "sequenced_assembly");

    /**
     * 判断配方是否为“危险”的序列组装配方。
     * 序列组装配方通常是单例且有状态的，缓存它们会导致不同机械手之间数据污染（跳步）。
     * * @param recipe 待检查的配方
     * @return true 表示危险（不可缓存），false 表示安全（可缓存）
     */
    @Unique
    private boolean lazytick$isDangerousRecipe(Recipe<?> recipe) {
        if (recipe == null) return false;

        // 1. 产物检查 (拦截 Step 0 及伪装者)
        // 这是为了解决 "Step 0" 配方伪装成普通 Deploying 配方的问题。
        // 只要产物是 "SequencedAssemblyItem" (半成品)，说明这必定是序列组装的一环。
        try {
            @SuppressWarnings("ConstantConditions")
            DeployerBlockEntity be = (DeployerBlockEntity)(Object)this;
            Level level = be.getLevel();
            if (level == null) return true;

            RegistryAccess access = level.registryAccess();
            ItemStack resultStack = recipe.getResultItem(access);

            if (!resultStack.isEmpty()) {
                Item resultItem = resultStack.getItem();

                // 判定 1: 它是 Create 定义的半成品基类吗?(最准确)
                if (resultItem instanceof SequencedAssemblyItem) {
                    return true;
                }

                // 判定 2: 名字里是否有 "incomplete" (针对自定义半成品,但可能有错杀概率)
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(resultItem);
                if (itemId != null && itemId.getPath().contains("incomplete")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // 产物检查报错视为危险
            return true;
        }

        // 2. Java 类型检查
        // 拦截标准的序列组装配方
        if (recipe instanceof SequencedAssemblyRecipe) return true;

        // 3. Serializer 引用检查
        try {
            RecipeSerializer<?> serializer = recipe.getSerializer();
            ResourceLocation serializerId = ForgeRegistries.RECIPE_SERIALIZERS.getKey(serializer);
            if (CREATE_SEQUENCED_ASSEMBLY.equals(serializerId)) return true;
        } catch (Exception e) {
            // 获取失败视为危险，宁可不缓存也不要出错
            return true;
        }
        return false;
    }

    //注入到 getRecipe 方法头部，尝试直接返回缓存的配方。
    @Inject(method = "getRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void lazytick$checkCache(ItemStack stack, CallbackInfoReturnable<Recipe<? extends Container>> cir) {
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
        if (lazytick$isDangerousRecipe(lazytick$cachedRecipe)) {
            this.lazytick$clearCache();
            return;
        }

        // 6. 状态同步
        // 即使直接返回配方，也必须将物品放入容器，否则 Create 后续逻辑（如砂纸打磨）无法正确扣除耐久。
        if (this.recipeInv != null) {
            this.recipeInv.setItem(0, stack);
            this.recipeInv.setItem(1, currentHeld);
        }
        if (this.sandpaperInv != null && currentHeld.getItem() instanceof SandPaperItem) {
            this.sandpaperInv.setItem(0, stack);
        }

        // 7. 缓存命中，直接返回，跳过原版查找
        cir.setReturnValue(lazytick$cachedRecipe);
        cir.cancel();
    }

    /**
     * 注入到 getRecipe 方法尾部，将原版计算出的配方存入缓存。
     */
    @Inject(method = "getRecipe", at = @At("RETURN"), remap = false)
    private void lazytick$saveCache(ItemStack stack, CallbackInfoReturnable<Recipe<? extends Container>> cir) {
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableCacheDeployer()) return;

        DeployerFakePlayer player = this.getPlayer();
        if (player == null) return;

        Recipe<? extends Container> result = cir.getReturnValue();

        // 1. 输入物品检查
        // 如果输入物品本身带有 SequencedAssembly 标签，说明它在传送带上是半成品，必须拉黑。
        CompoundTag tag = stack.getTag();
        boolean inputIsSequenced = tag != null && tag.contains("SequencedAssembly");

        // 2. 配方危险性检查
        // 检查输出是否会导致序列组装。
        boolean recipeIsDangerous = lazytick$isDangerousRecipe(result);

        // --- 黑名单判定 ---
        if (inputIsSequenced || recipeIsDangerous) {
            this.lazytick$hasCached = true;
            this.lazytick$cachedTargetItem = stack.getItem();
            this.lazytick$cachedHeldItem = player.getMainHandItem().getItem();

            this.lazytick$isBlacklisted = true; // 标记为黑名单，下次 checkCache 直接熔断
            this.lazytick$cachedRecipe = null;  // 不缓存危险配方
            return;
        }

        // --- 白名单存入 ---
        this.lazytick$hasCached = true;
        this.lazytick$cachedTargetItem = stack.getItem();
        this.lazytick$cachedHeldItem = player.getMainHandItem().getItem();

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
    }
}