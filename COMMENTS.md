# Notes

Since 2026-11-19 this folder holds only the pack content: settings/, presets/, zbbatch/. Everything else (mod sources, 108600 originals, approvals list, unsent author reports, Workshop upload tools, old installer, trash, friends' logs) is in Projects/.zomboid-backups/claude-20261119-fixes-uploaded-mods (fixes-rest/, source-*, plus the mod folders). All fix mods are on the Workshop (friends-only).

Build the zip (top folder PZ-Fixes/, the layout Custom Packs expects):

    cd .. && rm -f ~/Desktop/PZ-Fixes.zip && python3 -I -c "import os,zipfile;z=zipfile.ZipFile(os.path.expanduser('~/Desktop/PZ-Fixes.zip'),'w',zipfile.ZIP_DEFLATED);[z.write(os.path.join(r,f),os.path.join('PZ-Fixes',os.path.relpath(os.path.join(r,f),'Zomboid-B42.22-Fixes'))) for d in ('settings','presets','zbbatch') for r,_,fs in os.walk(os.path.join('Zomboid-B42.22-Fixes',d)) for f in sorted(fs)];z.write('Zomboid-B42.22-Fixes/README.md','PZ-Fixes/README.md');z.close()"

zbbatch/zbbatch.properties is a copy of ~/.zombie_buddy/zbbatch.properties calibrated on kata's PC; re-copy it after the Java mod list changes (ZBBatch recalibrates by itself when its fingerprint doesn't match).
