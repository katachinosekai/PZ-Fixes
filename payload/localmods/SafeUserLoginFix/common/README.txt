Safe User Login Fix 1.0.1 (mod id SafeUserLoginFix), for Safe User Login (Workshop 3232993626, mod id SafeUserLogin).
- Replaces SafeUserLogin's OnZombieUpdate handler (Client.lua:101-143) with the same rule on a keyed set per player
  (O(1), no holes, no growth); setUseless only when the value changes. Caught through SafeUserLogin_mod.Utils:
  createTextBar arms a one-call wrapper on Events.OnZombieUpdate.Add, which keeps SafeUserLogin's handler out;
  removeTextBar (from removeProtection and death) resets the set's zombies like its clearZombieList.
  Not caught: nothing is replaced.
- Under Viewpoint's 3D camera the countdown text is placed with VALCBridge.point (Viewpoint x A-LIFE), else hidden.
- 1.0.1: 1.0.0 called next(), which Kahlua lacks: the first protection failed right after the handler was caught and
  no zombie handler ran. Now pairs-based; any error in the zombie path restores SafeUserLogin's own handler (logged once).
Offline test (Kahlua-only globals): luajit source/test_keyedset.lua <path to SafeUserLogin/42/media/lua>
