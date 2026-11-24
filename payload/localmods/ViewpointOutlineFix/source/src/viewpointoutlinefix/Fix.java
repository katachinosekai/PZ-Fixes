package viewpointoutlinefix;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

// On/off switch. On only with the Viewpoint class this fix was written against (pinned by sha256)
// and the game methods it calls; anything else: off, Viewpoint's own outline code runs unchanged.
public final class Fix {
    // viewpoint/models/Characters.class from Viewpoint.jar 0.1.7 (sha256 8d0d5a73...), holds outlinesShown(IsoCell, int).
    static final String CHARACTERS = "viewpoint/models/Characters.class";
    static final String CHARACTERS_SHA = "d6897f27da3e78fd844025cb58331850880159356798883f3708d6d6f7118ec8";

    public static final boolean ON = check();

    private Fix() {}

    static String sha(String resource) throws Exception {
        ClassLoader own = Fix.class.getClassLoader();
        InputStream found = own != null ? own.getResourceAsStream(resource) : null;
        if (found == null) found = ClassLoader.getSystemResourceAsStream(resource);
        if (found == null) return "missing";
        try (InputStream in = found) {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
        }
    }

    // Looks the game members up without initializing any game class.
    static void gameMembers() throws Exception {
        ClassLoader l = Fix.class.getClassLoader();
        Class<?> obj = Class.forName("zombie.iso.IsoObject", false, l);
        Class<?> cell = Class.forName("zombie.iso.IsoCell", false, l);
        Class<?> outline = Class.forName("zombie.iso.fboRenderChunk.FBORenderObjectOutline", false, l);
        obj.getMethod("setOutlineHighlight", boolean.class);
        obj.getMethod("setOutlineHighlight", int.class, boolean.class);
        obj.getMethod("isOutlineHighlight", int.class);
        cell.getMethod("getObjectList");
        outline.getMethod("getInstance");
        outline.getMethod("getDuringUIRenderTime", int.class, obj);
        outline.getMethod("getDuringUIUpdateTime", int.class, obj);
        Class<?> ui = Class.forName("zombie.ui.UIManager", false, l);
        ui.getField("uiRenderTimeMS");
        ui.getField("uiUpdateTimeMS");
        Class.forName("zombie.core.PerformanceSettings", false, l).getField("fboRenderChunk");
    }

    private static boolean check() {
        try {
            String got = sha(CHARACTERS);
            if (!CHARACTERS_SHA.equals(got)) {
                System.out.println("[ViewpointOutlineFix] off: " + CHARACTERS + " is " + got + " (Viewpoint changed or missing), outlines left to Viewpoint");
                return false;
            }
            gameMembers();
            System.out.println("[ViewpointOutlineFix] active: Viewpoint 3D keeps a character outline for "
                    + Latch.WINDOW_MS + " ms after the last setOutlineHighlight(true); setOutlineHighlight(false) still clears at once");
            return true;
        } catch (Throwable t) {
            System.out.println("[ViewpointOutlineFix] off: check failed (" + t + ")");
            return false;
        }
    }
}
