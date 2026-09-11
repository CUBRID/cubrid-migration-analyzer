#!/usr/bin/env bash
set -e

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
JAVA="$APP_HOME/jre/bin/java"

if [ ! -x "$JAVA" ]; then
    echo "Bundled Java runtime is missing or not executable: $JAVA" >&2
    exit 1
fi

if [ -z "${CUBRID:-}" ]; then
    echo "CUBRID environment variable is not set." >&2
    exit 1
fi

export LD_LIBRARY_PATH="${CUBRID}/lib:${LD_LIBRARY_PATH:-}"

cd "$APP_HOME"

ANTLR4_RUNTIME="$(find "$APP_HOME/lib" -maxdepth 1 -name 'antlr4-runtime-*.jar' -print -quit)"

if [ -z "$ANTLR4_RUNTIME" ]; then
    echo "Required ANTLR runtime is missing from: $APP_HOME/lib" >&2
    exit 1
fi

exec "$JAVA" \
    -Djava.library.path="$APP_HOME/jni" \
    -Dsqlanalyzer.plcsql.jar="$APP_HOME/lib/pl_server.jar" \
    -cp "$APP_HOME/analyzer.jar:$APP_HOME/lib/*" \
    com.cubrid.sqlanalyzer.command.cli.AnalyzerConsoleMain \
    "$@"
