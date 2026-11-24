# Notes

Since 2026-11-19 this folder holds only the pack content: settings/, presets/, zbbatch/, payload/. Everything else (mod sources, 108600 originals, approvals list, unsent author reports, Workshop upload tools, old installer, trash, friends' logs) is in Projects/.zomboid-backups/claude-20261119-fixes-uploaded-mods (fixes-rest/, source-*, plus the mod folders). All fix mods are on the Workshop (friends-only).

Build the zip (top folder PZ-Fixes/, the layout Custom Packs expects; payload/localmods/ViewpointFixes is only the source of the Workshop item (3845159212), so it is left out of the zip and of the `public` branch: delete that folder from the public tree before committing it):

    cd .. && rm -f ~/Desktop/PZ-Fixes.zip && python3 -I -c "import os,zipfile;z=zipfile.ZipFile(os.path.expanduser('~/Desktop/PZ-Fixes.zip'),'w',zipfile.ZIP_DEFLATED);[z.write(os.path.join(r,f),os.path.join('PZ-Fixes',os.path.relpath(os.path.join(r,f),'Zomboid-B42.22-Fixes'))) for d in ('settings','presets','zbbatch','payload') for r,_,fs in os.walk(os.path.join('Zomboid-B42.22-Fixes',d)) for f in sorted(fs) if 'ViewpointFixes' not in r];z.write('Zomboid-B42.22-Fixes/README.md','PZ-Fixes/README.md');z.close()"

zbbatch/zbbatch.properties is a copy of ~/.zombie_buddy/zbbatch.properties calibrated on kata's PC; re-copy it after the Java mod list changes (ZBBatch recalibrates by itself when its fingerprint doesn't match).

payload/localmods/ViewpointBarricade (added 2026-11-20): GamerKreep's "Viewpoint - Barricade Fix" 1.0.11, copied from ~/Zomboid/mods. Its Workshop item 3815033191 was deleted from Steam and Barricades3D needs it, so friends get it as a local mod; payload/approvals.txt approves its jar (cbe10c19). Drop both if it comes back on the Workshop.

payload/localmods/ViewpointQOLLootFix (added 2026-11-22): our ZombieBuddy mod for Project Viewpoint QOL (Workshop 3811256204). QOL's Patch_LootTargetsContainers asks for a writable Object return value (`@Patch.Return(readOnly=false)`) on LootTargets.aimed, which returns LootTargets$Target, so Byte Buddy refuses it ("Cannot assign class java.lang.Object to class viewpoint.interact.LootTargets$Target") and "See All Nearby Containers" never reaches the aimed target. This mod applies the same exit code with a read-only return value (QOL's code only reads it). It is pinned to the sha256 of QOL's broken class and turns itself off if QOL changes it; drop it once QOL fixes the patch. Build and offline check: `source/build.sh` (needs JDK, GAME_DIR, WORKSHOP); the jar is reproducible, so its approval in payload/approvals.txt only changes when the code does. Adding it changes the Java mod list, so zbbatch/zbbatch.properties needs a recalibrated copy.

payload/localmods/ALifeHeliSpot3DFix (added 2026-11-23): our Lua-only mod for Project A-Life (Workshop 3803984183, 1.5.3). A-Life's World/ALifeHeliSpotClient.lua draws the spotlight helicopter's overlay (glow diamonds at the aircraft, a faint cone via Tracer.poly, a ground pool) with the isometric projection on OnPreUIDraw, with no 3D gate, so under Viewpoint's 3D camera it shows as a flat 2D triangle on the ground (e.g. at heli soldier drops, which get a spotlight 35% of the time). Its tracers and the support searchlight beam (ALifeHeliLight.beamHidden) already skip drawing when `Tracer.projector == nil and Tracer.inThreeD()`; this mod wraps HeliSpotClient.draw with the same gate. The IsoLightSource ground light is untouched and the isometric view is unchanged. No jar, so no ZombieBuddy approval. Drop it once A-Life gates the overlay itself.

1.1.0 (2026-11-23, display name "A-Life Viewpoint 3D Fixes", same mod id): two more isometric-only A-Life overlays. (a) The support heli fast-rope (World/ALifeSupportClient.lua, renderRope on OnPreUIDraw -> drawRope -> Tracer.screenPoint, iso math while Tracer.projector is nil) drew a 2D line in 3D. renderRope is registered by value, but it calls `pcall(Client.drawRope, ...)` through the table, so we wrap SupportClient.drawRope: in 3D the two rope ends go through `VALCBridge.point(x, y, zStoreys)` from Viewpoint x A-LIFE (Workshop 3822854313; Java valcompat/Bridge.java: returns a Java list {sx, sy} in full-window pixels, the same space OnPreUIDraw lines use, or an empty list when behind the camera/no camera copy; needs `VALCBridge.want(name, true)` once so the camera is copied each frame). No bridge or an end not projectable: no rope in 3D, never an iso line. (b) The encounter compass (Interface/ALifeEncounterCompass.lua, placement with cameraTurn = pi/4 and viewFlatten = 0.5): we wrap EncounterCompass.placement (drawArrow looks it up through the table); in 3D the arrow direction is the target bearing relative to the camera heading, screen up = forward, no flattening. Heading: `Viewpoint.Addons.viewState().crosshair.dx/dy` (Viewpoint's add-on view feed, already in world tiles), else `VALCBridge.centre(playerZ)` minus the player position; no heading (straight down, nothing available): no arrows in 3D. The 3D test for all three is now `VALCBridge.active()` (Viewpoint's View.enabled, so it follows a view switch on the same frame) when the bridge is loaded and not in split screen; otherwise A-Life's Tracer.inThreeD, which re-checks only every 250 ms. A-Life's own 3D gates (tracers, searchlight beam, far marks) still use the 250 ms check, so they can still draw isometrically for up to 250 ms after switching to 3D; not changed.

payload/localmods/ReactiveSE3DFix (added 2026-11-23): our Lua-only mod for Reactive Sound Events (mod id ReactiveSE, Workshop 3656190498, 1.3.0). ReactiveSE_SoundMarker:setAngleFromPoint (42.21/media/lua/client/ReactiveSE/ReactiveSE_SoundMarker.lua:109-126) turns the world offset to the sound into a screen direction with the fixed isometric rotation, so under Viewpoint's 3D camera the arrow points the wrong way. This mod replaces that class method (looked up per call by render and update): while Viewpoint.Loot.viewOn() (or VALCBridge.active()) says 3D is on, the angle is the bearing to the source minus the camera yaw from Viewpoint.Addons.viewState().eye.yaw (degrees, world bearing of the view direction), so up on the arrow is where the camera looks; if viewState is not available yet it uses the player's facing, and otherwise the original code. Isometric is unchanged. No jar, so no ZombieBuddy approval. Drop it once ReactiveSE handles the 3D view itself.

