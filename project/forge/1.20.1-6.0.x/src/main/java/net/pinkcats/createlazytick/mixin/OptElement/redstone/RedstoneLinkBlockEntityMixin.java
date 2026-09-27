package net.pinkcats.createlazytick.mixin.OptElement.redstone;

import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import net.minecraft.world.level.Level;
import net.pinkcats.createlazytick.redstone.RedstoneLinkRefreshAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Bound same-signal network refreshes without delaying real signal changes. */
@Mixin(RedstoneLinkBlockEntity.class)
public abstract class RedstoneLinkBlockEntityMixin implements RedstoneLinkRefreshAccess {

    private static final long CREATE_LAZY_TICK$REFRESH_INTERVAL = 200;

    private long createLazyTick$lastRefreshTick = Long.MIN_VALUE;

    @Shadow(remap = false)
    private int transmittedSignal;

    @Override
    public boolean createLazyTick$allowRegularTransmission(int signal) {
        Level level = ((RedstoneLinkBlockEntity) (Object) this).getLevel();
        if (level == null || level.isClientSide()) {
            return true;
        }

        long now = level.getGameTime();
        if (transmittedSignal == signal && createLazyTick$lastRefreshTick != Long.MIN_VALUE
                && now >= createLazyTick$lastRefreshTick
                && now - createLazyTick$lastRefreshTick < CREATE_LAZY_TICK$REFRESH_INTERVAL) {
            return false;
        }
        createLazyTick$lastRefreshTick = now;
        return true;
    }
}
