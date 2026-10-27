package net.pinkcats.createlazytick.bridge.Create;

import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;

/** A bounded wait for an already scheduled funnel attempt, not a successful transfer. */
public interface IPortableInterfaceLease {
    void createLazyTick$awaitFunnel(FunnelBlockEntity funnel, Object inventory, int remainingTicks);

    interface Source {
        IPortableInterfaceLease createLazyTick$getLeaseOwner();
    }
}
