package net.pinkcats.createlazytick.helper.extradatatool;

public class CrafterExtraDataTool {
    public static int packCrafterData(boolean isPowered, boolean isInWindow, boolean isDelayForced) {
        int data = 0;               //isInWindow  isPowered
        if (isPowered)  data += 1;  // 0/1(F/T)    0/1(F/T)  (binary)
        if (isInWindow) data += 2;
        if (isDelayForced) data += 4;
        return data;
    }

    public static boolean unpackIsPowered(int data) {
        // 使用位运算检查最后一位是否是1(true)
        return (data & 1) != 0;
    }

    public static boolean unpackInWindow(int data) {
        // 使用位运算检查倒数第二位是否是1(true)
        return (data & 2) != 0;
    }

    public static boolean unpackIsDelayForced(int data) {
        return (data & 4) != 0;
    }
}
