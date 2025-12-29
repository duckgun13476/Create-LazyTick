package net.pinkcats.createlazytick.mixin.compat.createenchantment;

import com.simibubi.create.content.fluids.OpenEndedPipe;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = OpenEndedPipe.class)
public class OpenEndedPipeMixin {

    @Shadow(remap = false)
    private Level world;

    @Shadow(remap = false)
    private BlockPos outputPos;

    @Shadow(remap = false)
    private AABB aoe;

    @Shadow(remap = false)
    private BlockPos pos;


    @Inject(method = "provideFluidToSpace", at = @At("HEAD"), cancellable = true, remap = false)
    private void inject(FluidStack fluid, boolean simulate, CallbackInfoReturnable<Boolean> cir){
        System.out.println("provideFluidToSpace");
    }
}
