#!/bin/bash
set -e

# Unified build script for tw.mxp.idempiere.kanban
# Ensures SPA is always rebuilt before Maven packaging

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
IDEMPIERE_REPO="${IDEMPIERE_REPO:-file:///idempiere/org.idempiere.p2/target/repository}"

echo "=== Step 1: Build SPA ==="
docker run --rm \
  -v "$SCRIPT_DIR":/app \
  -w /app/spa \
  node:20-slim \
  sh -c "npm install --silent 2>/dev/null && npm run build"

echo ""
echo "=== Step 2: Build Plugin + p2 ==="
docker run --rm \
  -v "$SCRIPT_DIR":/plugin \
  -v "${IDEMPIERE_SRC:-/Users/hungchunchiang/Projects/idempiere}":/idempiere \
  -v "$HOME/.m2":/root/.m2 \
  -w /plugin \
  maven:3.9-eclipse-temurin-17 \
  mvn verify -Didempiere.repository="$IDEMPIERE_REPO"

echo ""
echo "=== Build complete ==="
JAR=$(ls "$SCRIPT_DIR"/tw.mxp.idempiere.kanban/target/tw.mxp.idempiere.kanban-*.jar 2>/dev/null | head -1)
if [ -n "$JAR" ]; then
  echo "JAR: $JAR"
  echo "SPA files in JAR:"
  unzip -l "$JAR" | grep "web/assets/"
fi
