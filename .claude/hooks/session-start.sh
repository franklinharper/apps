#!/bin/bash
set -euo pipefail

# Only run in remote Claude Code sessions (claude.ai/code)
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

REPO_DIR="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/../.." && pwd)}"
KMP_DIR="$REPO_DIR/kmp-app-generator"
BIN_DIR="$HOME/.local/bin"

# Ensure kmp-web-wizard submodule is initialised
cd "$REPO_DIR"
git submodule update --init --recursive

# Build the fat JAR + wrapper script
cd "$KMP_DIR"
gradle generateWrapper

# Install symlinks so the command is on PATH
mkdir -p "$BIN_DIR"
ln -sf "$KMP_DIR/build/libs/kmp-app-generator-all.jar" "$BIN_DIR/kmp-app-generator-all.jar"
ln -sf "$KMP_DIR/build/libs/kmp-app-generator"          "$BIN_DIR/kmp-app-generator"

# Ensure ~/.local/bin is on PATH for this session
echo "export PATH=\"$BIN_DIR:\$PATH\"" >> "${CLAUDE_ENV_FILE:-/dev/null}"

echo "kmp-app-generator installed to $BIN_DIR"
