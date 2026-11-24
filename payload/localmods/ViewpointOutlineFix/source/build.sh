#!/usr/bin/env bash
# Builds ViewpointOutlineFix.jar into ../42/media/java/client and runs the offline checks (no game start).
# Set: JDK (JDK 25 bin dir), GAME_DIR (.../ProjectZomboid/projectzomboid), WORKSHOP (.../steamapps/workshop/content/108600)
set -euo pipefail
cd "$(dirname "$0")"
: "${JDK:?set JDK}" "${GAME_DIR:?set GAME_DIR}" "${WORKSHOP:?set WORKSHOP}"

GAME_JAR="$GAME_DIR/projectzomboid.jar"
ZB_JAR="$GAME_DIR/ZombieBuddy.jar"
VP_JAR="$WORKSHOP/3809306528/mods/Viewpoint/42/media/java/client/Viewpoint.jar"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT

mkdir -p "$OUT/classes" "$OUT/test-classes"
"$JDK/javac" --release 25 -cp "$GAME_JAR:$ZB_JAR:$VP_JAR" -d "$OUT/classes" $(find src -name '*.java')
printf 'Implementation-Title: ViewpointOutlineFix\nImplementation-Version: 1.0.1\n' > "$OUT/manifest.txt"
# Fixed timestamps so the jar (and its approval hash) is the same on every build.
"$JDK/jar" --create --date 2026-11-23T00:00:00Z --file "$OUT/ViewpointOutlineFix.jar" --manifest "$OUT/manifest.txt" -C "$OUT/classes" viewpointoutlinefix

"$JDK/javac" --release 25 -cp "$GAME_JAR:$ZB_JAR:$VP_JAR:$OUT/classes" -d "$OUT/test-classes" $(find test -name '*.java')
"$GAME_DIR/jre64/bin/java" -cp "$GAME_JAR:$ZB_JAR:$VP_JAR:$OUT/ViewpointOutlineFix.jar:$OUT/test-classes" viewpointoutlinefix.OutlineFixCheck
"$GAME_DIR/jre64/bin/java" -cp "$GAME_JAR:$ZB_JAR:$VP_JAR:$OUT/classes:$OUT/test-classes" viewpointoutlinefix.AdviceAccessTest "$OUT/classes/viewpointoutlinefix"

# Signatures the patches target (ZombieBuddy matches by name and the advice's argument types).
sig() { "$JDK/javap" -p -cp "$1" "$2" | grep -qF "$3" && echo "OK   $2: $3" || { echo "FAIL $2: $3"; exit 1; }; }
sig "$VP_JAR" viewpoint.models.Characters "public static void outlinesShown(zombie.iso.IsoCell, int)"
sig "$GAME_JAR" zombie.iso.IsoObject "public final void setOutlineHighlight(boolean)"
sig "$GAME_JAR" zombie.iso.IsoObject "public final void setOutlineHighlight(int, boolean)"

cp "$OUT/ViewpointOutlineFix.jar" ../42/media/java/client/
sha256sum ../42/media/java/client/ViewpointOutlineFix.jar
