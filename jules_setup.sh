#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

echo "--- Setting up Android SDK environment ---"

# Define the Android SDK Root and add it to the environment file that Jules will use
export ANDROID_SDK_ROOT="$HOME/Android/sdk"
echo "export ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT" >> $HOME/.bashrc

# Add SDK command-line tools to the PATH for this session and future sessions
export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools"
echo "export PATH=$PATH" >> $HOME/.bashrc

# Download and unzip the Android command-line tools
echo "Downloading Android command-line tools..."
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/tools.zip
mkdir -p $ANDROID_SDK_ROOT/cmdline-tools
unzip -q /tmp/tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools
mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest

# Use sdkmanager to download the necessary platform-tools, platforms, and build-tools
# The "yes" command automatically accepts the licenses.
echo "Installing SDK packages..."
yes | sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" > /dev/null

# Clean up the downloaded zip file
rm /tmp/tools.zip

echo "--- Android SDK setup complete! ---"

# Optional: Run a Gradle build to verify the setup
# ./gradlew build
