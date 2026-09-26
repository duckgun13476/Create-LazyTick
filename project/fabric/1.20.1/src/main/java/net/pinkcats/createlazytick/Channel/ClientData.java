package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;

public class ClientData {

    private final BlockPos pos;
    private final String dimension;
    private final int extraData;
    private final long createdAtMillis;

    public ClientData(int extraData , String dimension, BlockPos pos) {
        this.pos = pos;
        this.dimension = dimension;
        this.extraData = extraData;
        this.createdAtMillis = System.currentTimeMillis();
    }


    public String getDimension() {
        return dimension;
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getExtraData() {
        return extraData;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "dimension=" + dimension +
                ", pos="  + pos +
                ", extraData="  + extraData +
                '}';
    }
}
