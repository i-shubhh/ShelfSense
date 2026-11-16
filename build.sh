#!/usr/bin/env bash
set -e

echo "========================================================"
echo "  Building ShelfSense Desktop Application"
echo "  Core Java 11+ & Java AWT (Zero External Dependencies)"
echo "========================================================"

mkdir -p out
javac -d out $(find src -name "*.java")

echo "Compilation successful! Output classes compiled to out/"

