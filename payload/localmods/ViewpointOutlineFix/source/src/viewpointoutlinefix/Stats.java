package viewpointoutlinefix;

// In-game diagnosis for a flicker that remains: every 10 s while outlines are in use (at most MAX_LINES lines per
// session) one log line with what happened to them. "off before capture" = an outline set to true within the
// window but already off when Viewpoint captured: something turned it off explicitly (see "false while latched"
// and its first caller) or never set it for that player; the latch can't keep those.
public final class Stats {
    static final long PERIOD_NANOS = 10_000_000_000L;
    static final int MAX_LINES = 60;

    private static long periodStart = Long.MIN_VALUE;
    private static int lines;
    private static long setTrue, setFalse, falseLatched, captures, kept, cleared, offAtCapture;
    private static String falseCaller;

    private Stats() {}

    static synchronized void onSet(boolean on, boolean wasRecent, long now) {
        if (on) setTrue++;
        else {
            setFalse++;
            if (wasRecent) {
                falseLatched++;
                if (falseCaller == null) falseCaller = caller();
            }
        }
        tick(now);
    }

    static synchronized void onCapture(int keptNow, int clearedNow, int offNow, long now) {
        captures++;
        kept += keptNow;
        cleared += clearedNow;
        offAtCapture += offNow;
        tick(now);
    }

    // first frame outside this mod and IsoObject.setOutlineHighlight
    private static String caller() {
        try {
            return StackWalker.getInstance().walk(s -> s
                    .filter(f -> !f.getClassName().startsWith("viewpointoutlinefix.")
                            && !(f.getClassName().equals("zombie.iso.IsoObject") && f.getMethodName().equals("setOutlineHighlight")))
                    .limit(3)
                    .map(f -> f.getClassName() + "." + f.getMethodName() + ":" + f.getLineNumber())
                    .reduce((a, b) -> a + " < " + b).orElse("?"));
        } catch (Throwable t) {
            return "?";
        }
    }

    private static void tick(long now) {
        if (periodStart == Long.MIN_VALUE) periodStart = now;
        if (now - periodStart < PERIOD_NANOS) return;
        if (lines < MAX_LINES && (setTrue > 0 || falseLatched > 0)) {
            lines++;
            System.out.println("[ViewpointOutlineFix] last " + ((now - periodStart) / 1_000_000_000L) + " s: 3D captures " + captures
                    + ", setOutlineHighlight true " + setTrue + " / false " + setFalse
                    + " (false within " + Latch.WINDOW_MS + " ms of a true: " + falseLatched
                    + (falseCaller != null ? ", first from " + falseCaller : "") + ")"
                    + ", kept by the latch " + kept + ", cleared " + cleared + ", off before capture " + offAtCapture
                    + (lines == MAX_LINES ? " (last stats line)" : ""));
        }
        periodStart = now;
        setTrue = setFalse = falseLatched = captures = kept = cleared = offAtCapture = 0;
        falseCaller = null;
    }

    static synchronized void reset() {
        periodStart = Long.MIN_VALUE;
        lines = 0;
        setTrue = setFalse = falseLatched = captures = kept = cleared = offAtCapture = 0;
        falseCaller = null;
    }
}
