package net.pinkcats.NutUI.menu.architect.Helper;

import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.pinkcats.NutUI.Lib.mes;

import java.util.Locale;

import static net.pinkcats.NutUI.menu.architect.Helper.ResourceParse.BuildDefine;

public class DefineHelper {

    private static String sanitizePath(String raw) {
        if (raw == null) return "null";

        String s = raw.toLowerCase(Locale.ROOT);

        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            boolean ok =
                    (c >= 'a' && c <= 'z') ||
                            (c >= '0' && c <= '9') ||
                            c == '/' || c == '.' || c == '_' || c == '-';

            out.append(ok ? c : '_');
        }

        String fixed = out.toString();
        fixed = fixed.replaceAll("^_+", "");
        fixed = fixed.replaceAll("_+$", "");
        if (fixed.isEmpty()) fixed = "unnamed";

        return fixed;
    }

    // ===== core =====
    public static ResourceLocation safeDefine(String namespace, String rawPath, String tag) {
        try {
            return BuildDefine(namespace, rawPath);
        } catch (RuntimeException e) {

            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();

            if (!(root instanceof ResourceLocationException)) {
                throw e;
            }

            String fixed = sanitizePath(rawPath);

            try {
                ResourceLocation rl = BuildDefine(namespace, fixed);

                mes.warn(
                        "[{}] Invalid ResourceLocation path '{}', auto-corrected to '{}'. " +
                                "Please rename it to the corrected form to avoid future confusion.",
                        tag, rawPath, fixed
                );

                return rl;
            } catch (RuntimeException e2) {
                mes.error(
                        "["+tag+"] Failed to auto-correct ResourceLocation path. raw='{"
                                +rawPath+"}' fixed='{"+fixed+"}' root='{"+root.getMessage()+"}'"
                );
                throw e2;
            }
        }
    }


}
