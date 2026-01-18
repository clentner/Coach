#!/bin/sh
# scripts/install_adb_and_emulator.sh

# Exit immediately if not Linux
if [ "$(uname)" != "Linux" ]; then
    return 0 2>/dev/null || exit 0
fi

# Determine PROJECT_ROOT
if [ -n "$APP_HOME" ]; then
    PROJECT_ROOT="$APP_HOME"
else
    # Fallback for standalone execution
    dir_path=$(dirname "$0")
    SCRIPT_DIR=$(cd "$dir_path" && pwd)
    PROJECT_ROOT=$(dirname "$SCRIPT_DIR")
fi

# Source setup_local_env.sh to ensure SDK is available
. "$PROJECT_ROOT/scripts/setup_local_env.sh"

# Emulator and System Image packages
EMULATOR_PKG="emulator"
SYSTEM_IMAGE_PKG="system-images;android-34;google_apis;x86_64"

# Check if emulator is installed
if [ ! -d "$ANDROID_HOME/emulator" ]; then
    echo "Installing Android Emulator..."
    sdkmanager "$EMULATOR_PKG" > /dev/null 2>&1
    echo "Android Emulator installed."
fi

# Check if system image is installed
# The directory structure for system images is $ANDROID_HOME/system-images/android-34/google_apis/x86_64
if [ ! -d "$ANDROID_HOME/system-images/android-34/google_apis/x86_64" ]; then
    echo "Installing system image..."
    sdkmanager "$SYSTEM_IMAGE_PKG" > /dev/null 2>&1
    echo "System image installed."
fi

# Create AVD, overwriting if it exists
AVD_NAME="ci_emulator"
echo "Creating AVD..."
echo "no" | avdmanager create avd -n "$AVD_NAME" -k "$SYSTEM_IMAGE_PKG" --force
echo "AVD created."
