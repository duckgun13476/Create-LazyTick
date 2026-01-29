package net.pinkcats.createlazytick.mixin.OptElement.itemdrain;

import com.simibubi.create.content.fluids.drain.ItemDrainBlockEntity;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.pinkcats.createlazytick.config.ServerConfig;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import net.pinkcats.createlazytick.helper.util.LazyTickLogic;
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

        if (!ServerConfig.getEnableLazyTick() || !ServerConfig.getEnableLazyItemDrain()) {
            return;
        }

        ISmartBlockEntityControl control = (ISmartBlockEntityControl) this;

        NetworkSyncHelper.createLazyTick$syncPacketData(control,
                this.level, this.worldPosition, control.createLazyTick$getLazyTickInterval(), ServerConfig.getItemDrainDelayMax());

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

        // 如果需要处理倒水(>0)
        if (processingTicks > 0) {
            heldItem.prevBeltPosition = .5f;
            boolean wasAtBeginning = processingTicks == ItemDrainBlockEntity.FILLING_TIME;
            if (!onClient) {

                int interval = control.createLazyTick$getLazyTickInterval();
                boolean success = createLazyTick$performLazyDrain(accessor, interval);

                // 1. 如果处理中断 (返回 false)，直接退出
                if (!success) {
                    accessor.setProcessingTicks(0);
                    notifyUpdate();
                    ci.cancel();
                    return;
                }

                // 2. 处理虽然成功，但储量满了 (Ticks 回到 20)
                if (accessor.getProcessingTicks() == ItemDrainBlockEntity.FILLING_TIME) {
                    createLazyTick$applyBackoff(); // 应用退避
                    ci.cancel();
                    return;
                }

                // 检查是否还没倒完 (Ticks > 0)
                // 如果 == 0,则倒完了,直接向下进入位移逻辑
                if (accessor.getProcessingTicks() > 0) {
                    createLazyTick$resetDelayTick();
                    if (wasAtBeginning != (accessor.getProcessingTicks() == ItemDrainBlockEntity.FILLING_TIME))
                        this.sendData();
                    ci.cancel();
                    return;
                }
            }

            createLazyTick$resetDelayTick();

            if (wasAtBeginning != (accessor.getProcessingTicks() == ItemDrainBlockEntity.FILLING_TIME))
                this.sendData();

            ci.cancel();
            return;
        }


        //original method
        heldItem.prevBeltPosition = heldItem.beltPosition;
        heldItem.prevSideOffset = heldItem.sideOffset;

        int currentInterval = control.createLazyTick$getLazyTickInterval();
        float movementSpeed = 1 / 8f;
        float proposedDist = movementSpeed * currentInterval;
        float targetPos = heldItem.beltPosition + proposedDist;

        boolean crossingCenter = heldItem.beltPosition < 0.5f && targetPos >= 0.5f;

        if (crossingCenter && GenericItemEmptying.canItemBeEmptied(level, heldItem.stack)) {
            // 流体桶且正要经过中间 -> 设置为0.5,处理倒液体
            heldItem.beltPosition = 0.5f;
        } else {
            // 废料/已处理 -> 完全位移补偿
            heldItem.beltPosition = targetPos;
        }

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
        int newInterval = LazyTickLogic.computeNextInterval(control, currentInterval, ServerConfig.getItemDrainDelayMax());
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


    /**
     * 封装了跨越关键帧(5 ticks)时的模拟与执行逻辑(修复关于倒液和物品延迟过长的问题)
     * @return true=流程正常结束(需进一步检查是否装满); false=流程中断(物品消失/空了)
     */
    @Unique
    private static boolean createLazyTick$performLazyDrain(ItemDrainAccessor accessor, int interval) {
        int currentTicks = accessor.getProcessingTicks();
        int targetTicks = Math.max(0, currentTicks - interval);

        // 是否跨越了倒水阈值 (5 tick) 原版:>5 (检查), ==5 (e执行), <5 (动画)
        boolean crossingThreshold = currentTicks > 5 && targetTicks <= 5;

        // 如果需要倒...(跨越阈值)
        if (crossingThreshold) {
            // 1. 强制触发模拟检查
            accessor.setProcessingTicks(6);
            if (!accessor.invokeContinueProcessing()) {
                return false; // 物品被移除了，中断
            }

            // 容量满了Ticks 会被重置回 20 (FILLING_TIME)
            // 停止，不倒水
            if (accessor.getProcessingTicks() == ItemDrainBlockEntity.FILLING_TIME) {
                return true; // 返回 true,由调用者处理"满了"的情况
            }

            // 2. 容量未满且物品正常,强制触发倒水
            accessor.setProcessingTicks(5);
            if (!accessor.invokeContinueProcessing()) {
                return false; // 倒水后如果物品意外消失
            }

            // 检查通过且成功倒水后,应用计时器
        }

        // 3. 应用剩余时间
        // 无论是跨越了阈值，还是普通倒计时，最后都定位到目标时间
        accessor.setProcessingTicks(targetTicks);

        // 执行动画逻辑
        return accessor.invokeContinueProcessing();
    }
}