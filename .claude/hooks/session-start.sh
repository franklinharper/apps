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

# Compute hash of sources that affect the JAR output.
# Must match the algorithm in build.gradle.kts generateWrapper exactly:
#   - sha256(content) + "  " + relative-path  for each src/ file (sorted)
#   - sha256(content) + "  build.gradle.kts"
#   - submodule-commit + "  kmp-web-wizard"
# then sha256 of all those lines concatenated.
compute_source_hash() {
  {
    find "$KMP_DIR/src" -type f | sort | while IFS= read -r f; do
      hash=$(sha256sum "$f" | cut -d' ' -f1)
      echo "$hash  ${f#$KMP_DIR/}"
    done
    hash=$(sha256sum "$KMP_DIR/build.gradle.kts" | cut -d' ' -f1)
    echo "$hash  build.gradle.kts"
    subhash=$(cd "$KMP_DIR/kmp-web-wizard" && git rev-parse HEAD)
    echo "$subhash  kmp-web-wizard"
  } | sha256sum | cut -c1-64
}

CURRENT_HASH=$(compute_source_hash)
STORED_HASH=$(cat "$STORED_HASH_FILE" 2>/dev/null || echo "")

if [ "$CURRENT_HASH" = "$STORED_HASH" ] && [ -f "$DIST_JAR" ]; then
  echo "kmp-app-generator: dist/ is up to date, skipping build"
else
  echo "kmp-app-generator: source changed (or first run), rebuilding..."
  cd "$KMP_DIR"
  gradle generateWrapper   # also updates dist/ and dist/source.hash
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
