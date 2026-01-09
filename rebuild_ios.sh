#!/bin/bash

# BrainBurst iOS Rebuild Script
# Run this if you encounter "PhaseScriptExecution failed" errors

set -e

echo "🧹 Cleaning iOS build artifacts..."

# Clean Xcode build
cd iosApp
xcodebuild clean -workspace iosApp.xcworkspace -scheme iosApp 2>/dev/null || true

# Clean Gradle iOS builds
cd ..
./gradlew :shared:clean

# Rebuild the framework
echo "🔨 Building iOS framework..."
./gradlew :shared:linkPodDebugFrameworkIosSimulatorArm64

echo "✅ iOS framework rebuilt successfully!"
echo ""
echo "Now open Xcode and build the project:"
echo "  cd iosApp"
echo "  open iosApp.xcworkspace"