payload/localmods/ViewpointFoliageMotion (added 2026-11-23, NOT yet in the installer, approvals.txt or any mod list: test locally first): our ZombieBuddy mod for Viewpoint 0.1.7, foliage motion vectors for FSR/TAA. Viewpoint's velocity target (G-buffer colour 3, RGBA16F) is cleared in ModelPass.gbuffer and written only by model.vert/model_gbuffer.frag (lib/motion.glsl: `vec4((clip.xy/clip.w - prevClip.xy/prevClip.w) * 0.5, prevClip.w, 1)`, clip = plainViewProjection, prev = previousViewProjection x last frame's matrices); every other pixel (w = 0) gets depth reprojection (upscale_inputs.frag / lib/screen_motion.glsl: uUnproject = invPlainViewProjection, uPrevious = FrameContext.reprojection); FSR gets `-velocity.xy` (uv units, points to the previous position). Wind sway lives only in vertex/compute code: plant cards in mesh.vert -> lib/mesh_vertex.glsl -> lib/plant.glsl plantSway (uPlantSway = windX, windZ, FrameContext.time, gusts; FrameUniforms.wind), Viewpoint's grass in grass_blades.comp -> lib/grass_bend.glsl windBend (same uPlantSway plus uWindScale/uSwayPace), packed per blade into the blade buffer that grass.vert reads; so swaying foliage got camera-only motion. Model-pack trees (Lowpoly Tree Pack, drawn as uModels meshes in the same gbuffer program, lib/pack_model.glsl) and far billboards (far_tree.vert) have no sway, so reprojection is already right for them. The mod rewrites "gbuffer" (mesh.vert+gbuffer.frag), "grass" (grass.vert+grass.frag) and "grass blades" (compute) in SnippetSplice.splice: the vertex stage runs the sway again with last frame's uPlantSway (kept per FrameContext.index; eye too for eye-facing cards), the compute stores last frame's bends (same packing) in our SSBO (binding 7, a vec4 per blade slot), and the fragment stages write velocity in the model pass's encoding (prev clip via FrameContext.reprojection, so still plants give exactly the reprojection motion; non-plant mesh pixels write 0 = unchanged). All additions are inside `#if VFM_MOTION`; if a link fails, ProgramLinker.finish relinks with the define at 0 (Viewpoint's program). Java: the velocity attachment is cleared/enabled from SurfacePass.gbuffer's clear (ContactShadows.reads hook) until BloodPass.draw, and ModelPass.gbuffer's own clear is masked (glColorMaski 3) until its first model draw (Wireframe.apply). Pins: sha256 of 12 shader files and 20 classes. Off: Mod Options tickbox (read live from ModOptions.ini) or -Dviewpointfoliagemotion=off (nothing hooked). Approximations: grass trample and blasts use this frame's values for the previous position; grass far-calm uses this frame's eye. Build/offline checks: `source/build.sh` (glslang compile + link of original/rewritten/fallback texts expanded by Viewpoint's own Shaders.load). Shimmer A/B: `source/tools/shimmer.sh` (one headless boot, wind override via AutoLua, the add-on's capture trigger, `shimmer.py` = temporal 2nd-difference "flicker" over green pixels). To confirm on the first real run: (1) log `[ViewpointFoliageMotion] active ...: 3 programs rewritten` and no "did not link"; ZombieBuddy applied every hook incl. the private FrameUniforms.values and GrassPass.grow (no ZB errors); (2) no GL errors and the picture unchanged with the tickbox off; (3) Viewpoint's motion-vector debug view (or the FSR result) shows grass/plants moving with the wind and still ground still; (4) the shimmer run: AutoLua wind override really changes getWindIntensity (printed in the ready line), the first-person camera faces foliage (look at on_first.ppm / mask.ppm), frame times similar in both states, the capture reads the world image (outputDrawFbo) and not black/UI; (5) toggling Mod Options writes ModOptions.ini (live switch within ~2 s).

ViewpointFoliageMotion 1.1.0 (2026-11-23, after the first in-game run: flicker ON 0.971 vs OFF 0.824): offline re-check found the encoding, sign, scale and jitter consistent with Viewpoint (model pass and our foliage both use plainViewProjection for the current clip; ours uses FrameContext.reprojection = last plain view-projection shifted by the origin change, exactly what upscale_inputs.frag uses for still pixels; FSR gets the jitter separately in UpscaleFrame), prev bends written in the same compute invocation and slot that grass.vert reads (no double-buffer: grow -> barrier -> draw each frame), FrameContext.index ++ once a frame and the surface G-buffer drawn once (velocity target is RGBA32F, not RGBA16F). The run1 frames show the A/B was confounded by gusts: trunks/canopy identical in both states, but the foreground grass swayed in the ON window (9-frame low-pass std 2.03) and was almost still in the OFF window (0.98; mean-frame tile sharp), so ON "flickered" more because it moved more. One real defect fixed: grass.vert's FmPrevBlades block binding was set once per GL program id (a relink that reuses the id would read Viewpoint's blade records as last frame's bends); now set on every grass draw (grass.vert is #version 330, a layout binding would need ARB_shading_language_420pack). New: Velocity.java debug dumps (velocity target and FSR's motion input as PFM + colour PPM, stats incl. wind-axis alignment and FSR motion on still pixels), capture heading scan (Look.hold, not pinned), EncodingTest (bytecode + numeric reprojection + prev-bends ordering + frame keying), shimmer.sh --runs/--hour/--at/--scan/--pitch, shimmer.py sway/fast columns, PFM reader, --summary.

