#!/bin/bash
set -euo pipefail

# Only run in remote Claude Code sessions (claude.ai/code)
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

REPO_DIR="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "$0")/../.." && pwd)}"
KMP_DIR="$REPO_DIR/kmp-app-generator"
DIST_JAR="$KMP_DIR/dist/kmp-app-generator-all.jar"
STORED_HASH_FILE="$KMP_DIR/dist/source.hash"
BIN_DIR="$HOME/.local/bin"

# Ensure kmp-web-wizard submodule is initialised
cd "$REPO_DIR"
git submodule update --init --recursive

# Compute hash of sources that affect the JAR output:
#   src/ files + build.gradle.kts + submodule commit
compute_source_hash() {
  {
    find "$KMP_DIR/src" -type f | sort | xargs sha256sum 2>/dev/null
    sha256sum "$KMP_DIR/build.gradle.kts"
    cd "$KMP_DIR/kmp-web-wizard" && git rev-parse HEAD
  } | sha256sum | cut -c1-64
}

CURRENT_HASH=$(compute_source_hash)
STORED_HASH=$(cat "$STORED_HASH_FILE" 2>/dev/null || echo "")

if [ "$CURRENT_HASH" = "$STORED_HASH" ] && [ -f "$DIST_JAR" ]; then
  echo "kmp-app-generator: dist/ is up to date, skipping build"
else
  echo "kmp-app-generator: source changed (or first run), rebuilding..."
  cd "$KMP_DIR"
  gradle generateWrapper
  cp "$KMP_DIR/build/libs/kmp-app-generator-all.jar" "$KMP_DIR/dist/"
  echo "$CURRENT_HASH" > "$STORED_HASH_FILE"
  echo "kmp-app-generator: dist/ updated"
fi

# Install into ~/.local/bin
mkdir -p "$BIN_DIR"
cp "$DIST_JAR" "$BIN_DIR/kmp-app-generator-all.jar"

# Write a wrapper script pointing at the copied JAR
cat > "$BIN_DIR/kmp-app-generator" << 'WRAPPER'
#!/bin/bash
exec java -jar "$(dirname "$0")/kmp-app-generator-all.jar" "$@"
WRAPPER
chmod +x "$BIN_DIR/kmp-app-generator"

# Ensure ~/.local/bin is on PATH for this session
echo "export PATH=\"$BIN_DIR:\$PATH\"" >> "${CLAUDE_ENV_FILE:-/dev/null}"

echo "kmp-app-generator installed to $BIN_DIR"
