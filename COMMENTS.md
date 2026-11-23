# Notes

Since 2026-11-19 this folder holds only the pack content: settings/, presets/, zbbatch/, payload/. Everything else (mod sources, 108600 originals, approvals list, unsent author reports, Workshop upload tools, old installer, trash, friends' logs) is in Projects/.zomboid-backups/claude-20261119-fixes-uploaded-mods (fixes-rest/, source-*, plus the mod folders). All fix mods are on the Workshop (friends-only).

Build the zip (top folder PZ-Fixes/, the layout Custom Packs expects):

    cd .. && rm -f ~/Desktop/PZ-Fixes.zip && python3 -I -c "import os,zipfile;z=zipfile.ZipFile(os.path.expanduser('~/Desktop/PZ-Fixes.zip'),'w',zipfile.ZIP_DEFLATED);[z.write(os.path.join(r,f),os.path.join('PZ-Fixes',os.path.relpath(os.path.join(r,f),'Zomboid-B42.22-Fixes'))) for d in ('settings','presets','zbbatch','payload') for r,_,fs in os.walk(os.path.join('Zomboid-B42.22-Fixes',d)) for f in sorted(fs)];z.write('Zomboid-B42.22-Fixes/README.md','PZ-Fixes/README.md');z.close()"

zbbatch/zbbatch.properties is a copy of ~/.zombie_buddy/zbbatch.properties calibrated on kata's PC; re-copy it after the Java mod list changes (ZBBatch recalibrates by itself when its fingerprint doesn't match).

payload/localmods/ViewpointBarricade (added 2026-11-20): GamerKreep's "Viewpoint - Barricade Fix" 1.0.11, copied from ~/Zomboid/mods. Its Workshop item 3815033191 was deleted from Steam and Barricades3D needs it, so friends get it as a local mod; payload/approvals.txt approves its jar (cbe10c19). Drop both if it comes back on the Workshop.

payload/localmods/ViewpointQOLLootFix (added 2026-11-22): our ZombieBuddy mod for Project Viewpoint QOL (Workshop 3811256204). QOL's Patch_LootTargetsContainers asks for a writable Object return value (`@Patch.Return(readOnly=false)`) on LootTargets.aimed, which returns LootTargets$Target, so Byte Buddy refuses it ("Cannot assign class java.lang.Object to class viewpoint.interact.LootTargets$Target") and "See All Nearby Containers" never reaches the aimed target. This mod applies the same exit code with a read-only return value (QOL's code only reads it). It is pinned to the sha256 of QOL's broken class and turns itself off if QOL changes it; drop it once QOL fixes the patch. Build and offline check: `source/build.sh` (needs JDK, GAME_DIR, WORKSHOP); the jar is reproducible, so its approval in payload/approvals.txt only changes when the code does. Adding it changes the Java mod list, so zbbatch/zbbatch.properties needs a recalibrated copy.