ViewpointFoliageMotion 1.2.0 (2026-11-23, user: the game's own grass tiles still jagged in 3D, speckled blades with dark dots; recording 2026-11-23 19-09-07): sprite edges. The game's grass tiles are Viewpoint plant cards (Meshes.issue sets uPlants=1, chunk-mesh G-buffer program mesh.vert+gbuffer.frag), so they already had 1.1.0's motion vectors. With TAA or any upscaler on, FrameContext.plantsHashed() is true: SurfacePass.gbuffer sets uAlphaHash=1 (uAlphaRange 0.85) and the forward translucent pass skips foliage (uNoFoliage), so foliage edges are decided only by lib/sample_sprite_body.glsl:32-33, a per-pixel alpha hash against the full mip-0 alpha (`own`), re-randomised every frame (alphaHashShift, 64-frame cycle). The game's grass art is mostly half-transparent: e_newgrass_1 has 64% of its visible texels at 0 < alpha < 1 and thin blades never reach 1 (Tiles2x pages: straight alpha, transparent texels white, no mips; TexturePackPage -> TextureID flags 0/4, no premultiply), so close up every blade is ~68% random dots. FSR3 gets colour/depth/motion only (FsrVendor.record: no reactive or transparency mask) and the depth under each dot flips blade/ground per frame, so it rejects history there: speckled, stair-stepped blades, darker dots on the bright ground. Fix (Rewrite.EDGE_HEADER/EDGE_TEST, both pasted copies of the hash test, inside #if VFM_MOTION): coverage = premultiplied bilinear alpha (4 texelFetch clamped to the sprite's texel rect), sharpened to one screen pixel around cut 0.5 ((a - cut) / fwidth(a) + 0.5), only that band hashed; magnified edge pixels take the premultiplied colour (no white bleed). Cut 0.5 keeps coverage (texels >= 0.5 cover 1.02 x the alpha sum on e_newgrass_1; d_plants 0.98). Offline model (source/tools/edges_model.py on e_newgrass_1_0): at 4x magnification undecided (hashed) pixels 68% -> 10%, per-pixel frame variance 0.104 -> 0.017, coverage ratio 1.006; at 0.6x 66% -> 36%. Uniform uFmEdgeCut set with uFmOnMesh in FrameUniforms.values (0 = Viewpoint's test); Mod Options tickbox "edges" (default on, live); -Dviewpointfoliagemotion.edgeCut=<0..1> (default 0.5, 0 = off); capture plan switch=edges and shimmer.sh --switch edges for the A/B. New pins: lib/sample_sprite_body.glsl, sample_sprite.glsl, sample_wall.glsl, lanczos.glsl, alpha_hash.glsl. Cost: 4 texel fetches per hashed foliage pixel. Not changed: shadows, the non-TAA path (forward blend), isometric.

payload/localmods/ViewpointOutlineFix (added 2026-11-23): our ZombieBuddy mod for Viewpoint (Workshop 3809306528, 0.1.7, jar sha256 8d0d5a73). Symptom: in Viewpoint 3D, character outlines set by mods flicker (Safe User Login's white player outline during login protection, Viewpoint x A-LIFE's NPC outline under the crosshair). Cause: the game keeps an outline only while it is re-set every frame. IsoObject.setOutlineHighlight stores, per player, the UIManager.uiRenderTimeMS/uiUpdateTimeMS of the UI pass it was called in (0 when called outside UIManager.render/update), and IsoMovingObject.renderlast clears every outline whose stored time is not the current one. Viewpoint skips IsoCell.render in 3D and does the same clear itself in viewpoint.models.Characters.outlinesShown, right after each capture (FrameFill.captureWorld: Characters.snapshot, then outlinesShown); CharacterOutline.apply reads ModelSlotRenderData.characterOutline, filled from isOutlineHighlight(player) in ModelSlotRenderData.init during the capture. So an outline set outside the UI pass shows only on captures where its setter ran after the previous capture. Under Viewpoint that is not every frame: Viewpoint runs the game at a fixed tick (FixedTick.updating skips IngameState.update, so OnTick runs 0..n times per frame) and draws UI frames on its own budget (UiBetween). Safe User Login sets the outline from its text bar, which is setFollowGameWorld(true): UIManager.render skips those, IngameState.renderFrameTextInternal draws them, outside UIManager.rendering, so every call is stamped "not during UI"; its update() runs only on UI ticks (UIElement.update calls Lua only when UIManager.doTick, every 100 ms). ViewpointALife sets the NPC outline from OnTick, OnRenderTick and OnPostUIDraw (fired after UIManager.rendering is reset): same. Fix: Patch_OutlinesShown (OnEnter skipOn) replaces outlinesShown with Shown.run: the same rule, plus an outline is kept while setOutlineHighlight(true) was called on that object within the last 200 ms (Latch, fed by Patch_SetOutline on both IsoObject.setOutlineHighlight overloads via AllArguments). It never sets an outline: setOutlineHighlight(false) clears the flag in the game's own method and drops the latch, and an outline whose mod stops calling true goes away at most 200 ms later. Isometric is untouched (IsoMovingObject.renderlast is not patched; the setter hook only records a timestamp, pruned every 256 calls). Pinned to the sha256 of Viewpoint's Characters.class (d6897f27); on a mismatch, missing game members or an error it stays off and Viewpoint's code runs; one log line "[ViewpointOutlineFix] active/off". Build and offline check: source/build.sh (needs JDK, GAME_DIR, WORKSHOP): both patches through ZombieBuddy's PatchTransformer, applied to the real IsoObject and Characters and JVM-verified, the setter advice on a probe, and a simulated 144 fps capture loop with a 30 Hz setter (old rule: outline missing in 228 of 288 captures, latch: 0; gone 208 ms after the setter stops; explicit false hides it at the next capture). The jar is reproducible; its approval is in payload/approvals.txt. Adds a Java mod, so zbbatch/zbbatch.properties needs a recalibrated copy. In-game check: Viewpoint 3D, third person, log in with Safe User Login: steady white outline until the countdown ends, then gone; aim the crosshair at an A-Life NPC: steady outline, gone when you look away.

payload/localmods/SafeUserLoginFix (added 2026-11-23): our Lua-only mod for Safe User Login (mod id SafeUserLogin, Workshop 3232993626, B42 files in 42/). (1) Lag: its OnZombieUpdate (Client.lua:101-143) runs for every zombie update while a login protection is on (and protection lasts until the player moves or attacks, so a still test player keeps it forever): it scans the player's zombiesList linearly; entries are removed by setting them to nil, so the list gets holes, `#` stops early, zombies are not found and are appended again (the list grows); and it calls setUseless(false) then setUseless(true) for every attacking zombie on every update. This mod runs the same rule with a keyed set per player (O(1), no holes, bounded by the zombies that attacked) and calls setUseless only when the value changes (IsoZombie.setUseless is a plain field write, so skipping a same-value call changes nothing). CanSee (a line-of-sight test) is asked only when it can change the result (zombie in the set, or targeting the player while attacking). Same protection: useless = can see the player and targets the player and bAttack; joins the set then; leaves it when it can no longer see the player; every update during protection also resets the flag otherwise (as the original's setUseless(false)); at the end (removeProtection after the 5 s countdown, or death) every zombie in the set that is still useless is reset (its clearZombieList). Only difference: with several local players (split screen) a zombie is useless if that holds for any protected player; the original's result depended on pairs() order (last player wins). Seam: Client.lua's functions and playersList are locals, but it calls its helpers through the global table SafeUserLogin_mod.Utils, looked up per call. Utils.createTextBar is wrapped: it records the protected player and, the first time, arms a one-call wrapper on Events.OnZombieUpdate.Add; SafeUserLogin's OnCreatePlayer calls that Add with its handler a few lines later (Client.lua:182-185), which is kept out of the event and replaced by ours. SafeUserLogin never registers it again (its onZombieAdded stays true since its handler never runs). Utils.removeTextBar is wrapped to reset the set (removeProtection calls it right before its own clearZombieList, which now has an empty list). If the handler is not caught (Add not writable, other order), the wrapper is disarmed by our OnCreatePlayer handler (runs right after SafeUserLogin's) or the next tick and nothing is replaced. (2) Its countdown text (ISTextBarManager.lua, StandardText) is placed with isoToScreenX/Y: under Viewpoint 3D (VALCBridge.active(), else Viewpoint.Loot.viewOn(); not in split screen) update/render place it at VALCBridge.point(player x, y, z + 0.75) + (30, -20) px, and prerender skips the text when that can't be projected; isometric unchanged. (3) The outline (set three times a frame by the bar) is left alone: its 3D flicker is fixed for all mods by ViewpointOutlineFix. Offline test: `luajit source/test_keyedset.lua <SafeUserLogin>/42/media/lua` loads SafeUserLogin's real files twice (alone and with this mod) on mocks and drives both with the same 50k random zombie updates through login, countdown, end, respawn and death: useless flags equal after every update; a steady attacker costs 2000 setUseless calls in 1000 updates originally, 1 here; the original leaves 2 zombies useless after the end (its hole-y list scan misses them), this mod none; 3D placement and hiding checked with a bridge mock. No jar, so no ZombieBuddy approval. In-game check: load a save, stand still with a horde around: no lag growth; move: countdown, then "Login Protection Disabled" and zombies attack again; in 3D third person the countdown floats beside the player.

