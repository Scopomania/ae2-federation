#!/usr/bin/env bash
# Boots a headless dedicated server of the Forge 1.20.1 port, plays forge-1.20.1/smoke-commands.txt into it over RCON and
# reports what the server answered. It exercises registries, capability wiring, networking, config, data loading, the AE2
# mixins and, through the commands, block entity creation, saving and removal. No GUI, no rendering.
# Usage (repo root, e.g. in a Codespace):  forge-1.20.1/smoke-server.sh
set -u
cd "$(dirname "$0")/.."
LOG=/tmp/ae2f-smoke.log
COMMANDS=forge-1.20.1/smoke-commands.txt
RCON_PORT=25575
RCON_PASSWORD=smoke

# AE2 15.4 requires GuideME at runtime; find its newest 20.1.x build for Forge 1.20.1 on Modrinth.
GUIDEME_ID=$(curl -fsSG 'https://api.modrinth.com/v2/project/guideme/version' \
    --data-urlencode 'game_versions=["1.20.1"]' --data-urlencode 'loaders=["forge"]' \
    | jq -r '[.[] | select(.version_number | startswith("20.1"))][0] | "\(.id) \(.version_number)"')
echo "GuideME: ${GUIDEME_ID:-NOT FOUND} (Modrinth version id, version number)"
GUIDEME_VERSION_ID=${GUIDEME_ID%% *}
GRADLE=(./gradlew :forge-1.20.1:runServer --no-daemon --dependency-verification=off -Pguideme_modrinth_version="$GUIDEME_VERSION_ID")

find_run_dir() {
    local file
    file=$(find forge-1.20.1 -name eula.txt -not -path '*/build/*' | head -1)
    [ -n "$file" ] && dirname "$file"
}

# The first start only creates eula.txt and server.properties, then stops on the EULA.
RUN_DIR=$(find_run_dir)
if [ -z "$RUN_DIR" ]; then
    timeout 400 "${GRADLE[@]}" < /dev/null > "$LOG" 2>&1
    RUN_DIR=$(find_run_dir)
fi
if [ -z "$RUN_DIR" ]; then
    echo "Could not find the server run directory (no eula.txt). See $LOG"; exit 1
fi
echo "eula=true" > "$RUN_DIR/eula.txt"

props="$RUN_DIR/server.properties"
set_prop() {
    if grep -q "^$1=" "$props" 2>/dev/null; then sed -i "s|^$1=.*|$1=$2|" "$props"; else echo "$1=$2" >> "$props"; fi
}
set_prop enable-rcon true
set_prop rcon.port "$RCON_PORT"
set_prop rcon.password "$RCON_PASSWORD"
set_prop broadcast-rcon-to-ops false
set_prop online-mode false

# Stdin is closed on purpose: the console just ends, and the server runs until RCON tells it to stop.
timeout 900 "${GRADLE[@]}" < /dev/null > "$LOG" 2>&1 &
server=$!

for _ in $(seq 1 300); do
    grep -q -e 'Done (' -e 'Failed to start the minecraft server' "$LOG" 2>/dev/null && break
    kill -0 "$server" 2>/dev/null || break
    sleep 2
done

rcon_status=skipped
if grep -q 'Done (' "$LOG"; then
    echo "== command results (over RCON) =="
    python3 forge-1.20.1/smoke-rcon.py 127.0.0.1 "$RCON_PORT" "$RCON_PASSWORD" "$COMMANDS"
    rcon_status=$?
else
    kill "$server" 2>/dev/null
fi
wait "$server" 2>/dev/null

echo "== problems in the server log =="
grep -nE 'ERROR|FATAL|Exception|Mixin apply|InvalidInjection|crash-report|Caused by' "$LOG" \
    | grep -vE 'TRACE|Loading Class|NoSuchFileException|UnixException' | head -40
echo "== verdict =="
if grep -q 'Done (' "$LOG"; then
    echo "SERVER STARTED (commands: $([ "$rcon_status" = 0 ] && echo 'all answered cleanly' || echo "see above, code $rcon_status"))"
else
    echo "SERVER DID NOT FINISH STARTING (full log: $LOG)"
fi
