# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AWS4Tasker (formerly OpenCV4Tasker) is an Android plugin for Tasker and MacroDroid that provides AI-powered image analysis capabilities. The app can detect humans in images and perform general-purpose image analysis using multiple AI engines including Claude AI, Google Gemini, OpenRouter, and MediaPipe (local).

## Build Commands

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

## Architecture Overview

### Core Components

- **Application Class**: `OpenCV4TaskerApplication` - Main application entry point with singleton pattern
- **Activities**: 
  - `SplashActivity` - Launcher activity
  - `MainActivity` - Main UI for testing image analysis
  - `ConfigActivity` - General configuration
- **AI Engines**: Multiple implementations of image analysis:
  - `HumansDetectorClaudeAI` - Claude AI integration (default model: `claude-sonnet-4-6`)
  - `HumansDetectorGemini` - Google Gemini integration (default model: `gemini-2.5-flash`)
  - `HumansDetectorOpenRouter` - OpenRouter integration (user-configurable model)
  - `HumansDetectorTensorFlow` - Local processing via MediaPipe Tasks Vision (class name kept for backward compatibility)
  - `AIImageAnalyzer` - Common interface for AI-based image analysis

### Tasker Plugin System

The app integrates with Tasker/MacroDroid through:
- **Actions**:
  - `DetectHumansActionHelper` - Human detection in images
  - `AnalyzeImageActionHelper` - General AI image analysis
  - `CancelNotificationActionHelper` - Cancels a notification by its key
- **Events**:
  - `NotificationInterceptedEvent` - Intercepts notifications (with or without images)
- **Configuration Activities**:
  - `ActivityConfigDetectHumansAction`
  - `ActivityConfigAnalyzeImageAction`
  - `ActivityConfigCancelNotificationAction`
  - `ActivityConfigNotificationInterceptedEvent`

### Notification Interception

The notification interception system includes:
- `NotificationInterceptorService` - Core notification listener service
- `NotificationImageExtractor` - Extracts images from notifications
- `NotificationFileManager` - Manages temporary image files

### Key Dependencies

- Tasker Plugin Library: `com.joaomgcd:taskerpluginlibrary:0.4.10`
- MediaPipe Tasks Vision: `com.google.mediapipe:tasks-vision:0.10.21` (local object detection, replaces TensorFlow Lite)
- AndroidX libraries for modern Android development
- Kotlin support with Java interop

## Development Notes

- **Target SDK**: 36, **Min SDK**: 30 (Android 11+)
- **Language**: Mixed Java/Kotlin codebase
- **Permissions**: Requires storage, internet, notification access, and battery optimization bypass
- **Build Tools**: Android Gradle Plugin 8.9.0, Gradle 8.11.1, Build Tools 35.0.0
- The project uses view binding and data binding
- MediaPipe model is stored in `app/src/main/assets/`
- Package name: `online.avogadro.opencv4tasker`

## Engine Configuration

The app supports four AI engines selected via radio buttons:
- **CLAUDE**: Cloud-based Claude AI analysis (`claude-sonnet-4-6` by default, configurable)
- **GEMINI**: Google Gemini integration (`gemini-2.5-flash` by default, configurable)
- **OPENROUTER**: OpenRouter cloud proxy (user-configurable model)
- **TENSORFLOW**: Local MediaPipe processing (default for backward compatibility; class/key name kept as `TENSORFLOW`)

Engine selection is persisted using `SharedPreferencesHelper` and each engine implements the `AIImageAnalyzer` interface for consistency.