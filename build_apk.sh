#!/usr/bin/env bash
# ==============================================================================
# Headless Android APK Builder (No Sudo or Android Studio required!)
# Uses user-space OpenJDK 17 + Gradle 8.4 + Android Command Line Tools
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRATCH_DIR="/home/ranjeet/.gemini/antigravity-ide/scratch"
JDK_DIR="$SCRATCH_DIR/jdk17"
GRADLE_DIR="$SCRATCH_DIR/gradle"
SDK_DIR="$SCRATCH_DIR/android-sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

echo "======================================================================"
echo "⚡ Tailflick Headless APK Builder"
echo "======================================================================"

# 1. Environment Exports
export JAVA_HOME="$JDK_DIR"
export ANDROID_HOME="$SDK_DIR"
export PATH="$JDK_DIR/bin:$GRADLE_DIR/bin:$SDK_DIR/cmdline-tools/latest/bin:$SDK_DIR/platform-tools:$PATH"

JAVA_VER=$(java -version 2>&1 | head -n 1)
echo "✅ Java ready: $JAVA_VER"
echo "✅ Gradle ready: $(gradle -v | head -n 3 | tail -n 1)"

# 2. Setup Minimal Android SDK (CMDLINE TOOLS)
if [ ! -d "$SDK_DIR/cmdline-tools/latest" ]; then
    echo "📥 Downloading Android Command Line Tools (~140MB)..."
    mkdir -p "$SDK_DIR/cmdline-tools"
    TMP_ZIP="/tmp/cmdline-tools.zip"
    wget -q --show-progress "$CMDLINE_TOOLS_URL" -O "$TMP_ZIP"
    
    echo "📦 Extracting SDK tools..."
    unzip -q "$TMP_ZIP" -d "$SDK_DIR/cmdline-tools"
    mv "$SDK_DIR/cmdline-tools/cmdline-tools" "$SDK_DIR/cmdline-tools/latest"
    rm -f "$TMP_ZIP"
    echo "✅ Android SDK tools installed at $SDK_DIR"
fi

# 3. Accept Licenses & Install Required Build Tools
echo "⚙️  Checking Android SDK packages (API 37.2)..."
yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-37.2" "build-tools;36.0.0" > /dev/null 2>&1 || true

# 4. Build Debug APK
cd "$SCRIPT_DIR"
echo "🚀 Compiling Android Kotlin app into APK..."
./gradlew assembleDebug

APK_SOURCE="$SCRIPT_DIR/app/build/outputs/apk/debug/app-debug.apk"
APK_DEST_PATH="$SCRIPT_DIR/app/build/outputs/apk/debug/tailflick-debug.apk"
WEB_DEST="/home/ranjeet/.gemini/antigravity-ide/scratch/gesture_actions_web/tailflick-debug.apk"

if [ -f "$APK_SOURCE" ]; then
    cp "$APK_SOURCE" "$APK_DEST_PATH"
    cp "$APK_SOURCE" "$WEB_DEST"
    echo "======================================================================"
    echo "🎉 SUCCESS! Your APK has been compiled successfully:"
    echo "   📍 Direct File Path: $APK_DEST_PATH"
    echo "   🌐 Web Server Link: http://localhost:8080/tailflick-debug.apk"
    echo "======================================================================"
else
    echo "❌ APK build failed. Check error logs."
fi
