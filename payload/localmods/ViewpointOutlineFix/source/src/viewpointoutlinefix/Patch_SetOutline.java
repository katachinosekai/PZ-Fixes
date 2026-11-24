package viewpointoutlinefix;

import me.zed_0xff.zombie_buddy.Patch;

// Both IsoObject.setOutlineHighlight overloads, (boolean) and (int, boolean): the last argument is "on".
// AllArguments instead of typed arguments so one advice fits both overloads.
@Patch(className = "zombie.iso.IsoObject", methodName = "setOutlineHighlight")
public class Patch_SetOutline {
    @Patch.OnExit
    public static void exit(@Patch.This Object self, @Patch.AllArguments Object[] args) {
        if (Fix.ON) Latch.onSet(self, args);
    }
}
