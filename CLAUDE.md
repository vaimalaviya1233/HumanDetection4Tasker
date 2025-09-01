# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AWS4Tasker (formerly OpenCV4Tasker) is an Android plugin for Tasker and MacroDroid that provides AI-powered image analysis capabilities. The app can detect humans in images and perform general-purpose image analysis using multiple AI engines including Claude AI, Google Gemini, and TensorFlow Lite.

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
  - `HumansDetectorClaudeAI` - Claude AI integration for human detection
  - `HumansDetectorGemini` - Google Gemini integration  
  - `HumansDetectorTensorFlow` - Local TensorFlow Lite processing
  - `AIImageAnalyzer` - Common interface for AI-based image analysis

### Tasker Plugin System

The app integrates with Tasker/MacroDroid through:
- **Actions**: 
  - `DetectHumansActionHelper` - Human detection in images
  - `AnalyzeImageActionHelper` - General AI image analysis
- **Events**:
  - `NotificationInterceptedEvent` - Intercepts notifications with images
- **Configuration Activities**:
  - `ActivityConfigDetectHumansAction`
  - `ActivityConfigAnalyzeImageAction` 
  - `ActivityConfigNotificationInterceptedEvent`

### Notification Interception

New notification interception system includes:
- `NotificationInterceptorService` - Core notification listener service
- `NotificationImageExtractor` - Extracts images from notifications
- `NotificationFileManager` - Manages temporary image files

### Key Dependencies

- Tasker Plugin Library: `com.joaomgcd:taskerpluginlibrary:0.4.10`
- TensorFlow Lite: `org.tensorflow:tensorflow-lite:2.5.0` with related libraries
- AndroidX libraries for modern Android development
- Kotlin support with Java interop

## Development Notes

- **Target SDK**: 33, **Min SDK**: 30 (Android 11+)
- **Language**: Mixed Java/Kotlin codebase
- **Permissions**: Requires storage, internet, notification access, and battery optimization bypass
- **Build Tools**: Gradle with Android build tools 8.2.2
- The project uses view binding and data binding
- TensorFlow models are stored in `app/src/main/assets/`
- Package name: `online.avogadro.opencv4tasker`

## Engine Configuration

The app supports three AI engines selected via radio buttons:
- **CLAUDE**: Cloud-based Claude AI analysis
- **GEMINI**: Google Gemini integration
- **TENSORFLOW**: Local TensorFlow Lite processing (default for backward compatibility)

Engine selection is persisted using `SharedPreferencesHelper` and each engine implements the `AIImageAnalyzer` interface for consistency.