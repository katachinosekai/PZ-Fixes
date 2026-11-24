package viewpointoutlinefix;

import me.zed_0xff.zombie_buddy.Patch;
import zombie.iso.IsoCell;

// Viewpoint's per-capture outline clear; replaced by Shown.run when the fix is on (exact argument types:
// ZombieBuddy matches the overload by them).
@Patch(className = "viewpoint.models.Characters", methodName = "outlinesShown")
public class Patch_OutlinesShown {
    @Patch.OnEnter(skipOn = true)
    public static boolean enter(@Patch.Argument(0) IsoCell cell, @Patch.Argument(1) int playerIndex) {
        return Fix.ON && Shown.run(cell, playerIndex);
    }
}
