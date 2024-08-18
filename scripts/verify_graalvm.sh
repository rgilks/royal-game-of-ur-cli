#!/usr/bin/env zsh
set -euo pipefail

echo "Verifying GraalVM installation..."

if [ -z "${GRAALVM_HOME:-}" ]; then
    echo "Error: GRAALVM_HOME is not set. Please run 'just install-graalvm' and restart your terminal."
    exit 1
fi

if [ ! -d "$GRAALVM_HOME" ]; then
    echo "Error: GRAALVM_HOME directory does not exist: $GRAALVM_HOME"
    exit 1
fi

if ! command -v native-image &> /dev/null; then
    echo "Error: native-image is not found in PATH. Please ensure GraalVM is correctly installed."
    exit 1
fi

echo "GRAALVM_HOME: $GRAALVM_HOME"
echo "Java version:"
java -version
echo "Native image version:"
native-image --version

echo "GraalVM verification complete."