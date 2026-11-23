package viewpointqollootfix;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import viewpointqol.ViewpointQOLContainers;
import viewpointqol.ViewpointQOLVersion;

public final class Fix {
    // QOL's broken Patch_LootTargetsContainers (Workshop 3811256204, jar of 2026-11-16). If QOL changes it, stay off.
    static final String BROKEN = "viewpointqol/Patch_LootTargetsContainers.class";
    static final String BROKEN_SHA = "b719caf22d081b16afa60aa22476c03d2e500ca038575563c05e9a69077799e7";

    public static final boolean ON = check();
    private static boolean ran;

    private Fix() {}

    static String sha(String resource) throws Exception {
        try (InputStream in = ClassLoader.getSystemResourceAsStream(resource)) {
            if (in == null) return "missing";
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
        }
    }

    private static boolean check() {
        try {
            String got = sha(BROKEN);
            if (BROKEN_SHA.equals(got)) {
                System.out.println("[ViewpointQOLLootFix] active: QOL's LootTargets.aimed container patch is applied from this mod");
                return true;
            }
            System.out.println("[ViewpointQOLLootFix] off: " + BROKEN + " is " + got + " (QOL changed or missing), nothing to fix");
        } catch (Throwable t) {
            System.out.println("[ViewpointQOLLootFix] off: check failed (" + t + ")");
        }
        return false;
    }

    // Body of QOL's Patch_LootTargetsContainers.exit, unchanged.
    public static void apply(Object target) {
        if (target != null && ViewpointQOLVersion.isSupported() && ViewpointQOLContainers.isActive()) {
            ViewpointQOLContainers.overrideContainers(target);
            if (!ran) {
                ran = true;
                System.out.println("[ViewpointQOLLootFix] aimed() patch ran with QOL nearby containers active");
            }
        }
    }
}
