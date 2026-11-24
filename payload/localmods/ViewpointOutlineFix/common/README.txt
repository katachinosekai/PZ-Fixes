Viewpoint - Steady Outlines Fix 1.0.1 (mod id ViewpointOutlineFix). Replaces Viewpoint's Characters.outlinesShown
(the per-capture outline clear) with the same rule plus a 200 ms latch fed by IsoObject.setOutlineHighlight(true);
setOutlineHighlight(false) drops the latch and the game clears the flag itself. Pinned to Viewpoint 0.1.7's
Characters.class (sha256 d6897f27...). Source and offline check: source/ (build.sh).
1.0.1: log line "[ViewpointOutlineFix] 1.0.1 loaded by ZombieBuddy" at every load (before "active"/"off"), and while
outlines are in use one stats line per 10 s (max 60): 3D captures, true/false sets, false right after a true (first
caller), kept by the latch, cleared, and "off before capture" (latched but already off: turned off by something the
latch can't override). Note: ZombieBuddy's load lines are in Logs/<date>_DebugLog.txt; console.txt may start later.
