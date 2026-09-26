package net.pinkcats.NutUI.Lib;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.pinkcats.createlazytick.CreateLazyTick;
import org.jetbrains.annotations.NotNull;

public class mes {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String BLUE = "\u001B[34m";
    public static final String YELLOW = "\u001B[33m"; // yellow
    public static final String CYAN = "\u001B[36m";   // cyan
    public static final String MAGENTA = "\u001B[35m"; // mega
    public static final String WHITE = "\u001B[37m";   // white
    public static final String BLACK = "\u001B[30m";   // black

    public static final String LOGO = "[Create:LazyTick]";

    public static void blue(Object message) {
        String messageString = String.valueOf(message);
        CreateLazyTick.LOGGER.info(CYAN+LOGO+BLUE + "{}" + RESET, messageString);
    }
    public static void warn(Object message) {
        String messageString = String.valueOf(message);
        CreateLazyTick.LOGGER.warn(CYAN+LOGO+YELLOW + "{}" + RESET, messageString);
    }
    public static void error(Object message) {
        SourceResult result = getSourceResult(message);
        CreateLazyTick.LOGGER.error(CYAN + "[{}][Error]" + RED + "{}" + RESET, result.caller, result.messageString());
    }
    public static void info(Object message) {
        String messageString = String.valueOf(message);
        CreateLazyTick.LOGGER.info(CYAN+LOGO+GREEN + "{}" + RESET, messageString);
    }
    public static void purple(Object message) {
        String messageString = String.valueOf(message);
        CreateLazyTick.LOGGER.info(CYAN+LOGO+MAGENTA + "{}" + RESET, messageString);
    }

    private static final StackWalker WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static void debug(Object message) {
        SourceResult result = getSourceResult(message);
        CreateLazyTick.LOGGER.info(CYAN + "[{}][Debug]" + MAGENTA + "{}" + RESET, result.caller(), result.messageString());
    }

    private static @NotNull SourceResult getSourceResult(Object message) {
        String messageString = String.valueOf(message);
        String caller = WALKER.walk(frames -> {
            boolean seenThisDebug = false;
            for (StackWalker.StackFrame f : (Iterable<StackWalker.StackFrame>) frames::iterator) {
                if (f.getClassName().equals(mes.class.getName())
                        && f.getMethodName().equals("debug")) {
                    seenThisDebug = true;
                    continue;
                }
                if (seenThisDebug) {
                    return shortenClassPath(f.getClassName());
                }
            }
            return "unknown";
        });
        return new SourceResult(messageString, caller);
    }

    private record SourceResult(String messageString, String caller) {
    }

    private static String shortenClassPath(String fullClassName) {
        String[] parts = fullClassName.split("\\.");
        if (parts.length <= 3)
            return fullClassName;
        StringBuilder sb = new StringBuilder();
        for (int i = 3; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            if (i == parts.length - 1)
                sb.append(part);
            else
                sb.append(part.charAt(0)).append('.');
        }
        return sb.toString();
    }


    // display func
    public static Component spaces(int n) {
        if (n <= 0) return Component.empty();
        return Char(" ".repeat(n));
    }

    public static Component enter(){
        return Char("\n");
    }

    /**
     * Only Used for text which not need to translate
     * @return Component
     */
    public static Component Char(String string) {
        return Component.literal(string);}

    /**
     * Only Used for text which do not need to translate
     * @return Component
     */
    public static MutableComponent CharM(String string) {
        return (MutableComponent) Char(string);}



    //for log problem
    public static String fmt(String template, Object... args) {
        if (template == null) return "null";

        String result = template;
        for (Object arg : args) {
            String value = String.valueOf(arg);
            result = result.replaceFirst("\\{}", value.replace("$", "\\$"));
        }
        return result;
    }

    public static void blue(String template, Object... args) {
        String messageString = fmt(template, args);
        CreateLazyTick.LOGGER.info(CYAN+LOGO+BLUE + "{}" + RESET, messageString);
    }
    public static void warn(String template, Object... args) {
        String messageString = fmt(template, args);
        CreateLazyTick.LOGGER.warn(CYAN+LOGO+YELLOW + "{}" + RESET, messageString);
    }
    public static void error(String template, Object... args) {
        String result = fmt(template, args);
        CreateLazyTick.LOGGER.error(CYAN + "[Error]" + RED + "{}" + RESET, result);
    }
    public static void info(String template, Object... args) {
        String messageString = fmt(template, args);
        CreateLazyTick.LOGGER.info(CYAN+LOGO+GREEN + "{}" + RESET, messageString);
    }
    public static void purple(String template, Object... args) {
        String messageString = fmt(template, args);
        CreateLazyTick.LOGGER.info(CYAN+LOGO+MAGENTA + "{}" + RESET, messageString);
    }
}
