#!/usr/bin/env bash
set -euo pipefail

# ── Configuration ──────────────────────────────────────────────────────────────
REPO_DIR="$(cd "$(dirname "$0")" && pwd)"   # directory this script lives in
COMPOSE_FILE="$REPO_DIR/docker-compose.yml"
SERVICE="chat-ui"

# Backend URL — override via environment or edit here
BACKEND_BASE_URL="${BACKEND_BASE_URL:-http://scc-app:8080}"

# ── Helpers ────────────────────────────────────────────────────────────────────
log()  { printf '\033[1;34m>>>\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m✓\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m✗\033[0m %s\n' "$*" >&2; exit 1; }

# ── Preflight ──────────────────────────────────────────────────────────────────
command -v docker        >/dev/null 2>&1 || fail "docker not found"
command -v docker compose>/dev/null 2>&1 || \
    command -v docker-compose>/dev/null 2>&1 || fail "docker compose not found"

cd "$REPO_DIR"

# ── Pull latest code ───────────────────────────────────────────────────────────
log "Pulling latest from GitHub…"
if git -C "$REPO_DIR" diff --quiet HEAD 2>/dev/null; then
    git -C "$REPO_DIR" pull origin main
    ok "Code up to date"
else
    log "Skipping pull — uncommitted local changes detected"
fi

# ── Build image ────────────────────────────────────────────────────────────────
log "Building Docker image for $SERVICE…"
BACKEND_BASE_URL="$BACKEND_BASE_URL" docker compose -f "$COMPOSE_FILE" build "$SERVICE"
ok "Image built"

# ── Restart service ────────────────────────────────────────────────────────────
log "Restarting $SERVICE…"
BACKEND_BASE_URL="$BACKEND_BASE_URL" docker compose -f "$COMPOSE_FILE" up -d --force-recreate "$SERVICE"
ok "$SERVICE is up"

# ── Health check ───────────────────────────────────────────────────────────────
log "Waiting for health check…"
MAX=24; COUNT=0
until curl -sf http://localhost:8090/login >/dev/null 2>&1; do
    COUNT=$((COUNT+1))
    [ $COUNT -ge $MAX ] && fail "Service did not become healthy after ${MAX} attempts"
    printf '.'
    sleep 5
done
echo
ok "ServiceCodeChat UI is live at http://localhost:8090"
