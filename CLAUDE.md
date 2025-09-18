# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android Reverse Engineering CTF challenge based on the OneList todo application. The project contains 10 hidden flags in the format `CYWR{...}` that increase in difficulty and are designed to teach Android security analysis techniques.

**Important**: This is educational security content for learning reverse engineering, not production code.

## CTF Structure

### Target
- **Goal**: Find 10 hidden flags with format `CYWR{...}`
- **Difficulty**: Progressive from beginner to expert
- **Learning Focus**: Android reverse engineering fundamentals

### Flag Categories
- **Flags 1-5**: Static Analysis (using jadx, apktool, strings, grep)
- **Flags 6-10**: Dynamic Analysis (using frida, adb, runtime instrumentation)

## Build System & Commands

### Android Build Commands
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew clean                  # Clean project
```

### Build Variants
- **debug**: Development build with `.debug` suffix
- **release**: Production build with minification and ProGuard obfuscation

### APK Installation
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Architecture

Multi-module Android project using Clean Architecture:

### Core Modules
- **core/data**: Repository implementations and data layer
- **core/database**: Room database with entity models
- **core/domain**: Use cases and business logic
- **core/model**: Data models
- **core/common**: Shared utilities
- **core/designsystem**: UI components and theme

### Feature Modules
- **feature/lists**: Main list management functionality
- **feature/settings**: App settings and preferences
- **feature/whatsnew**: What's new feature

### App Module
- **app**: Main application with navigation and dependency injection

## Key Technologies
- **Kotlin**: Primary language
- **Jetpack Compose**: UI framework
- **Room**: Database layer
- **Koin**: Dependency injection
- **Firebase Crashlytics**: Crash reporting (release only)

## Reverse Engineering Analysis Points

### Static Analysis Targets
- String constants and hardcoded values
- Build variant differences (debug vs release)
- Resource files and manifest entries
- Database schema and migrations
- ProGuard obfuscation in release builds

### Dynamic Analysis Targets
- Runtime behavior and logging
- SharedPreferences storage
- SQLite database contents
- Settings screen interactions
- Application title and UI interactions

### Common Analysis Commands
```bash
# Extract and analyze APK
jadx -d output_dir app.apk
apktool d app.apk

# Device analysis
adb logcat | grep -i cywr
adb shell pm list packages | grep onelist
adb shell run-as com.lolo.io.onelist.debug
```

## Key Files for Analysis
- `app/src/main/kotlin/com/lolo/io/onelist/MainActivity.kt`: Main entry point
- `app/src/main/kotlin/com/lolo/io/onelist/navigation/OneListNavHost.kt`: Navigation
- `core/data/src/main/kotlin/com/lolo/io/onelist/core/data/repository/OneListRepositoryImpl.kt`: Data logic
- `app/src/main/res/values/strings.xml`: String resources
- `app/src/main/AndroidManifest.xml`: App manifest

## Database
- Uses Room database with migrations
- Schema files in `app/schemas/`
- Database name: `OneListDatabase`