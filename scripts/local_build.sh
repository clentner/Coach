#!/bin/bash
set -e

echo "Setting up local environment..."
./scripts/setup_local_env.sh

# Set Environment Variables needed for the build
export ANDROID_HOME="$PWD/.android_sdk"
export CI_KEYSTORE_PATH="ci-debug.keystore"
export CI_DEBUG_KEYSTORE_PASS="android"
export CI_DEBUG_KEY_ALIAS="androiddebugkey"
export CI_DEBUG_KEY_PASS="android"

echo "Environment variables set. Starting build..."
./scripts/run_ci_tasks.sh
