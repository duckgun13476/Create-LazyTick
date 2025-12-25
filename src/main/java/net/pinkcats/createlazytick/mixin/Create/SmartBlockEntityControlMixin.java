package net.pinkcats.createlazytick.mixin.Create;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.pinkcats.createlazytick.bridge.Create.ISmartBlockEntityControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SmartBlockEntity.class,remap = false)
public abstract class SmartBlockEntityControlMixin extends BlockEntity implements ISmartBlockEntityControl {

    @Unique private byte lazytick$forceDisabled = 0;
    @Unique private String lazytick$operatorName = "";

    public SmartBlockEntityControlMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }


    // 1. 注入到 initialize()：这是 Create 初始化行为的地方
    //@Inject(method = "initialize", at = @At("RETURN"))
    //private void lazytick$onInit(CallbackInfo ci) {
    //    if (this.lazytick$forceDisabled == 0 && this.level != null) {
   //         ForcedActiveManager.register(this.level, this.worldPosition);
    //    }
    //}

    // 2. 注入到 invalidate()：这是 SmartBlockEntity.setRemoved() 必定会调用的清理方法
    //@Inject(method = "invalidate", at = @At("HEAD"))
   // private void lazytick$onInvalidate(CallbackInfo ci) {
     //   if (this.level != null) {
    //        ForcedActiveManager.unregister(this.level, this.worldPosition);
    //    }
    //}


    @Inject(method = "write", at = @At("RETURN"))
    private void lazytick$writeNBT(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (this.lazytick$forceDisabled == 0) {
            tag.putByte("LazyTickForceDisabled", (byte) 1);
            if (!this.lazytick$operatorName.isEmpty())
                tag.putString("LazyTickOperator", this.lazytick$operatorName);

        }
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void lazytick$readNBT(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (tag.contains("LazyTickForceDisabled")) {

            this.lazytick$forceDisabled = tag.getByte("LazyTickForceDisabled");
            this.lazytick$operatorName = tag.getString("LazyTickOperator");
        } else {
            this.lazytick$forceDisabled = 0;
            this.lazytick$operatorName = "";
        }

    }




    // Interface
    @Override
    public byte createLazyTick$ControlState() { return this.lazytick$forceDisabled; }

    @Override
    public void createLazyTick$SetForceControl(byte value) { this.lazytick$forceDisabled = value; }

    @Override
    public String createLazyTick$getUserName() { return this.lazytick$operatorName; }

    @Override
    public void createLazyTick$setUserName(String value) { this.lazytick$operatorName = value; }



}