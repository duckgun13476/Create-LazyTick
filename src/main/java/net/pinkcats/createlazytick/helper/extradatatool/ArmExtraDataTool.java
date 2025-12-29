package net.pinkcats.createlazytick.helper.extradatatool;

public class ArmExtraDataTool {
    public static int packArmData(boolean ignoreLazy, boolean weakLazy) {
        int data = 0;
        if (ignoreLazy) data += 1; // 第一位:是否忽略
        if (weakLazy)   data += 2; // 第二位:是否弱化
        return data;
    }

    public static boolean unpackIgnore(int data) {
        return (data & 1) != 0;
    }

    public static boolean unpackWeak(int data) {
        return (data & 2) != 0;
    }
}