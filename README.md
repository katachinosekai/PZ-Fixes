# PZ-Fixes (B42.22)

The fix mods themselves come from the Steam Workshop collection 3825994973.
This pack only carries what the Workshop can't:

- `settings/`: controls and mod settings, the server's mod list and the mod-list preset C3
- `presets/`: the Kata Solo world preset
- `payload/`: local mods with their ZombieBuddy approvals: Viewpoint - Barricade Fix (deleted from the Workshop, Barricades3D needs it) Viewpoint QOL - Nearby Containers Fix (makes QOL's nearby-containers patch apply) A-Life Viewpoint 3D Fixes (Lua only: under Viewpoint's 3D camera hides A-Life's flat isometric spotlight cone, draws the support heli rope in 3D and turns the encounter arrows with the camera) and Reactive Sound Events 3D Arrow Fix (Lua only: points the sound-event arrow relative to Viewpoint's 3D camera), Viewpoint - Steady Outlines Fix (stops character outlines, e.g. the login-protection and A-Life NPC outlines, from flickering in Viewpoint's 3D view) and Safe User Login Fix (Lua only: makes Safe User Login's login protection cheap in big hordes and puts its countdown above the player in Viewpoint 3D)
- `zbbatch/`: the calibrated ZBBatch cache (`zbbatch.properties`), so Java mods load fast from the first start

Install with PZ Launcher (https://github.com/katachinosekai/pzlauncher/releases/latest):
**Mods > Custom Packs > Choose PZ-Fixes.zip... > Install**. The launcher waits for the
collection, copies these files (backing up yours) and switches ZBBatch on.
