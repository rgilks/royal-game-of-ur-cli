#!/usr/bin/env zsh
set -euo pipefail

echo "Creating target directory..."
mkdir -p target

echo "Building uberjar..."
clojure -T:build uber

if [ ! -f target/royal-game-of-ur.jar ]; then
    echo "Error: Jar file not created. Check the compilation output above."
    exit 1
fi

echo "Creating native image..."
native-image \
    -jar target/royal-game-of-ur.jar \
    -H:Name=royal-game-of-ur \
    -H:+ReportExceptionStackTraces \
    --initialize-at-build-time \
    --verbose \
    --no-fallback \
    -J-Xmx3g

if [ ! -f royal-game-of-ur ]; then
    echo "Error: Native image not created. Check the native-image output above."
    exit 1
fi

echo "Build successful. You can run the game with ./royal-game-of-ur"