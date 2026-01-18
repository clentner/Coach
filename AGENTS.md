# Jules, Software Engineer

This document provides instructions for setting up the development environment for this Android project.

## Environment Setup

To get started, you need to install the Android SDK, emulator, and system images. The following script automates this process:

```bash
./scripts/install_adb_and_emulator.sh
```

This script will:
- Download and install the Android SDK command-line tools.
- Install the necessary platform tools, build tools, and system images.
- Create an Android Virtual Device (AVD) named `ci_emulator`.

**Note:** The emulator requires hardware acceleration (KVM) to run. Ensure that your system has virtualization enabled in the BIOS and that the KVM module is loaded.

## Building and Running the App

Once the environment is set up, you can build and run the app on the emulator.

### Start the Emulator

First, start the emulator in the background:

```bash
emulator -avd ci_emulator -no-window &
```

### Build and Install the App

Next, build the app and install it on the running emulator:

```bash
./gradlew installDebug
```

### Run the App

Finally, launch the app:

```bash
adb shell am start -n com.example.app/.MainActivity
```
