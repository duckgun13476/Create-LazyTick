package net.pinkcats.createlazytick.mixin.OptElement.itemdrain;

import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.pinkcats.createlazytick.Config;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.LazyTickLogic;
import net.pinkcats.createlazytick.helper.LazyTickScrollBehaviour;
import net.pinkcats.createlazytick.helper.NetworkSyncHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ItemDrainBlockEntity.class,remap = false)
public abstract class ItemDrainLazyTickMixin extends SmartBlockEntity {

    // 当前实际生效的冷却倒计时
    @Unique
    private int createLazyTick$itemDrainTick = 0;

    public ItemDrainLazyTickMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "addBehaviours", at = @At("RETURN"), remap = false)
    private void lazytick$addScrollBehaviour(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
        LazyTickScrollBehaviour.addTo(this, behaviours);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;tick()V", shift = At.Shift.AFTER), cancellable = true, remap = false)
    public void optimizedTick(CallbackInfo ci) {

        if (!Config.enable_lazy_tick || !Config.enable_lazy_item_drain) {
            return;
        }

        ISmartBlockEntityControl control = (ISmartBlockEntityControl) this;

        NetworkSyncHelper.createLazyTick$syncPacketData(control,
                this.level, this.worldPosition, control.createLazyTick$getLazyTickInterval(), Config.item_drain_delay_max);

        /*if(!level.isClientSide()) {
            System.out.println("ItemDrain:" + createLazyTick$itemDrainTick + "|" + control.createLazyTick$getLazyTickInterval());
        }*/

        ItemDrainAccessor accessor = (ItemDrainAccessor) this;
        TransportedItemStack heldItem = accessor.getHeldItem();
        int processingTicks = accessor.getProcessingTicks();

        if (heldItem == null) {
            accessor.setProcessingTicks(0);
            createLazyTick$resetDelayTick(); // 重置等待时间
            ci.cancel();
            return;
        }

        // 无论是物品输出堵塞还是流体倾倒堵塞，有冷却就跳过
        createLazyTick$itemDrainTick++;
        if (createLazyTick$itemDrainTick < control.createLazyTick$getLazyTickInterval()) {
            ci.cancel();
            return;
        }
        createLazyTick$itemDrainTick = 0;
        // ----------------

        boolean onClient = level != null && level.isClientSide && !isVirtual();

        if (level == null) {
            ci.cancel();
            return;
        }

        if (processingTicks > 0) {
            heldItem.prevBeltPosition = .5f;
            boolean wasAtBeginning = processingTicks == ItemDrainBlockEntity.FILLING_TIME;
            if (!onClient || processingTicks < ItemDrainBlockEntity.FILLING_TIME) {
                processingTicks--;
                accessor.setProcessingTicks(processingTicks);
            }

            // 执行原版处理逻辑
            boolean continueResult = accessor.invokeContinueProcessing();

            if (!continueResult) {
                accessor.setProcessingTicks(0);
                notifyUpdate();
                ci.cancel();
                return;
            }

            // 如果 ticks 被重置回 20,说明内部流体满了,懒加载
            if (accessor.getProcessingTicks() == ItemDrainBlockEntity.FILLING_TIME) {
                createLazyTick$applyBackoff();
                ci.cancel();
                return;
            } else {
                // 否则说明正在倒液,重置等待时间
                createLazyTick$resetDelayTick();
            }

            if (wasAtBeginning != (processingTicks == ItemDrainBlockEntity.FILLING_TIME))
                this.sendData();

            ci.cancel();
            return;
        }


        //original method
        heldItem.prevBeltPosition = heldItem.beltPosition;
        heldItem.prevSideOffset = heldItem.sideOffset;

        heldItem.beltPosition += 1 / 8f;

        if (heldItem.beltPosition > 1) {
            heldItem.beltPosition = 1;

            if (onClient) {
                ci.cancel();
                return;
            }

            Direction side = heldItem.insertedFrom;

            ItemStack tryExportingToBeltFunnel = getBehaviour(DirectBeltInputBehaviour.TYPE)
                    .tryExportingToBeltFunnel(heldItem.stack, side.getOpposite(), false);
            if (tryExportingToBeltFunnel != null) {
                if (tryExportingToBeltFunnel.getCount() != heldItem.stack.getCount()) {
                    if (tryExportingToBeltFunnel.isEmpty())
                        accessor.setHeldItem(null);
                    else
                        heldItem.stack = tryExportingToBeltFunnel;
                    notifyUpdate();

                    // 成功部分输出,重置冷却逻辑
                    createLazyTick$resetDelayTick();

                    ci.cancel();
                    return;
                }
                if (!tryExportingToBeltFunnel.isEmpty()) {

                    createLazyTick$applyBackoff(); // 漏斗阻塞,应用退避
                    ci.cancel();
                    return;

                }
            }

            BlockPos nextPosition = worldPosition.relative(side);
            DirectBeltInputBehaviour directBeltInputBehaviour =
                    BlockEntityBehaviour.get(level, nextPosition, DirectBeltInputBehaviour.TYPE);

            if (directBeltInputBehaviour == null) {
                if (!BlockHelper.hasBlockSolidSide(level.getBlockState(nextPosition), level, nextPosition,
                        side.getOpposite())) {
                    ItemStack ejected = heldItem.stack;
                    Vec3 outPos = VecHelper.getCenterOf(worldPosition)
                            .add(Vec3.atLowerCornerOf(side.getNormal()).scale(.75));
                    Vec3 outMotion = Vec3.atLowerCornerOf(side.getNormal())
                            .scale(1 / 8f).add(0, 1 / 8f, 0);
                    outPos = outPos.add(outMotion.normalize());
                    ItemEntity entity = new ItemEntity(level, outPos.x, outPos.y + 6 / 16f, outPos.z, ejected);
                    entity.setDeltaMovement(outMotion);
                    entity.setDefaultPickUpDelay();
                    entity.hurtMarked = true;
                    level.addFreshEntity(entity);

                    accessor.setHeldItem(null);
                    notifyUpdate();

                    // 成功抛出物品,重置
                    createLazyTick$resetDelayTick();
                } else {
                    createLazyTick$applyBackoff(); // 物理(其他类别方块)阻塞,应用退避
                }
                ci.cancel();
                return;
            }

            if (!directBeltInputBehaviour.canInsertFromSide(side)) {
                createLazyTick$applyBackoff(); // 接口拒绝,应用退避
                ci.cancel();
                return;
            }

            // 核心卡顿点?
            ItemStack returned = directBeltInputBehaviour.handleInsertion(heldItem.copy(), side, false);

            if (returned.isEmpty()) {
                if (level.getBlockEntity(nextPosition) instanceof ItemDrainBlockEntity)
                    award(AllAdvancements.CHAINED_DRAIN);
                accessor.setHeldItem(null);
                notifyUpdate();

                // 完全成功(物品没了),重置
                createLazyTick$resetDelayTick();

                ci.cancel();
                return;
            }

            // 完全失败检测(物品数量数量没变)
            if (returned.getCount() == heldItem.stack.getCount()) {
                createLazyTick$applyBackoff(); // 插入失败,懒加载
                ci.cancel();
                return;
            }

            // 部分插入成功(数量变化)
            if (returned.getCount() != heldItem.stack.getCount()) {
                heldItem.stack = returned;
                notifyUpdate();

                //能够正常运行,正常加载
                createLazyTick$resetDelayTick();

                ci.cancel();
                return;
            }

            ci.cancel();
            return;
        }

        if (heldItem.prevBeltPosition < .5f && heldItem.beltPosition >= .5f) {
            if (!GenericItemEmptying.canItemBeEmptied(level, heldItem.stack)) {
                ci.cancel();
                return;
            }
            heldItem.beltPosition = .5f;
            if (onClient) {
                ci.cancel();
                return;
            }
            accessor.setProcessingTicks(ItemDrainBlockEntity.FILLING_TIME);
            this.sendData();
        }

        ci.cancel();
    }

    @Unique
    private void createLazyTick$applyBackoff() {
        ISmartBlockEntityControl control = (ISmartBlockEntityControl) this;
        createLazyTick$itemDrainTick = 0;

        int currentInterval = control.createLazyTick$getLazyTickInterval();
        int newInterval = LazyTickLogic.computeNextInterval(control, currentInterval, Config.item_drain_delay_max);
        if (newInterval != currentInterval) {
            LazyTickLogic.setIntervalSafe(control, newInterval);
        }
    }

    @Unique
    private void createLazyTick$resetDelayTick() {
        ISmartBlockEntityControl control = (ISmartBlockEntityControl) this;
        createLazyTick$itemDrainTick = 0;

        LazyTickLogic.setIntervalSafe(control, 1);
    }
}