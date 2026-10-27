package net.pinkcats.createlazytick.mixin.OptElement.funnel;

import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.pinkcats.createlazytick.bridge.Create.IPortableInterfaceLease;
import net.pinkcats.createlazytick.config.ServerConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PortableStorageInterfaceBlockEntity.class, remap = false)
public abstract class PortableInterfaceLeaseMixin implements IPortableInterfaceLease {
    @Shadow protected int transferTimer;
    @Shadow protected boolean powered;
    @Shadow protected Entity connectedEntity;

    // A directly attached funnel can occupy at most one of six adjacent blocks.
    // Keep expired entries until real progress/new connection: repeated sleeping ticks
    // must never grant that same consumer a fresh deadline.
    @Unique private FunnelBlockEntity[] createLazyTick$consumers;
    @Unique private Object[] createLazyTick$inventories;
    @Unique private long[] createLazyTick$deadlines;

    @Unique
    private void createLazyTick$clearLeases() {
        if (createLazyTick$consumers != null) {
            java.util.Arrays.fill(createLazyTick$consumers, null);
            java.util.Arrays.fill(createLazyTick$inventories, null);
            java.util.Arrays.fill(createLazyTick$deadlines, 0);
        }
    }

    @Inject(method = {"startConnecting", "stopTransferring", "onContentTransferred"}, at = @At("HEAD"))
    private void createLazyTick$resetLeases(CallbackInfo ci) {
        createLazyTick$clearLeases();
    }

    @Override
    public void createLazyTick$awaitFunnel(FunnelBlockEntity funnel, Object inventory, int remainingTicks) {
        PortableStorageInterfaceBlockEntity self = (PortableStorageInterfaceBlockEntity) (Object) this;
        if (self.getLevel() == null || self.getLevel().isClientSide || self.isRemoved()
                || powered || connectedEntity == null || !connectedEntity.isAlive() || transferTimer <= 4
                || remainingTicks <= 0 || funnel.isRemoved() || funnel.getLevel() != self.getLevel()
                || !funnel.getBlockPos().closerThan(self.getBlockPos(), 1.1))
            return;
        if (createLazyTick$consumers == null) {
            createLazyTick$consumers = new FunnelBlockEntity[6];
            createLazyTick$inventories = new Object[6];
            createLazyTick$deadlines = new long[6];
        }
        for (FunnelBlockEntity consumer : createLazyTick$consumers)
            if (consumer == funnel)
                return;
        for (int i = 0; i < 6; i++) {
            if (createLazyTick$consumers[i] != null)
                continue;
            createLazyTick$consumers[i] = funnel;
            createLazyTick$inventories[i] = inventory;
            createLazyTick$deadlines[i] = self.getLevel().getGameTime() + (long) remainingTicks + 2;
            return;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void createLazyTick$holdForScheduledAttempt(CallbackInfo ci) {
        PortableStorageInterfaceBlockEntity self = (PortableStorageInterfaceBlockEntity) (Object) this;
        if (createLazyTick$consumers == null || self.getLevel() == null || self.getLevel().isClientSide)
            return;
        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyFunnel()
                || self.isRemoved() || powered || connectedEntity == null || !connectedEntity.isAlive()) {
            createLazyTick$clearLeases();
            return;
        }
        if (transferTimer != 5)
            return;
        long now = self.getLevel().getGameTime();
        for (int i = 0; i < 6; i++) {
            FunnelBlockEntity consumer = createLazyTick$consumers[i];
            if (consumer == null || now >= createLazyTick$deadlines[i])
                continue;
            InvManipulationBehaviour inventory = BlockEntityBehaviour.get(consumer, InvManipulationBehaviour.TYPE);
            if (consumer.isRemoved() || consumer.getLevel() != self.getLevel()
                    || consumer.getBlockState().getOptionalValue(BlockStateProperties.POWERED).orElse(false)
                    || inventory == null || inventory.getInventory() != createLazyTick$inventories[i]
                    || !inventory.getTarget().getConnectedPos().equals(self.getBlockPos())) {
                createLazyTick$deadlines[i] = 0;
                continue;
            }
            // Vanilla tick decrements this back to 5. Keep its connection/animation
            // range and clamp unchanged; never call the successful-transfer callback.
            transferTimer = 6;
            return;
        }
    }
}
