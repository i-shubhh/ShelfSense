#!/usr/bin/env bash
set -e

if [ ! -d "out" ] || [ ! -f "out/Main.class" ]; then
    echo "Compiled classes not found. Running build.sh first..."
    ./build.sh
fi

echo "========================================================"
echo "  Starting ShelfSense Desktop Application..."
echo "========================================================"

if [ "$1" = "demo" ] || [ "$1" = "--demo" ]; then
    java -cp out Main demo
else
    java -cp out Main "$@"
fi

