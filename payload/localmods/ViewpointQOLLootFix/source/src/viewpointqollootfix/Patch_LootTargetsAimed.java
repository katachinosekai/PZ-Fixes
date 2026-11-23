package viewpointqollootfix;

import me.zed_0xff.zombie_buddy.Patch;

// Same hook as Project Viewpoint QOL's Patch_LootTargetsContainers, but the return value is read-only.
// QOL's copy asks for a writable Object return (readOnly=false); aimed() returns LootTargets$Target, so
// Byte Buddy refuses it and the patch never applies. The advice only reads the value, so read-only is the same.
@Patch(className = "viewpoint.interact.LootTargets", methodName = "aimed")
public class Patch_LootTargetsAimed {
    @Patch.OnExit
    public static void exit(@Patch.Return Object target) {
        if (Fix.ON) Fix.apply(target);
    }
}
