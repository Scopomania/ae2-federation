#!/usr/bin/env bash
# Boots a headless dedicated server of the Forge 1.20.1 port and says whether it reached "Done".
# It exercises registries, capability wiring, networking, the config and the AE2 mixins (no GUI, no rendering).
# Usage (repo root, e.g. in a Codespace):  forge-1.20.1/smoke-server.sh
set -u
cd "$(dirname "$0")/.."
LOG=/tmp/ae2f-smoke.log

# AE2 15.4 requires GuideME at runtime; find its newest 20.1.x build for Forge 1.20.1 on Modrinth.
GUIDEME_ID=$(curl -fsSG 'https://api.modrinth.com/v2/project/guideme/version' \
    --data-urlencode 'game_versions=["1.20.1"]' --data-urlencode 'loaders=["forge"]' \
    | jq -r '[.[] | select(.version_number | startswith("20.1"))][0] | "\(.id) \(.version_number)"')
echo "GuideME: ${GUIDEME_ID:-NOT FOUND} (Modrinth version id, version number)"
GUIDEME_VERSION_ID=${GUIDEME_ID%% *}
GRADLE=(./gradlew :forge-1.20.1:runServer --no-daemon --dependency-verification=off -Pguideme_modrinth_version="$GUIDEME_VERSION_ID")

accept_eula() { find forge-1.20.1 -name eula.txt -exec sed -i 's/eula=false/eula=true/' {} +; }
# The console reads "stop" once it is up; if the line arrives early it simply waits in the pipe.
run() { ( sleep 60; echo stop ) | timeout 480 "${GRADLE[@]}" > "$LOG" 2>&1; }

run
if grep -q 'agree to the EULA' "$LOG"; then
    accept_eula
    run
fi
echo "== key lines =="
grep -nE 'Done \(|ERROR|FATAL|Exception|Mixin apply|InvalidInjection|Caused by|crash-report|ae2federation|AE2F' "$LOG" \
    | grep -vE 'TRACE|Loading Class' | head -80
echo "== verdict =="
if grep -q 'Done (' "$LOG"; then echo "SERVER STARTED"; else echo "SERVER DID NOT FINISH STARTING (full log: $LOG)"; fi
