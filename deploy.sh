#!/usr/bin/env bash
set -euo pipefail

# ── Configuration ──────────────────────────────────────────────────────────────
REPO_DIR="$(cd "$(dirname "$0")" && pwd)"   # directory this script lives in
COMPOSE_FILE="$REPO_DIR/docker-compose.yml"
SERVICE="chat-ui"

# Backend URL — override via environment or edit here
BACKEND_BASE_URL="${BACKEND_BASE_URL:-http://scc-app:8080}"

# ── Load .env ──────────────────────────────────────────────────────────────────
ENV_FILE="$REPO_DIR/.env"
if [ -f "$ENV_FILE" ]; then
    # shellcheck disable=SC1090
    set -a; source "$ENV_FILE"; set +a
else
    fail ".env file not found at $ENV_FILE — copy .env.example and fill in values"
fi

# GitHub credentials
GITHUB_USER="fjchawla"
GITHUB_REPO="${GITHUB_REPO:-fjchawla/chat-ui}"
: "${GITHUB_TOKEN:?GITHUB_TOKEN must be set in .env}"

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
REPO_URL="https://${GITHUB_USER}:${GITHUB_TOKEN}@github.com/${GITHUB_REPO}.git"

if git -C "$REPO_DIR" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    # Ensure remote 'origin' uses the authenticated URL
    git -C "$REPO_DIR" remote set-url origin "$REPO_URL" 2>/dev/null || \
        git -C "$REPO_DIR" remote add origin "$REPO_URL"

    if git -C "$REPO_DIR" diff --quiet HEAD 2>/dev/null; then
        git -C "$REPO_DIR" pull origin main
        ok "Code up to date"
    else
        log "Skipping pull — uncommitted local changes detected"
    fi
else
    # Directory exists but has no git repo — initialise in-place
    log "No git repo found — initialising and fetching from GitHub…"
    git -C "$REPO_DIR" init
    git -C "$REPO_DIR" remote add origin "$REPO_URL"
    git -C "$REPO_DIR" fetch --depth=1 origin main
    git -C "$REPO_DIR" reset --hard origin/main
    ok "Repository initialised"
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
