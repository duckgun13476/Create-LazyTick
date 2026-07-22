package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;

import java.util.Objects;

public class ClientData {

    private final BlockPos pos;
    private final String dimension;
    private final int extraData;

    public boolean isSimilar(ClientData other) {
        if (other == null) {
            return false;}

        if (this.extraData != other.extraData) {
            return false;}

        if (!Objects.equals(this.dimension, other.dimension)) {
            return false;}

        return Objects.equals(this.pos, other.pos);

    }


    public ClientData(int extraData , String dimension, BlockPos pos) {
        this.pos = pos;
        this.dimension = dimension;
        this.extraData = extraData;
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

    @Override
    public String toString() {
        return "Packet{" +
                "dimension=" + dimension +
                ", pos="  + pos +
                ", extraData="  + extraData +
                '}';
    }
}
