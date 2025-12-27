package net.pinkcats.createlazytick.mixin.Minecraft;

import net.minecraft.server.MinecraftServer;
import net.pinkcats.createlazytick.bridge.Crafter.CrafterCacheStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

import static net.pinkcats.createlazytick.Config.global_cache_record_delay;
import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;
import static net.pinkcats.createlazytick.CreateLazyTick.LOGGER;
import static net.pinkcats.createlazytick.helper.RecipeCacheTool.*;

@Mixin(MinecraftServer.class)
public class CacheLabel {

    @Unique
    private static int createLazyTick$tick = 0;

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void ReloadLabel(Collection<String> collection, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        IsServerReload = true;
        LOGGER.info("[CreateLazyTick] clearing cache...");

        // Clear Spout Cache.
        CAN_FILL_CACHE.clear();
        AMOUNT_CACHE.clear();

        // Clear Crafter Cache / Reset Tick.
        CrafterRecipeCache.clear();
        IsCrafterCacheFull = false;
        CrafterCacheStats.reset();
        CrafterCacheStats.onCooldownSkip();


    }


    @Inject(method = "reloadResources", at = @At("RETURN"))
    private void ReloadEnd(Collection<String> collection, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        IsServerReload = true;
        LOGGER.info("[CreateLazyTick] End cache...");


    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void tickServer(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        createLazyTick$tick++;
        if (createLazyTick$tick > global_cache_record_delay){
            createLazyTick$tick = 0;
            IsServerReload = false;
        }

    }


}
