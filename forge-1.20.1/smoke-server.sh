#!/usr/bin/env bash
# Boots a headless dedicated server of the Forge 1.20.1 port, feeds it forge-1.20.1/smoke-commands.txt, and reports.
# It exercises registries, capability wiring, networking, the config, data loading, the AE2 mixins, and (through the
# commands) block entity creation, saving and removal. No GUI, no rendering.
# Usage (repo root, e.g. in a Codespace):  forge-1.20.1/smoke-server.sh
set -u
cd "$(dirname "$0")/.."
LOG=/tmp/ae2f-smoke.log
FIFO=/tmp/ae2f-console.fifo
COMMANDS=forge-1.20.1/smoke-commands.txt

# AE2 15.4 requires GuideME at runtime; find its newest 20.1.x build for Forge 1.20.1 on Modrinth.
GUIDEME_ID=$(curl -fsSG 'https://api.modrinth.com/v2/project/guideme/version' \
    --data-urlencode 'game_versions=["1.20.1"]' --data-urlencode 'loaders=["forge"]' \
    | jq -r '[.[] | select(.version_number | startswith("20.1"))][0] | "\(.id) \(.version_number)"')
echo "GuideME: ${GUIDEME_ID:-NOT FOUND} (Modrinth version id, version number)"
GUIDEME_VERSION_ID=${GUIDEME_ID%% *}
GRADLE=(./gradlew :forge-1.20.1:runServer --no-daemon --dependency-verification=off -Pguideme_modrinth_version="$GUIDEME_VERSION_ID")

accept_eula() { find forge-1.20.1 -name eula.txt -exec sed -i 's/eula=false/eula=true/' {} +; }

run() {
    rm -f "$FIFO"; mkfifo "$FIFO"
    timeout 900 "${GRADLE[@]}" < "$FIFO" > "$LOG" 2>&1 &
    local server=$!
    exec 3> "$FIFO"                       # keeps the server's console open while we type into it
    for _ in $(seq 1 300); do             # up to ~10 minutes for Gradle plus the boot
        grep -q -e 'Done (' -e 'agree to the EULA' -e 'Failed to start the minecraft server' "$LOG" 2>/dev/null && break
        kill -0 "$server" 2>/dev/null || break
        sleep 2
    done
    if grep -q 'Done (' "$LOG" 2>/dev/null; then
        while IFS= read -r line; do
            case "$line" in ''|\#*) continue ;; esac
            echo "$line" >&3
            sleep 3
        done < "$COMMANDS"
        sleep 5
        echo 'save-all' >&3
        sleep 4
    fi
    echo 'stop' >&3
    exec 3>&-
    wait "$server"
}

run
if grep -q 'agree to the EULA' "$LOG"; then
    accept_eula
    run
fi

echo "== command results =="
# Everything the server printed after it finished starting, minus noise.
awk '/Done \(/{on=1} on' "$LOG" | grep -vE 'TRACE|^\s+at |Loading Class' | grep -E '\[Server thread/' | sed -E 's/^\[[0-9:]+\] \[Server thread\/[A-Z]+\] \[[^]]*\]: //' | head -80
echo "== problems =="
grep -nE 'ERROR|FATAL|Exception|Mixin apply|InvalidInjection|crash-report|Caused by' "$LOG" | grep -vE 'TRACE|Loading Class|server.properties|NoSuchFileException|UnixException' | head -40
echo "== verdict =="
if grep -q 'Done (' "$LOG"; then echo "SERVER STARTED"; else echo "SERVER DID NOT FINISH STARTING (full log: $LOG)"; fi