payload/localmods/SafeUserLoginFix 1.0.1 + ViewpointOutlineFix 1.0.1 (2026-11-23, after the user's first MP session with 1.0.0): SafeUserLoginFix 1.0.0 called next(), which PZ's Kahlua lacks (BaseLib registers no next; stdlib.lua neither): "Object tried to call nil in syncHandler" at line 82 right after "zombie handler replaced", so during that protection no zombie handler ran at all. 1.0.1 uses a pairs loop, wraps the zombie path in pcall and on any error puts SafeUserLogin's own caught handler back (log "SafeUserLoginFix: error, SafeUserLogin's handler restored", once); the offline test now runs every file with only Kahlua's globals (next/xpcall/loadstring/table.unpack absent) and rejects goto/::/// in the source; with the 1.0.0 file it fails exactly like the game. ViewpointOutlineFix was NOT missing in that session: ~/Zomboid/console.txt starts after ZombieBuddy's load phase; Logs/2026-11-23_14-35_DebugLog.txt shows it in ZB's list, "[ViewpointOutlineFix] active" at 14:36:12, IsoObject.setOutlineHighlight patched and Characters.outlinesShown patched at 14:37:18 (after joining the server; Viewpoint says fixed tick off in MP). So if outlines still flicker, something clears them that the latch does not cover (an explicit setOutlineHighlight(false), e.g. IsoMovingObject.renderlast if IsoCell.render runs, or a mod's false/true toggle). 1.0.1 adds an always-printed load line and a 10 s stats line (captures, sets, false right after true with its first caller, kept, cleared, off before capture) to tell which in the next session.

## payload/localmods/ViewpointFixes
### What it is (added 2026-11-23, not yet in the installer, approvals.txt, any mod list or the zip)
Viewpoint Fixes 1.0.0 (mod id ViewpointFixes, javaPkgName viewpointfixes, jar 42/media/java/client/ViewpointFixes.jar): our three ZombieBuddy fixes for Viewpoint 0.1.7 merged into one mod for the friends-only Workshop upload. Modules: OutlineFix 1.0.1 (was ViewpointOutlineFix), QOLLootFix 1.0.0 (was ViewpointQOLLootFix), FoliageMotion 1.2.0 (was ViewpointFoliageMotion). Sources copied from payload/localmods/<old mod>/source at master 752223e; module code, pins and checks unchanged except as listed below. The three old folders stay in payload/localmods until the switch-over (the pack still ships ViewpointQOLLootFix and ViewpointOutlineFix).
### Replaces the standalone mods, never alongside them
ViewpointFixes replaces ViewpointOutlineFix, ViewpointQOLLootFix and ViewpointFoliageMotion; they must not be enabled together with it (both would patch the same methods). Guard (viewpointfixes.Guard.standalone): when a module starts, if the system class loader has the old mod's <package>/Main.class (its jar is loaded) or zombie.ZomboidFileSystem.instance.getModIDs() lists the old mod id, it logs "[ViewpointFixes/<Module>] WARN the standalone mod <id> is also enabled (...); this module stays off so nothing is patched twice" and that module stays off: OutlineFix and QOLLootFix Fix.ON = false (their advice does nothing), FoliageMotion state 2 (no shader is rewritten, no relink, no velocity target changes; checked again on the first shader link). The other modules are not affected. mod.info's loadModAfter lists the three old ids so an old mod that is still enabled loads first (ZombieBuddy adds each jar and runs its Main in mod order), which makes the jar check reliable.
### Layout
ZombieBuddy only collects @Patch classes whose package is exactly javaPkgName (PatchEngine.collectPatches compares Class.getPackage().getName()), so all 16 patch classes are in viewpointfixes, named Patch_<Module>_<old name> (Outline 2, QOLLoot 1, Foliage 13); module code is in viewpointfixes.outline, viewpointfixes.qolloot and viewpointfixes.foliage (old Main of FoliageMotion is foliage.Foliage). Advice is inlined into game/Viewpoint classes, so everything it calls is public (AdviceAccessTest checks all 16).
### One entry point, isolated modules
viewpointfixes.Main.main prints "[ViewpointFixes] 1.0.0 loaded by ZombieBuddy: OutlineFix 1.0.1, QOLLootFix 1.0.0, FoliageMotion 1.2.0", then starts each module inside Main.module, which catches any Throwable and logs "[ViewpointFixes/<Module>] off: start failed (...); the other modules are not affected". Each module keeps its own state in its own classes and its own safety checks: OutlineFix pins Viewpoint's Characters.class (sha256 d6897f27...) and the game members it calls, Shown.run turns the module off after an error; QOLLootFix pins QOL's broken Patch_LootTargetsContainers.class (b719caf2...) and is off as soon as QOL changes it (or QOL is missing); FoliageMotion keeps its pins (17 shader files, 20 classes, checked on the first foliage program link), Rewrite.Miss leaves a changed shader alone, the link fallback relinks Viewpoint's own program if a driver refuses the edit, and 20 runtime errors turn it off. Changes: QOLLootFix's calls into QOL moved to qolloot.Qol (only loaded once the module is on and runs), and an exception there now turns the module off instead of reaching LootTargets.aimed; QOL is no longer a hard requirement (require= is ZombieBuddy and Viewpoint; without QOL the module logs "off: ... missing").
### Switches (Mod Options page "Viewpoint Fixes", all default on)
Lua 42/media/lua/client/ViewpointFixes_Options.lua; ModOptions.ini lines tickbox|ViewpointFixes|<key>|true/false, keys outline ("Steady character outlines"), qolloot ("QOL: nearby containers on the aimed target"), foliage ("Foliage motion vectors", was tickbox|ViewpointFoliageMotion|enabled) and edges ("Clean foliage sprite edges", was tickbox|ViewpointFoliageMotion|edges). viewpointfixes.Options reads the file (mtime check) at most every 2 s, from Shown.run (each 3D capture), qolloot Fix.apply (each aimed()) and Motion.frame; every change is logged once with its module prefix ("[ViewpointFixes/OutlineFix] off (Mod Options)", "[ViewpointFixes/FoliageMotion] sprite edges off (Mod Options)"). Off means: outline, the latch is not fed and Viewpoint's own outlinesShown runs; qolloot, aimed() is left to Viewpoint/QOL; foliage and edges as in ViewpointFoliageMotion 1.2.0. The standalone mod's saved tickboxes are not migrated (new keys start on). Unchanged: -Dviewpointfoliagemotion=off (FoliageMotion fully off), -Dviewpointfoliagemotion.edgeCut=<0..1>, -Dviewpointfoliagemotion.velocityDump=<dir>, -Dviewpointfoliagemotion.velocityScale=<px>, trigger files Zomboid/Lua/ViewpointFoliageMotion-capture.txt and ViewpointFoliageMotion-velocity.txt.
### Log lines
Prefixes [ViewpointFixes], [ViewpointFixes/OutlineFix], [ViewpointFixes/QOLLootFix], [ViewpointFixes/FoliageMotion] (were [ViewpointOutlineFix], [ViewpointQOLLootFix], [ViewpointFoliageMotion]); the text after the prefix is the old one, plus the module version in the "active" lines. Load: "[ViewpointFixes/OutlineFix] 1.0.1 active: ...", "[ViewpointFixes/QOLLootFix] 1.0.0 active: ...", "[ViewpointFixes/FoliageMotion] 1.2.0 loaded; checks Viewpoint on its first shader link"; first 3D frame: "[ViewpointFixes/FoliageMotion] active 1.2.0: 3 programs rewritten (...); foliage motion vectors on; sprite edges on, cut 0.5". source/tools/shimmer.sh greps the new prefix, checks "mod = ViewpointFixes," in the mod list and refuses if ViewpointFoliageMotion is enabled too.
### Build and offline checks
source/build.sh (JDK, GAME_DIR, WORKSHOP as for the old mods; needs Viewpoint 3809306528 and Project Viewpoint QOL 3811256204 for compiling): one jar, reproducible (fixed date), then ModulesTest (load lines, module isolation, the four switches with a private user.home, Options re-read interval), ModulesTest guard (fake old Main.class on the class path: all three modules off with their WARN line), AdviceAccessTest (all 16 patch classes), OutlineFixCheck + signatures (as 1.0.1), LootFixCheck (as 1.0.0; also shows QOL's own patch is still refused by Byte Buddy with the QOL jar of Nov 19), RewriteTest, EncodingTest, HookCheck (Patch_Foliage_*), shimmer.py --selftest, glslangValidator compile of 27 texts + vert/frag link (now required: the build fails without glslangValidator). 2026-11-23: 177 OK, 0 FAIL (the old builds: 23 + 12 + 119; the two AdviceAccessTests are one now, ModulesTest adds 24). Code has no comments (rule); what was in the copied sources' comments is below per file.
### Pack and zip
Switched over 2026-11-24 (branch pack/workshop-switch): ViewpointFixes is in both mod lists (where ViewpointQOLLootFix was, after ProjectViewpointQOL) and in payload/approvals.txt (jar b2c997ed). payload/localmods/ViewpointFixes stays in the repo as source only and the zip build command skips it (it ships via the Workshop collection).

Also 2026-11-24: ALifeHeliSpot3DFix, ReactiveSE3DFix, SafeUserLoginFix, ViewpointOutlineFix, ViewpointQOLLootFix and ViewpointFoliageMotion now ship via the Workshop collection (ALifeHeliSpot3DFix 3845159242, ReactiveSE3DFix 3845159276, SafeUserLoginFix 3845159303, ViewpointFixes 3845159212 replacing the other three); their payload/localmods folders were removed from the pack (git history keeps them) and their approvals lines dropped. The sections about them below are history. payload/localmods now holds only ViewpointBarricade (shipped in the zip) and ViewpointFixes (source only).

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/Patch_Foliage_ContactReads.java
### @Patch(className = "viewpoint.render.ContactShadows", methodName = "reads", isAdvice = true)
Called in SurfacePass.gbuffer right after its glClear, with the G-buffer bound.

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/Patch_Foliage_Wireframe.java
### @Patch(className = "viewpoint.render.Wireframe", methodName = "apply", isAdvice = true)
ModelPass.gbuffer's first call after its velocity clear: the clear stays masked, every model draw is not.

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/Patch_Outline_OutlinesShown.java
### @Patch(className = "viewpoint.models.Characters", methodName = "outlinesShown")
Viewpoint's per-capture outline clear; replaced by Shown.run when the fix is on (exact argument types:
ZombieBuddy matches the overload by them).

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/Patch_Outline_SetOutline.java
### @Patch(className = "zombie.iso.IsoObject", methodName = "setOutlineHighlight")
Both IsoObject.setOutlineHighlight overloads, (boolean) and (int, boolean): the last argument is "on".
AllArguments instead of typed arguments so one advice fits both overloads.

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/Patch_QOLLoot_LootTargetsAimed.java
### @Patch(className = "viewpoint.interact.LootTargets", methodName = "aimed")
Same hook as Project Viewpoint QOL's Patch_LootTargetsContainers, but the return value is read-only.
QOL's copy asks for a writable Object return (readOnly=false); aimed() returns LootTargets$Target, so
Byte Buddy refuses it and the patch never applies. The advice only reads the value, so read-only is the same.

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/foliage/Capture.java
### public final class Capture
Test aid for the shimmer measurement (source/tools/shimmer.sh), idle unless its trigger file has a plan:
Zomboid/Lua/ViewpointFoliageMotion-capture.txt, one line
"frames=120 warmup=90 crop=768 out=/abs/dir order=on,off [switch=motion|edges] [velocity=1] [scan=8 scanframes=45 pitch=-12]".
switch (default motion) picks what on/off means: the motion vectors, or the sprite edges (the other one stays as the
Mod Options have it).
scan=N first turns the first-person view (viewpoint.input.Look.hold, not pinned: a test aid only) to N headings 360/N
degrees apart at the given pitch (degrees, negative = down), reads the crop once per heading after scanframes frames,
scores its foliage share (the same green mask as shimmer.py) and keeps the heading with the most foliage
(out/scan.txt). Then for each state in order (each state at most once) it switches the motion vectors on or off at a
frame boundary, lets the temporal history settle for warmup frames, then reads the centre crop of Viewpoint's
finished world image (no UI) at the end of each of the next frames consecutive frames (WorldRenderer.finish OnExit,
glReadPixels RGB8 from the output framebuffer) into out/<state>.rgb, frame times into out/<state>.times; with
velocity=1 (default) the first captured frame of each state also gets a velocity dump out/<state>_velocity.* (and
out/<state>_motion.* when Viewpoint upscales; see Velocity). At the end out/done.txt and the Mod Options state again.
### static int scanN, scanAt, scanWait, scanFrames;
heading scan
### static void scanStep() throws Exception
One scan frame read at the end of the frame: score it, turn to the next heading or settle on the best.
### static double foliageShare(byte[] rgb)
Share of the crop that shimmer.py's foliage mask would call foliage (green hue 50..170, saturation >= 0.12, value >= 0.06), every 2nd pixel.
### public static void finished()
WorldRenderer.finish OnExit.
### static byte[] readCrop() throws Exception
The centre crop (crop x crop, or less) of the finished world image, RGB8, rows bottom-up.

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/foliage/Motion.java
### public final class Motion
The runtime side, all on Viewpoint's render thread.

Velocity target: Viewpoint clears its velocity attachment (G-buffer colour 3) at the start of ModelPass.gbuffer and
draws only models into it; every other pixel gets its motion from depth reprojection. With motion on, the attachment is
cleared to the same zeros at the start of SurfacePass.gbuffer instead and kept enabled while the chunk meshes and the
grass draw (the edited gbuffer.frag writes velocity for plant draws and zeros for everything else; grass.frag for every
blade); ModelPass.gbuffer's own clear is then masked out (glColorMaski on buffer 3, restored before its first model
draw), so models still overwrite exactly where they are in front. Blood and everything later is drawn as before.

