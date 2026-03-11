package net.pinkcats.createlazytick.helper;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.CreateLazyTick;
import net.pinkcats.createlazytick.bridge.Crafter.CrafterGridSignature;
import net.pinkcats.createlazytick.bridge.Spout.SpoutCacheKey;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.pinkcats.createlazytick.CreateLazyTick.DropResourceLocation;

public class RecipeCacheTool {

    // Spout Cache Class
    public static final Map<Item, Boolean> CAN_FILL_CACHE = Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Item, Boolean> eldest) {
            return size() > ServerConfig.getSpoutCacheMax();
        }
    });


    public static final Map<SpoutCacheKey, Integer> AMOUNT_CACHE = Collections.synchronizedMap(new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<SpoutCacheKey, Integer> eldest) {
            return size() > ServerConfig.getSpoutCacheMax();
        }
    });




    public static boolean IsCrafterCacheFull = false;
    // Crafter Cache Class
    public static final Map<CrafterGridSignature, ItemStack> CrafterRecipeCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CrafterGridSignature, ItemStack> eldest) {
            // 使用配置项 crafter_global_cache_max
            int maxCacheSize = ServerConfig.getCrafterGlobalCacheMax();
            if (size() > maxCacheSize) {
                // 日志需要检查 Debug 开关
                if (!IsCrafterCacheFull && ServerConfig.getEnableCacheCrafterDebugger()) {
                    CreateLazyTick.LOGGER.info("[CreateLazyTick] Crafter Cache hit max capacity ({}). Eviction started.", maxCacheSize);
                    IsCrafterCacheFull = true;
                }
                return true;
            }
            return false;
        }
    };


    // 预定义 Create 序列组装的注册名常量，用于快速比对
    private static final ResourceLocation CREATE_SEQUENCED_ASSEMBLY = DropResourceLocation("create", "sequenced_assembly");

    /**
     * 判断配方是否为“危险”的序列组装配方。
     * 序列组装配方通常是单例且有状态的，缓存它们会导致不同机械手之间数据污染（跳步）。
     * * @param recipe 待检查的配方
     * @return true 表示危险（不可缓存），false 表示安全（可缓存）
     */
    public static boolean isDangerousSARecipe(ItemStack input, Recipe<?> recipe, Level level) {
        // 0. 拦截显式半成品
        if (input.hasTag()) {
            CompoundTag tag = input.getTag();
            if (tag != null && tag.contains("SequencedAssembly")) {
                return true;
            }
        }

        if (recipe == null) return false;

        // 1. 产物检查 (拦截 Step 0 及伪装者)
        // 这是为了解决 "Step 0" 配方伪装成普通 Deploying 配方的问题。
        // 只要产物是 "SequencedAssemblyItem" (半成品)，说明这必定是序列组装的一环。
        try {
            if (level == null) return true;

            ItemStack resultStack = recipe.getResultItem();

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


}
