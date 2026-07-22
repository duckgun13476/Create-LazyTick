package net.pinkcats.createlazytick.bridge;

public class BeltEum {
    public enum Ending {
        UNRESOLVED(0), EJECT(0), INSERT(.25f), FUNNEL(.5f), BLOCKED(.45f);

        public float margin;

        Ending(float f) {
            this.margin = f;
        }
    }
}