Previous frame: last frame's wind (SceneData.windX/windZ), sway time (FrameContext.time) and gusts, i.e. the inputs of
uPlantSway, and last frame's eye, kept per FrameContext.index; the unjittered view projection and the reprojection
matrix are Viewpoint's own for this frame (FrameContext.plainViewProjection, FrameContext.reprojection).
### static volatile Boolean forced;
switches
capture runs force on/off
### static boolean active;
for this frame
### static final float EDGE_CUT = edgeCut();
sprite edges (Rewrite.EDGE_HEADER): Mod Options tickbox "edges", the cut from -Dviewpointfoliagemotion.edgeCut (0 = off)
### static volatile Boolean forcedEdges;
capture runs with switch=edges
### static boolean edges;
for this frame
### static long curIndex = Long.MIN_VALUE;
previous / current frame sway inputs
### static Object targets;
per-frame velocity state
### static int prevBuffer, prevBytes;
previous bends of the grass blades
### public static void frame(Object ctx)
---- frame boundary (FrameUniforms.set OnExit)
### static void roll(Object ctx) throws Exception
---- previous frame's sway inputs
### public static void values(Object program, Object ctx)
FrameUniforms.values OnExit: the mesh program and the grass compute get their uniforms where uPlantSway gets its.
### static void velocity(boolean on) throws Exception
---- the velocity attachment over the surface pass
### frameOn = active && meshFrame >= curIndex - 1 && meshFrame != Long.MIN_VALUE;
the mesh program carried our uniforms last frame (a relink without edits stops that)
### public static void surfaceCleared()
ContactShadows.reads OnEnter: called right after SurfacePass.gbuffer's glClear, the G-buffer bound.
### public static void unmask()
Wireframe.apply OnEnter (ModelPass.gbuffer's first call after its clear) and ModelPass.gbuffer OnExit.
### public static void grow(Object grassPass)
---- grass: last frame's bends, one vec4 a blade slot, written by the compute and read by grass.vert
GrassPass.grow OnEnter: our buffer at binding 7 for the compute (always, so the edited compute never sees none).
### public static void grassDraw(Object storage, Object program)
GrassStorage.draw OnEnter, the grass program in use: its switch, matrices and our buffer.
### if (velocityOn)
grass not edited (or relinked without the edits) while the velocity target is on: this frame stays as
Viewpoint draws it (target off again, ModelPass clears it as usual)
### int block = GL43.glGetProgramResourceIndex(p.id(), GL_SHADER_STORAGE_BLOCK, "FmPrevBlades");
every draw, not cached by program id: a relink can hand back the same id with the block at binding 0

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/foliage/Pins.java
### final class Pins
SHA-256 of every Viewpoint 0.1.7 class this mod hooks or reads and every shader file its edits and its encoding rely on.
### static String mismatch() throws Exception
null when every pin matches, else what differs first.

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/foliage/Programs.java
### public final class Programs
Rewrites the three foliage programs as Viewpoint links them (SnippetSplice.splice) and relinks them unedited if a link fails.
### static String kind(Object name)
"gbuffer", "grass" or "grass blades", as Viewpoint names the program alone or with ", <pack> ..." after it.
### public static void prepare(Object[] args)
SnippetSplice.splice(name, sources, int) OnEnter: sources[] is replaced in place before Viewpoint splices and links it.
### public static int retry(Object begun)
ProgramLinker.finish OnExit with result 0: if the program carries our edits, link it again with them compiled out.

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/foliage/Rewrite.java
### public final class Rewrite
The in-memory shader edits, on the text Viewpoint has expanded (includes pasted in) just before it links a program.
Every edit is an exact anchor of Viewpoint 0.1.7's own lines; an anchor that is missing (or not there exactly as often
as expected) throws Miss and the program is left as Viewpoint wrote it.

All added code sits inside "#if VFM_MOTION". Setting the one line "#define VFM_MOTION 1" to 0 (what the link
fallback does if a driver refuses the edited program) gives back Viewpoint's program unchanged in what it computes.

