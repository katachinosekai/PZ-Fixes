#!/usr/bin/env bash
# Builds ViewpointQOLLootFix.jar into ../42/media/java/client and runs the offline check (no game start).
# Set: JDK (JDK 25 bin dir), GAME_DIR (.../ProjectZomboid/projectzomboid), WORKSHOP (.../steamapps/workshop/content/108600)
set -euo pipefail
cd "$(dirname "$0")"
: "${JDK:?set JDK}" "${GAME_DIR:?set GAME_DIR}" "${WORKSHOP:?set WORKSHOP}"

GAME_JAR="$GAME_DIR/projectzomboid.jar"
ZB_JAR="$GAME_DIR/ZombieBuddy.jar"
VP_JAR="$WORKSHOP/3809306528/mods/Viewpoint/42/media/java/client/Viewpoint.jar"
QOL_JAR="$WORKSHOP/3811256204/mods/ProjectViewpointQOL/42/media/java/client/ProjectViewpointQOL.jar"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

mkdir -p "$OUT/classes" "$OUT/test-classes"
"$JDK/javac" --release 25 -cp "$GAME_JAR:$ZB_JAR:$VP_JAR:$QOL_JAR" -d "$OUT/classes" $(find src -name '*.java')
printf 'Implementation-Title: ViewpointQOLLootFix\nImplementation-Version: 1.0.0\n' > "$OUT/manifest.txt"
# Fixed timestamps so the jar (and its approval hash) is the same on every build.
"$JDK/jar" --create --date 2026-11-22T00:00:00Z --file "$OUT/ViewpointQOLLootFix.jar" --manifest "$OUT/manifest.txt" -C "$OUT/classes" viewpointqollootfix

"$JDK/javac" --release 25 -cp "$GAME_JAR:$ZB_JAR:$VP_JAR:$QOL_JAR:$OUT/classes" -d "$OUT/test-classes" $(find test -name '*.java')
"$GAME_DIR/jre64/bin/java" \
    -cp "$GAME_JAR:$ZB_JAR:$VP_JAR:$QOL_JAR:$OUT/ViewpointQOLLootFix.jar:$OUT/test-classes" viewpointqollootfix.LootFixCheck

cp "$OUT/ViewpointQOLLootFix.jar" ../42/media/java/client/
sha256sum ../42/media/java/client/ViewpointQOLLootFix.jar
