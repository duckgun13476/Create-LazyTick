package net.pinkcats.createlazytick.bridge.Create;

public interface ISmartBlockEntityControl {

    byte createLazyTick$ControlState();
    void createLazyTick$SetForceControl(byte value);

    String createLazyTick$getUserName();
    void createLazyTick$setUserName(String value);


}