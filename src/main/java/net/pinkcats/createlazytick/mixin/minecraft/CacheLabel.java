package net.pinkcats.createlazytick.mixin.minecraft;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

import static net.pinkcats.createlazytick.CreateLazyTick.IsServerReload;
import static net.pinkcats.createlazytick.CreateLazyTick.LOGGER;

@Mixin(MinecraftServer.class)
public class CacheLabel {

    @Unique
    private static int createLazyTick$tick = 0;

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void ReloadLabel(Collection<String> collection, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        IsServerReload = true;
        LOGGER.info("[CreateLazyTick] clearing cache...");
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void tickServer(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        createLazyTick$tick++;
        if (createLazyTick$tick >20){
            createLazyTick$tick = 0;
            IsServerReload = false;
        }

    }


}
