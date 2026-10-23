package net.pinkcats.NutUI.menu.architect.data;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public final class EntryListRegression {
    private static int checks;

    public static void main(String[] args) throws Exception {
        check(!EntryList.contains("first"), "static read before singleton initialization");
        check(!EntryList.removeByIdAndMsg("absent", "absent"), "static removal before initialization");
        EntryList.add("first", "early");
        check(EntryList.contains("first"), "static add before initialization");
        check(EntryList.removeByIdAndMsg("first", "early"), "static removal succeeds");

        Entry received = new Entry("received", "packet");
        EntryList.set(new ArrayList<>(List.of(received)));
        EntryList singleton = EntryList.getInstance();
        check(singleton.find("received") == received, "first singleton access preserves received data");
        check(singleton == EntryList.getInstance(), "singleton identity is stable");
        check(singleton.getAllEntries().size() == 1, "received list remains intact");
        singleton.getAllEntries().clear();
        check(EntryList.contains("received"), "returned list is structurally independent");
        check(singleton.update("received", "updated"), "update works");
        check("updated".equals(singleton.find("received").getMsg()), "updated message retained");
        check(singleton.remove("received"), "instance removal works");
        EntryList.add("last", "value");
        singleton.clear();
        check(singleton.getAllEntries().isEmpty(), "clear works");
        check(Modifier.isPrivate(EntryList.class.getDeclaredConstructor().getModifiers()),
                "construction cannot reset global storage");
        System.out.println("EntryList initialization: " + checks + " checks passed");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
