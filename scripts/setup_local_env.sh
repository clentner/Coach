#!/bin/sh
# scripts/setup_local_env.sh

# Exit immediately if not Linux
if [ "$(uname)" != "Linux" ]; then
    return 0 2>/dev/null || exit 0
fi

# Determine PROJECT_ROOT
if [ -n "$APP_HOME" ]; then
    PROJECT_ROOT="$APP_HOME"
else
    # Fallback for standalone execution
    # For POSIX sh, $0 is the script path
    dir_path=$(dirname "$0")
    # Resolve to absolute path
    SCRIPT_DIR=$(cd "$dir_path" && pwd)
    PROJECT_ROOT=$(dirname "$SCRIPT_DIR")
fi

# Java Check
if ! command -v java >/dev/null 2>&1 && [ -z "$JAVA_HOME" ]; then
    echo "Error: Java is not found. Please install Java or set JAVA_HOME." >&2
    return 1 2>/dev/null || exit 1
fi

# Android SDK Setup
export ANDROID_HOME="$PROJECT_ROOT/.android_sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"

# Install Command Line Tools if missing
if [ ! -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "Downloading Android SDK Command Line Tools..."
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    # Use -o (overwrite) and -q (quiet)
    if command -v wget >/dev/null 2>&1; then
        wget -q "$CMDLINE_TOOLS_URL" -O "$ANDROID_HOME/cmdline-tools/cmdline-tools.zip"
    elif command -v curl >/dev/null 2>&1; then
        curl -s "$CMDLINE_TOOLS_URL" -o "$ANDROID_HOME/cmdline-tools/cmdline-tools.zip"
    else
        echo "Error: Neither wget nor curl found." >&2
        return 1 2>/dev/null || exit 1
    fi

    unzip -q -o "$ANDROID_HOME/cmdline-tools/cmdline-tools.zip" -d "$ANDROID_HOME/cmdline-tools"

    # Move extracted 'cmdline-tools' to 'latest'
    if [ -d "$ANDROID_HOME/cmdline-tools/cmdline-tools" ]; then
        rm -rf "$ANDROID_HOME/cmdline-tools/latest"
        mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    fi
    rm "$ANDROID_HOME/cmdline-tools/cmdline-tools.zip"
    echo "Android SDK Command Line Tools installed."
fi

# Update PATH
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

# Install Packages
PACKAGES_TO_INSTALL=""

# Check platform-tools
if [ ! -d "$ANDROID_HOME/platform-tools" ]; then
    PACKAGES_TO_INSTALL="$PACKAGES_TO_INSTALL platform-tools"
fi

# Check platforms;android-34
if [ ! -d "$ANDROID_HOME/platforms/android-34" ]; then
    PACKAGES_TO_INSTALL="$PACKAGES_TO_INSTALL platforms;android-34"
fi

# Check build-tools;34.0.0
if [ ! -d "$ANDROID_HOME/build-tools/34.0.0" ]; then
    PACKAGES_TO_INSTALL="$PACKAGES_TO_INSTALL build-tools;34.0.0"
fi

if [ -n "$PACKAGES_TO_INSTALL" ]; then
    echo "Installing missing SDK packages:$PACKAGES_TO_INSTALL"

    # Accept licenses if likely needed
    if [ ! -d "$ANDROID_HOME/licenses" ]; then
         yes | sdkmanager --licenses > /dev/null 2>&1 || true
    fi

    sdkmanager $PACKAGES_TO_INSTALL > /dev/null 2>&1
fi

# Keystore Setup
KEYSTORE_FILE="$PROJECT_ROOT/ci-debug.keystore"
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "Generating dummy keystore..."
    keytool -genkey -v -keystore "$KEYSTORE_FILE" -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US" > /dev/null 2>&1
    echo "Keystore generated."
fi

# Local Properties
LOCAL_PROPERTIES="$PROJECT_ROOT/local.properties"
SDK_DIR_ENTRY="sdk.dir=$ANDROID_HOME"

if [ ! -f "$LOCAL_PROPERTIES" ]; then
    echo "Creating local.properties..."
    echo "$SDK_DIR_ENTRY" > "$LOCAL_PROPERTIES"
elif ! grep -q "^sdk.dir=" "$LOCAL_PROPERTIES"; then
    echo "Updating local.properties with sdk.dir..."
    echo "$SDK_DIR_ENTRY" >> "$LOCAL_PROPERTIES"
fi
