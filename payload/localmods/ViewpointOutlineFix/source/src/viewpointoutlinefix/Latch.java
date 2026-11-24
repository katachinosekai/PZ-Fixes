package viewpointoutlinefix;

import java.util.IdentityHashMap;
import java.util.Iterator;

// When each object last got setOutlineHighlight(true). Viewpoint 3D clears an outline only once this is
// older than WINDOW_MS; setOutlineHighlight(false) drops the entry (and the game clears the flag itself).
// The latch never turns an outline on: it only delays Viewpoint's per-capture clear of a flag that is set.
public final class Latch {
    public static final long WINDOW_MS = 200;
    static final long WINDOW_NANOS = WINDOW_MS * 1_000_000L;
    private static final int PRUNE_EVERY = 256;

    private static final IdentityHashMap<Object, long[]> LAST_ON = new IdentityHashMap<>();
    private static int calls;

    private Latch() {}

    // Advice entry (inlined into IsoObject.setOutlineHighlight(boolean) and (int, boolean)): last argument is "on".
    public static void onSet(Object self, Object[] args) {
        if (self == null || args == null || args.length == 0) return;
        set(self, Boolean.TRUE.equals(args[args.length - 1]), System.nanoTime());
    }

    public static synchronized void set(Object o, boolean on, long now) {
        boolean wasRecent = false;
        if (on) {
            long[] t = LAST_ON.get(o);
            if (t == null) LAST_ON.put(o, new long[] {now});
            else t[0] = now;
        } else {
            long[] t = LAST_ON.remove(o);
            wasRecent = t != null && now - t[0] <= WINDOW_NANOS;
        }
        Stats.onSet(on, wasRecent, now);
        if (++calls >= PRUNE_EVERY) {
            calls = 0;
            prune(now);
        }
    }

    public static synchronized boolean recent(Object o, long now) {
        long[] t = LAST_ON.get(o);
        return t != null && now - t[0] <= WINDOW_NANOS;
    }

    public static synchronized void prune(long now) {
        for (Iterator<long[]> it = LAST_ON.values().iterator(); it.hasNext();) {
            if (now - it.next()[0] > WINDOW_NANOS) it.remove();
        }
    }

    // latched objects (true within the window) whose flag for this player is already off; Viewpoint captures them dark
    static synchronized int offNow(int playerIndex, long now) {
        int n = 0;
        for (java.util.Map.Entry<Object, long[]> e : LAST_ON.entrySet()) {
            if (now - e.getValue()[0] <= WINDOW_NANOS && e.getKey() instanceof zombie.iso.IsoObject o
                    && !o.isOutlineHighlight(playerIndex)) n++;
        }
        return n;
    }

    static synchronized int size() {
        return LAST_ON.size();
    }

    static synchronized void clear() {
        LAST_ON.clear();
        calls = 0;
    }
}
