# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

1List is an Android todo list application built with Kotlin and Jetpack Compose. It allows users to manage multiple lists from a single screen with features like item creation, editing, marking as done, adding comments, and list management.

## Architecture

This project follows a multi-module Clean Architecture pattern with the following structure:

### Core Modules
- **core/data**: Repository implementations, file access, shared preferences, and data layer logic
- **core/database**: Room database setup with DAOs and entity models 
- **core/designsystem**: UI theme, colors, typography, and design system components
- **core/domain**: Use cases and business logic (contains 20+ use cases like CreateList, AddItemToList, etc.)
- **core/model**: Data models and entities
- **core/common**: Shared utilities and common code
- **core/testing**: Testing utilities and custom test runner

### Feature Modules
- **feature/lists**: Main list management UI and functionality
- **feature/settings**: App settings and preferences
- **feature/whatsnew**: What's new feature implementation

### App Module
- **app**: Main application module with MainActivity, navigation, and DI setup

## Build System

The project uses Gradle with Kotlin DSL and custom build logic:
- **build-logic/convention**: Custom Gradle plugins for consistent module setup
- Uses version catalogs for dependency management
- Multi-module setup with proper dependency injection using Koin

## Common Development Commands

### Build Commands
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK (requires signing config)
./gradlew clean                  # Clean project
```

### Testing Commands
```bash
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumented tests
./gradlew testDebugUnitTest      # Run debug unit tests
```

### Build Variants
- **debug**: Development build with debug suffix (.debug)
- **release**: Production build with minification and signing
- **instrumented**: Test build with test suffix (.tst)

## Key Technologies
- **Kotlin**: Primary language
- **Jetpack Compose**: UI framework
- **Room**: Database layer
- **Koin**: Dependency injection
- **Firebase Crashlytics**: Crash reporting (release builds only)
- **Robolectric**: Unit testing framework

## Database Schema
The app uses Room database with migrations. Schema files are located in:
- `app/schemas/com.lolo.io.onelist.core.database.OneListDatabase/`
- `core/database/schemas/com.lolo.io.onelist.core.database.OneListDatabase/`

## Testing Strategy
- Unit tests for use cases and repository logic
- Instrumented tests for database operations and UI
- Custom test runner: `OneListTestRunner`
- Test utilities in `core/testing` module

## Key Files to Understand
- `app/src/main/kotlin/com/lolo/io/onelist/MainActivity.kt`: Main entry point
- `app/src/main/kotlin/com/lolo/io/onelist/navigation/OneListNavHost.kt`: Navigation setup
- `core/domain/src/main/kotlin/com/lolo/io/onelist/core/domain/use_cases/OneListUseCases.kt`: All use cases
- `core/data/src/main/kotlin/com/lolo/io/onelist/core/data/repository/OneListRepositoryImpl.kt`: Main repository