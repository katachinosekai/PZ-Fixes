package viewpointoutlinefix;

import zombie.core.PerformanceSettings;
import zombie.iso.IsoCell;
import zombie.iso.IsoMovingObject;
import zombie.iso.fboRenderChunk.FBORenderObjectOutline;
import zombie.ui.UIManager;

// Replacement for Viewpoint's Characters.outlinesShown(IsoCell, int), which runs right after each 3D character
// capture and clears every outline that was not set during the current UI render/update pass (the same rule as
// the game's IsoMovingObject.renderlast). Same rule here, plus: an outline set to true within Latch.WINDOW_MS stays.
public final class Shown {
    private static boolean failed;

    private Shown() {}

    // true: handled, Viewpoint's original is skipped. false: run the original.
    public static boolean run(IsoCell cell, int playerIndex) {
        if (failed || cell == null) return false;
        try {
            long now = System.nanoTime();
            int kept = 0, cleared = 0;
            FBORenderObjectOutline outline = FBORenderObjectOutline.getInstance();
            for (IsoMovingObject o : cell.getObjectList()) {
                if (!o.isOutlineHighlight(playerIndex)) continue;
                if (PerformanceSettings.fboRenderChunk) {
                    long r = outline.getDuringUIRenderTime(playerIndex, o);
                    long u = outline.getDuringUIUpdateTime(playerIndex, o);
                    if (r != 0L && r == UIManager.uiRenderTimeMS) continue;
                    if (u != 0L && u == UIManager.uiUpdateTimeMS) continue;
                }
                if (Latch.recent(o, now)) { kept++; continue; }
                o.setOutlineHighlight(playerIndex, false);
                cleared++;
            }
            Stats.onCapture(kept, cleared, Latch.offNow(playerIndex, now), now);
            Latch.prune(now);
            return true;
        } catch (Throwable t) {
            failed = true;
            System.out.println("[ViewpointOutlineFix] off after an error, Viewpoint clears outlines itself again: " + t);
            return false;
        }
    }
}