What it adds (the same velocity encoding as Viewpoint's own model pass, lib/motion.glsl):
  velocity = vec4((clip.xy / clip.w - previousClip.xy / previousClip.w) * 0.5, previousClip.w, 1.0)
clip = the unjittered view projection times this frame's position, previousClip = the reprojection Viewpoint uses for
still pixels (FrameContext.reprojection, upscale_inputs.frag's uPrevious) times where the vertex was last frame: the
same plant or grass blade swayed with last frame's wind, time and gusts (and, for plant cards, last frame's eye).

1.2.0 also edits gbuffer.frag's foliage alpha hash (sprite edges, see EDGE_HEADER), inside the same "#if VFM_MOTION"
blocks; uFmEdgeCut = 0 (never set, or switched off) leaves Viewpoint's test as it was.
### static final String MESH_HEADER = """
---- the chunk-mesh G-buffer program (mesh.vert + gbuffer.frag): plant cards (bushes, flowers, game grass tiles, sprite trees)
### t = everyAtLeastOnce(t, "vec2 plantSway(vec3 foot, float h, float height, bool grass) {\n",
plant.glsl's sway reads uPlantSway; a local of the same name (set from fmSway) lets main run it with last frame's.
### static final String EDGE_HEADER = """
Sprite edges (1.2.0), gbuffer.frag only. With TAA or an upscaler on, Viewpoint draws foliage sprites (the game's
grass tiles as plant cards, bushes, sprite trees) only into the G-buffer and decides each pixel by an alpha hash
against the sprite's full alpha (lib/sample_sprite_body.glsl: c.a < alphaHashThreshold -> discard). The game's grass
art is mostly half-transparent (thin blades never reach alpha 1), so close up every blade is a field of random dots
that FSR cannot settle (the depth under each dot flips between blade and ground every frame, and FSR gets no
reactive mask): speckled, stair-stepped blades.

Instead, where the hash test runs (uFmEdgeCut > 0): the coverage is the sprite's alpha filtered bilinearly in
premultiplied form (four texels, clamped to the sprite's own rectangle), sharpened around uFmEdgeCut to one screen
pixel (a' = (a - cut) / fwidth(a) + 0.5), and only that one-pixel band is hashed. Up close the blade is solid with
a smooth, sub-pixel outline that moves with FSR's jitter like a polygon edge; far away (texels smaller than a pixel,
fwidth large) it falls back to a soft hash much like Viewpoint's. Cut 0.5 keeps the art's total coverage (measured on
e_newgrass_1: texels with alpha >= 0.5 cover 1.02 x the alpha sum). Magnified edge pixels also take their colour from
the premultiplied filter (only where the filtered alpha is below 1, so Lanczos / Sharp / Dithered keep the inside), so the transparent texels' colour (white in the game's tile pages, straight alpha) does not
bleed into the outline. The returned alpha stays Viewpoint's.
### static final String HASH_TEST = " if (uAlphaHash != 0 && plant) {\n if (c.a < alphaHashThreshold(anchor, ancho
sample_sprite_body.glsl's hash test; pasted twice into gbuffer.frag (sample_sprite.glsl: sampler2D, sample_wall.glsl: sampler2DArray).
### static final String GRASS_HEADER = """
---- Viewpoint's grass (grass.vert + grass.frag), its blades bent each frame by grass_blades.comp
### public static final int PREV_BINDING = 7;
The SSBO binding point of the previous frame's bends (one vec4 a blade slot); Viewpoint's own go up to 5, its model
uploads use 8 and 9, LayeredLooks 10. In the compute (#version 430) it is in the layout; grass.vert is #version 330
where a binding qualifier needs ARB_shading_language_420pack, so Motion.grassDraw sets the block's binding before
every grass draw (1.0.0 did that once per GL program id: a relink that gets the same id back, as Mesa reuses freed
names, would have left the block at binding 0 = Viewpoint's blade records, read as "last frame's bends").
### t = everyAtLeastOnce(t, " vec2 wind = uPlantSway.xy, along = -wind;\n",
grass_bend.glsl's windBend reads uPlantSway itself; a local of the same name lets main run it a second time with
last frame's wind, gusts and time (time is its own argument).
### static String header(String text, String decls)
---- helpers
MARK and the header after the #version line and the #extension lines that follow it.
### public static String disabled(String text)
What the link fallback sends instead: the same text with every addition compiled out.

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/foliage/Velocity.java
### public final class Velocity
Debug view of the motion vectors, as files (Viewpoint has no velocity debug view): for one frame,
 - prefix + "velocity.pfm/.ppm": Viewpoint's velocity target (G-buffer colour 3, render resolution) as the G-buffer passes
   left it, i.e. what this mod (foliage) and Viewpoint's model pass wrote. PFM (bottom-up rows, little-endian float RGB)
   = (x, y, w) of lib/motion.glsl: x, y = current minus previous position in UV units (multiply by the render width /
   height for pixels), w = 1 where a vector was written, 0 where upscale_inputs falls back to depth reprojection.
   PPM: black = no vector (w = 0); white = a vector of ~0; colour = moving, hue = screen direction (red = right,
   yellow-green = up, cyan = left, blue-violet = down), saturation = length / SCALE pixels (full at SCALE).
 - prefix + "motion.pfm/.ppm": what FSR / frame generation actually get (upscale_inputs.frag's fragMotion, read back
   from VendorInputs' framebuffer, colour attachment 1): PFM (x, y, 0) = previous minus current UV, FSR's convention.
   The PPM shows its negation, so it reads like velocity.ppm: still ground and walls white, swaying foliage coloured.
   Only when Viewpoint upscales (FSR / frame generation); with TAA alone there is no such image.
 - prefix + "velocity.txt": the numbers (vectors written, lengths in pixels, how well they line up with the wind's
   axis on screen, FSR input on still pixels), also logged as one line.
SCALE: -Dviewpointfoliagemotion.velocityScale=<pixels> (default 2).

Triggers: Capture (each state's first captured frame), the file Zomboid/Lua/ViewpointFoliageMotion-velocity.txt
holding an absolute directory (one dump on the next frame, the file is emptied), or the debug mode
-Dviewpointfoliagemotion.velocityDump=<dir>: a dump every 600 frames, at most 20 a session.
Reading happens on the render thread at the end of the frame; scoring and writing on a background thread.
### static volatile Path pending;
requested prefix, armed at the next frame boundary
### static Path armed;
this frame's prefix
### static float[] motion;
FSR input of the armed frame, if Viewpoint upscaled
### static void request(Path prefix)
Ask for a dump of the next frame; prefix = directory + file name start (e.g. /tmp/x/on_).
### static void frame(Object ctx)
FrameUniforms.set OnExit (via Motion.frame), after Capture.frame.
### public static void inputsDrawn(Object ctx, int fbo)
VendorInputs.draw OnExit: its framebuffer (depth, motion) has just been drawn; keep the motion of an armed frame.
### static float[] readFloats(int w, int h, int n, boolean pixels)
Reads w*h*n floats (n = 2: RG of the read framebuffer, n = 4: RGBA of the bound 2D texture), pack state restored.
### public static void finished()
WorldRenderer.finish OnExit: the velocity target still holds this frame (it is cleared in the next G-buffer pass).
### static float[] windAxis(viewpoint.render.FrameContext c, int w, int h)
The wind's axis on screen in pixels (unit length), about 4 m ahead of the eye at grass height; null if unknown.
### static String stats(float[] v, float[] m, int w, int h, float[] axis)
Numbers of one dump (also used by the offline test): see the class comment.
### static void pfm(Path p, float[] a, int stride, int w, int h, float sign) throws IOException
PFM colour (PF): rows bottom-up as GL reads them, little-endian (scale -1); channels x, y and (stride 4) w or 0.
### static void ppm(Path p, float[] a, int stride, int w, int h, float sign, boolean flagged) throws IOException
The colour picture (top-down rows): see the class comment. sign -1 shows FSR's previous-minus-current as velocity.
### static int colour(float x, float y)
White for ~0, saturated hue for SCALE pixels or more; hue = direction (0 = +x right, 120 = +y up on screen).

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/outline/Latch.java
### public final class Latch
When each object last got setOutlineHighlight(true). Viewpoint 3D clears an outline only once this is
older than WINDOW_MS; setOutlineHighlight(false) drops the entry (and the game clears the flag itself).
The latch never turns an outline on: it only delays Viewpoint's per-capture clear of a flag that is set.
### public static void onSet(Object self, Object[] args)
Advice entry (inlined into IsoObject.setOutlineHighlight(boolean) and (int, boolean)): last argument is "on".
### static synchronized int offNow(int playerIndex, long now)
latched objects (true within the window) whose flag for this player is already off; Viewpoint captures them dark

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/outline/Shown.java
### public final class Shown
Replacement for Viewpoint's Characters.outlinesShown(IsoCell, int), which runs right after each 3D character
capture and clears every outline that was not set during the current UI render/update pass (the same rule as
the game's IsoMovingObject.renderlast). Same rule here, plus: an outline set to true within Latch.WINDOW_MS stays.
### public static boolean run(IsoCell cell, int playerIndex)
true: handled, Viewpoint's original is skipped. false: run the original.

## payload/localmods/ViewpointFixes/source/src/viewpointfixes/outline/Stats.java
### public final class Stats
In-game diagnosis for a flicker that remains: every 10 s while outlines are in use (at most MAX_LINES lines per
session) one log line with what happened to them. "off before capture" = an outline set to true within the
window but already off when Viewpoint captured: something turned it off explicitly (see "false while latched"
and its first caller) or never set it for that player; the latch can't keep those.
### private static String caller()
first frame outside this mod and IsoObject.setOutlineHighlight

## payload/localmods/ViewpointFixes/source/test/viewpointfixes/foliage/EncodingTest.java
### public class EncodingTest
Offline checks of the motion vectors against Viewpoint's own (no GL, no game):
 1. encoding vs the model pass: model.vert / lib/motion.glsl / upscale_inputs.frag text, and in the bytecode which
    FrameContext matrices each pass uses (model pass: plain + previous; FSR inputs: inverse plain + reprojection;
    TemporalPass.begin builds reprojection = previous plain view-projection shifted by the origin change); the foliage
    programs use plain + reprojection, never the jittered viewProjection.
 2. numbers (joml, the same maths as the GLSL): a still point gets from our formula exactly what upscale_inputs'
    depth path gives it (jitter, origin shift included), so a foliage pixel at rest blends in with its surroundings;
    a point moving right / up gives velocity +x / +y and FSR motion -x / -y (it points back to where the history is).
 3. previous-bends ordering: GrassPass.draw grows the blades (the compute writes this frame's bends and, in the same
    invocation and slot, the bends with last frame's inputs) then barriers, then draws; GrassPass.grow sets the frame
    uniforms (our uFmPrevSway) before its dispatches; the slot written is the one grass.vert reads.
 4. frame keying (Motion.roll on real FrameContext / SceneData objects): prev = the previous frame index's inputs; a
    second call in the same frame changes nothing; a skipped index, an origin shift or previousValid=false give
    prev = now (no foliage motion that frame rather than a wrong one).
### static List<String> uses(String cls, String method) throws Exception
Field reads (FrameContext.x) and calls, in order, of a method's bytecode.
### Motion.init();
ours: Motion sets uFmPlain from plainViewProjection and uFmReproject from reprojection (fields resolved in init)
### Matrix4f reproject = new Matrix4f(prevPlain).translate(ox, 0, oz);
TemporalPass.begin: reprojection = previous plain view-projection translated by (origin - previous origin)
### Matrix4f still = new Matrix4f(proj).mul(new Matrix4f(prevView).translate(ox, 0, oz));
(a) camera still in the world, origin shifted: this frame's view of scene coords = last frame's after the shift
### Matrix4f moved = new Matrix4f(proj).mul(new Matrix4f().rotateY(0.02f * (r.nextFloat() - 0.5f)).translateLocal(
(b) camera turned and moved a little as well
### worstShift = Math.max(worstShift, Math.hypot((c0.x / c0.w - pc.x / pc.w) * 0.5 * w, (c0.y / c0.w - pc.y / pc.w
(a) still camera, still point, shifted origin: our velocity must be 0
### float vx = (c.x / c.w - pc.x / pc.w) * 0.5f, vy = (c.y / c.w - pc.y / pc.w) * 0.5f;
(b) ours for a still point vs upscale_inputs' depth path (uUnproject = inverse plain, uPrevious = reprojection)
### Matrix4f view = new Matrix4f(prevView).translate(ox, 0, oz);
(c) sign: the point moved 5 cm to the camera's right and up since last frame (camera still)
### for (int f = 1; f <= 4; f++)
frames 1..4, inputs = frame number
### Motion.roll(ctx);
the same frame again (a second program's first use): nothing moves
### index.setLong(ctx, 6);
frame 5 skipped
### s.originX = 1;
origin shift
### valid.setBoolean(ctx, false);
camera cut
### v[i * 4] = 1f / w;
1 px right

## payload/localmods/ViewpointFixes/source/test/viewpointfixes/foliage/HookCheck.java
### public class HookCheck
Every @Patch target exists exactly once by name in this Viewpoint.jar (ZombieBuddy matches by name), and every member read by reflection exists.

## payload/localmods/ViewpointFixes/source/test/viewpointfixes/foliage/RewriteTest.java
### public class RewriteTest
Offline: expands Viewpoint's own shader sources with its own Shaders.load (includes, generated constants, the
#version/#extension header, exactly as the game builds them before SnippetSplice), applies the rewrite, then checks
 - every expected addition is there,
 - with every "#if VFM_MOTION ... #endif" block and the header removed, the text is byte for byte Viewpoint's,
 - Rewrite.disabled (the link fallback) only flips the one define,
 - a second rewrite is refused,
and writes original / rewritten / disabled texts to the out dir for glslangValidator (build.sh).
### int edit = gb.indexOf("float fmKeep = c.a;");
the edit sits where the variables it reads exist: inside sampleSprite, after own / near / lo / hi / read, before the hash test
### String motion = new String(ClassLoader.getSystemResourceAsStream("viewpoint/shaders/lib/motion.glsl").readAllB
the velocity encoding is Viewpoint's model pass's, character for character

## payload/localmods/ViewpointFixes/source/test/viewpointfixes/outline/OutlineFixCheck.java
### public class OutlineFixCheck
Offline check, no game start: both patches through ZombieBuddy's own PatchTransformer and Byte Buddy Advice
against the real IsoObject and Viewpoint Characters classes (applied + JVM-verified), the setter advice on a
probe, and the latch against a simulated Viewpoint frame loop.
Needs the game jar, ZombieBuddy.jar, Viewpoint.jar and this mod on the class path.
### static byte[] patched(String cls, String method, Advice advice)
Redefines a class from the class path without loading it, then runs the JVM verifier rules on the result.
### static int simulate(boolean latch, long[] offAfterStop, long[] offAfterFalse)
Old Viewpoint rule vs the latch, at 144 captures/s with a mod that sets the outline (marked "not during UI",
so stale for Viewpoint's clear) at 30 Hz, like A-Life's OnTick under Viewpoint's fixed tick or
SafeUserLogin's text bar. Returns captures without outline in the first 2 s.
### while (nextSet <= t)
setter calls due before this capture
### if (t >= falseAt - frame && t < falseAt) { flag = true; Latch.set(npc, true, t); }
one last set just before false
### if (t >= falseAt && t < falseAt + frame) { flag = false; Latch.set(npc, false, t); }
explicit setOutlineHighlight(false)
### boolean shown = flag;
the capture reads the flag
### if (flag && !(latch && Latch.recent(npc, t))) flag = false;
Viewpoint's clear after the capture
### Class<?> probe = new ByteBuddy().subclass(Object.class).name("viewpointfixes.outline.SetterProbe")
Setter advice behaviour on a probe with both overload shapes.
### Latch.clear();
Window and pruning.
### long[] stop = new long[1], off = new long[1];
Frame loop.
### Stats.reset();
Stats line (in-game diagnosis): one line per period with the counts, capped per session.
### Latch.set(s1, false, 2);
false right after a true: what the latch can't keep
### Latch.set(s1, true, Stats.PERIOD_NANOS + 5);
period over -> line

## payload/localmods/ViewpointFixes/source/test/viewpointfixes/qolloot/LootFixCheck.java
### public class LootFixCheck
Offline check, no game: runs both patch classes through ZombieBuddy's own PatchTransformer and Byte Buddy Advice
against Viewpoint's real LootTargets, verifies the result, and runs the advice on a probe method.
Needs the game jar, ZombieBuddy.jar, Viewpoint.jar, ProjectViewpointQOL.jar and this mod on the class path.
### static Advice zbAdvice(Class<?> patch) throws Exception
ZombieBuddy's PatchTransformer (ZB annotations -> Byte Buddy Advice annotations), with a capturing
Instrumentation like ZBBatch's preTransformPatches: the in-game path redefines the patch class with these bytes.
### static Class<?> loadPatched(byte[] bytes) throws Exception
Loads the patched LootTargets (and its own package) in a throwaway loader and initializes it: runs the verifier.
### Advice qol = zbAdvice(Class.forName("viewpointqol.Patch_LootTargetsContainers"));
1. QOL's own patch: Byte Buddy must refuse it (the bug).
### check(Fix.ON, "Fix.ON: the QOL patch class on the class path is the known broken one");
2. This mod's patch: applies, and the patched class passes the verifier.
### check(Modifier.isPublic(Fix.class.getModifiers()) && Modifier.isPublic(Fix.class.getField("ON").getModifiers()
3. Advice only uses public members of this mod (it is inlined into Viewpoint's class).
### Class<?> frame = Class.forName("viewpoint.core.Frame", false, LootFixCheck.class.getClassLoader());
4. Behaviour: same advice on a probe with aimed()'s exact signature, in Viewpoint's package and loader.

## payload/localmods/ViewpointFixes/source/tools/shimmer.sh
### Usage and procedure
Foliage shimmer A/B for ViewpointFixes' FoliageMotion module (copied from ViewpointFoliageMotion 1.2.0's tools): ONE headless boot into the test save, first person, player held still,
wind forced by an AutoLua probe (ClimateManager wind-intensity override, weather stopped), then the add-on's own capture
(trigger file Zomboid/Lua/ViewpointFoliageMotion-capture.txt) records --frames consecutive frames of Viewpoint's
finished world image (centre crop, no UI) with the motion vectors ON, switches them OFF at a frame boundary, lets the
temporal history settle (--warmup frames) and records again (order --order). shimmer.py then scores per-pixel temporal
noise over the foliage pixels.
Scene (run1 of 2026-11-23 was dark and faced tree trunks, 15% foliage): the probe sets the time of day (--hour, default
12) and can teleport to a known grassy square (--at X,Y[,Z]); the first capture plan turns the first-person view to
--scan headings (default 8) and keeps the one with the most foliage on screen (logged, OUT/run1/scan.txt), looking
--pitch degrees down. Gusts make one on/off pair unreliable (see shimmer.py "sway"), so --runs N (default 2) captures
N times in the same session, the order swapped every run (on,off then off,on), each into OUT/run<k>; every state's
first captured frame also gets a velocity dump (<state>_velocity.ppm/.pfm, <state>_motion.* = FSR's input; see
Velocity.java). shimmer.py scores each run and the mean (OUT/score.txt).
Same rules and patterns as zomboid-eltf/tools/memtest/horde.sh: only inside own lock (PZLOCK_NAME=<pzlock session>,
required); run as a background job; hard cap; the test save is backed up and restored; trigger files emptied at the end;
GPU watch (journalctl -k: 'ring .*timeout|GPU reset\(|MODE1' -> close the game, restore, exit 7).
Needs (not done here: mod lists / approvals are the user's): ViewpointFixes installed (local mod or Workshop), in the
mod list (Zomboid/mods/default.txt) after Viewpoint and the standalone ViewpointFoliageMotion NOT in it (the script refuses otherwise: the module would stay off), its jar approved in ~/.zombie_buddy/mod_approvals.json; AutoLoadSave
enabled (Auto*.txt triggers). Mod Options tickbox may be on or off: the capture forces each state itself.

--switch edges (1.2.0): on/off switches the sprite edges instead of the motion vectors (those stay as Mod Options has them).

usage: MEMTEST=<.../tools/memtest> PZLOCK_NAME=<s> shimmer.sh [--wind 0..1] [--frames N] [--warmup N] [--crop PX] [--order on,off] [--settle S] [--switch motion|edges]
[--runs N] [--hour H] [--at X,Y[,Z]] [--scan N] [--pitch DEG] [--res WxH] [--out DIR] [--dry-run]
order_of: run k uses the order as given for odd k, swapped for even k.
plan_of: run k -> capture plan line. probe: AutoLua file, first line = delay in seconds, then the Lua chunk (its tile count is informative only and never fails the probe).
CAP: captures run well above 10 fps. The final sleep 2: the velocity dumps are written on a background thread.
Log patterns: [ViewpointFixes/FoliageMotion] active / WARN capture (were [ViewpointFoliageMotion]); trigger file name unchanged.

## payload/localmods/ViewpointFixes/source/tools/shimmer.py
### Scores and usage
GL rows are bottom-up
rows bottom-up, as written
grey half: not foliage
velocity PFM as Velocity.java writes it (PF, little-endian, rows bottom-up): 2 of 8 pixels with a 1 px vector
velocity.pfm: x, y = current - previous UV, z = w (1 = vector written); motion.pfm: x, y = previous - current UV.
Shimmer score from a FoliageMotion capture directory (shimmer.sh / the add-on's capture).

Input: <dir>/done.txt (w= h= frames= order=), <dir>/<state>.rgb (frames x h x w x RGB8, rows bottom-up), <dir>/<state>.times.
Per state, on luminance Y (Rec. 709, 0..255):
  std     mean over the mask of each pixel's temporal standard deviation (all change: sway motion + shimmer)
  flicker mean over the mask of |Y[t+1] - 2 Y[t] + Y[t-1]|, the temporal second difference: frame-to-frame alternation
          (aliasing crawl, alpha-test popping, history rejection), mostly blind to smooth sway. This is the shimmer score.
  sway    mean over the mask of the temporal std of a 9-frame moving average: the slow part (the sway itself). Gusts
          come and go (grass_bend.glsl: travelling gust noise), so a state captured in a lull sways less and flickers
          less whatever the motion vectors do: run1 of 2026-11-23 had OFF grass almost still (sway 0.98) and ON grass
          swaying (2.03) in the same spot. Compare states only when their sway is similar; average runs with the order
          swapped (shimmer.sh --runs 2).
  fast    mean |Y - 9-frame moving average|: the part of the change faster than the sway.
Mask "foliage": pixels whose mean colour (over all states) is green-ish: hue 50..170 deg, saturation >= 0.12, value >= 0.06.
Mask "all": the whole crop. Writes PPM previews: <state>_first.ppm, <state>_flicker.ppm (same scale for all states), mask.ppm.
Velocity dumps (<state>_velocity.pfm / _motion.pfm from the add-on, see Velocity.java) are summarised from the PFM
itself (an independent check of the add-on's own <state>_velocity.txt): share of pixels with a vector, lengths in pixels.
Usage: python3 -I shimmer.py <capture dir>
       python3 -I shimmer.py --summary <run dir> [<run dir> ...]   per-run scores and their mean (shimmer.sh --runs)
       python3 -I shimmer.py --velocity <file.pfm>                 numbers of one velocity / motion dump
       python3 -I shimmer.py --selftest

## payload/localmods/ViewpointFixes/source/tools/edges_model.py
### Model
Offline model of the G-buffer foliage alpha test: Viewpoint's hash on the full alpha vs 1.2.0 sprite edges.
usage: python3 -I edges_model.py <sprite.png (RGBA, straight alpha, e.g. e_newgrass_1_0 cut from Tiles2x.pack)> <magnification> <out.png>
Prints coverage / alpha sum, mean per-pixel frame variance of the hash decision and the share of undecided pixels;
writes one frame and the 64-frame mean of both on a bright ground (left Viewpoint, right 1.2.0). Needs numpy, Pillow.
texel space
GL_LINEAR on straight alpha
fwidth like a 2x2 quad: |dFdx| + |dFdy| from the quad's pixels
one frame of each + the 64-frame mean, on a bright ground colour, with blade colour = straight vs premultiplied filtered
