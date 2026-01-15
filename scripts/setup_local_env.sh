#!/bin/bash
set -e

# Define Android Home
export ANDROID_HOME="$PWD/.android_sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip"

# Setup Android SDK
if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
    echo "Downloading Android SDK Command Line Tools..."
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    wget -q "$CMDLINE_TOOLS_URL" -O cmdline-tools.zip

    echo "Unzipping..."
    unzip -q cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"

    # Move extracted 'cmdline-tools' to 'latest'
    # The zip usually contains a 'cmdline-tools' folder at the root.
    # We unzipped into $ANDROID_HOME/cmdline-tools, so we have $ANDROID_HOME/cmdline-tools/cmdline-tools
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"

    rm cmdline-tools.zip
    echo "Android SDK Command Line Tools installed."
else
    echo "Android SDK Command Line Tools already installed."
fi

# Add to PATH temporarily for this script
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

echo "Accepting licenses..."
yes | sdkmanager --licenses > /dev/null 2>&1 || true

echo "Installing/Updating SDK packages..."
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Keystore setup
KEYSTORE_FILE="ci-debug.keystore"
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "Generating dummy keystore..."
    keytool -genkey -v -keystore "$KEYSTORE_FILE" -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Android Debug,O=Android,C=US"
    echo "Keystore generated."
else
    echo "Keystore already exists."
fi

echo "Local environment setup complete."
