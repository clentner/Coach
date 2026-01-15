#!/bin/bash
set -e

echo "Running assembleDebug..."
./gradlew assembleDebug

echo "Running test..."
./gradlew test

echo "Running lint..."
./gradlew lint
