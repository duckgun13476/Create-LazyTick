package net.pinkcats.createlazytick.Channel;

import net.minecraft.core.BlockPos;

import java.util.Objects;

public class ClientData {

    private final BlockPos pos;
    private final int dimension;
    private final String Player;

    public boolean isSimilar(ClientData other) {
        if (other == null) {
            return false;}

        if (!Objects.equals(this.Player, other.Player)) {
            return false;}

        if (this.dimension != other.dimension) {
            return false;}

        return Objects.equals(this.pos, other.pos);

    }


    public ClientData(String Player , int dimension, BlockPos pos) {
        this.pos = pos;
        this.dimension = dimension;
        this.Player = Player;
    }


    public int getDimension() {
        return dimension;
    }

    public BlockPos getPos() {
        return pos;
    }


    @Override
    public String toString() {
        return "Packet{" +
                "dimension=" + dimension +
                ", pos="  + pos +
                ", Player="  + Player +
                '}';
    }
}
