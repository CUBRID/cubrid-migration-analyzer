#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "build-with-submodules.sh is deprecated; use build.sh --with-submodules." >&2
exec bash "$SCRIPT_DIR/build.sh" --with-submodules "$@"
