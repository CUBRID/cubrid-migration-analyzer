#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_REPO="${BUILD_REPO:-$PROJECT_DIR/.build/submodule-m2repo}"
PL_SERVER_JAR="${PL_SERVER_JAR:-$PROJECT_DIR/pl_server/pl_server.jar}"
CMT_MODULES="${CMT_MODULES:-plugins/com.cubrid.common.log,plugins/com.cubrid.common.configuration,plugins/com.cubrid.cubridmigration.core}"
CMT_VERSION="${CMT_VERSION:-1.0.0-SNAPSHOT}"

WITH_SUBMODULES=false
MAVEN_ARGS=()

print_usage() {
    cat <<'EOF'
Usage: bash build.sh [options] [maven arguments]

Options:
  -s, --with-submodules  Build and install the required CMT submodules first
  -h, --help             Show this help message

Examples:
  bash build.sh
  bash build.sh --with-submodules
  bash build.sh --with-submodules clean package
  bash build.sh test
EOF
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        -s|--with-submodules)
            WITH_SUBMODULES=true
            ;;
        -h|--help)
            print_usage
            exit 0
            ;;
        --)
            shift
            MAVEN_ARGS+=("$@")
            break
            ;;
        *)
            MAVEN_ARGS+=("$1")
            ;;
    esac
    shift
done

if [ "${#MAVEN_ARGS[@]}" -eq 0 ]; then
    MAVEN_ARGS=(-DskipTests package)
fi

if [ "$WITH_SUBMODULES" = true ]; then
    CMT_POM="$PROJECT_DIR/submodule/cubrid-migration/pom.xml"
    if [ ! -f "$CMT_POM" ]; then
        echo "CMT submodule is not initialized: $CMT_POM" >&2
        echo "Run: git submodule update --init --recursive" >&2
        exit 1
    fi

    echo "Installing CMT artifacts from submodule..."
    mvn \
        -Dmaven.repo.local="$BUILD_REPO" \
        -f "$CMT_POM" \
        -Pconsole \
        -pl "$CMT_MODULES" \
        -am \
        -DskipTests \
        install
fi

CMT_REPO_PATH="$BUILD_REPO/com/cubrid/cubridmigration"
CMT_CONFIGURATION_JAR="$CMT_REPO_PATH/com.cubrid.common.configuration/$CMT_VERSION/com.cubrid.common.configuration-$CMT_VERSION.jar"
CMT_CORE_JAR="$CMT_REPO_PATH/com.cubrid.cubridmigration.core/$CMT_VERSION/com.cubrid.cubridmigration.core-$CMT_VERSION.jar"

if [ ! -f "$CMT_CONFIGURATION_JAR" ] || [ ! -f "$CMT_CORE_JAR" ]; then
    echo "Required CMT artifacts are missing from: $BUILD_REPO" >&2
    echo "Run the initial build with: bash build.sh --with-submodules" >&2
    exit 1
fi

if [ ! -f "$PL_SERVER_JAR" ]; then
    echo "pl_server jar is missing: $PL_SERVER_JAR" >&2
    echo "Place a custom-built pl_server.jar there or set PL_SERVER_JAR=/path/to/pl_server.jar." >&2
    exit 1
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -d "$JAVA_HOME/include" ]; then
    echo "JAVA_HOME must point to a JDK installation with JNI headers." >&2
    exit 1
fi

if [ -z "${CUBRID:-}" ] || [ ! -d "$CUBRID/include" ]; then
    echo "CUBRID must point to a CUBRID installation with development headers." >&2
    exit 1
fi

echo "Building JNI SQL validator..."
make -C "$PROJECT_DIR/jni" all

echo "Building SQL Analyzer..."
mvn \
    -Dmaven.repo.local="$BUILD_REPO" \
    -Dpl.server.jar="$PL_SERVER_JAR" \
    -f "$PROJECT_DIR/pom.xml" \
    "${MAVEN_ARGS[@]}"
