# AIRMIND AI 🎧⚡
> **Universal Bluetooth Earbuds Controller and AI Mobile Control for Android**

AIRMIND AI is an advanced Android application built with Kotlin, Jetpack Compose, Material 3, and Clean Architecture. It allows users to control phone functions through any brand of Bluetooth earbuds, monitor live audio with remote listening, adjust active noise cancellation (ANC) and transparency modes, customize gestures, and interact with an on-device AI voice assistant.

---

## 🚀 Key Features

- **Instant Bluetooth Earbuds Detection:** Multi-tier hardware detection via `AudioManager` output/input routing, synchronous profile state querying (`A2DP` and `HFP`), and Android 14+ system broadcast receivers.
- **3D Real-Time Animated Earbuds:** Native Jetpack Compose 3D visualization with holographic energy rings, particle systems, and device gyroscope orientation tracking.
- **Noise Cancellation Controller:** Active toggle and slider controls for ANC, Transparency Mode, and Off.
- **Remote Audio Listening:** Real-time phone microphone audio streaming to connected Bluetooth earbuds with audio gain, noise suppression, and voice enhancement DSP.
- **Universal Gesture Mapping:** Configure single-tap, double-tap, triple-tap, and long-press actions for Left, Right, or Both earbuds.
- **AI Voice Assistant:** Voice input speech-to-text recognition, text-to-speech feedback, application shortcuts, and conversational commands.
- **Smart Automations:** Profile-based triggers (e.g. on earbud connection or battery thresholds).

---

## 🛠️ Tech Stack & Requirements

- **Language:** Kotlin 2.2+
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Minimum SDK:** Android 7.0 (API 24)
- **Target SDK / Compile SDK:** Android 15/16 (API 36)
- **Build System:** Gradle 9.3.1 with Kotlin DSL (`build.gradle.kts`)
- **JDK:** Java 21 (Temurin / OpenJDK)
- **Architecture:** MVVM, Clean Architecture, Repository Pattern
- **Persistence:** Room Database, Jetpack DataStore Preferences

---

## 📦 Building the App

### Prerequisites
1. Install **JDK 21** (or ensure `JAVA_HOME` points to a Java 21 installation).
2. Install the **Android SDK** with build-tools (or Android Studio Ladybug/Meerkat).

### Command Line Build

1. **Clone the repository:**
   ```bash
   git clone <repository_url>
   cd <repository_directory>
   ```

2. **Ensure Gradle wrapper is executable:**
   ```bash
   chmod +x gradlew
   ```

3. **Run Unit & Robolectric Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

4. **Build the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The generated APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

5. **Build the Android App Bundle (AAB):**
   ```bash
   ./gradlew bundleDebug
   ```
   The generated AAB will be located at:
   `app/build/outputs/bundle/debug/app-debug.aab`

---

## ⚙️ GitHub Actions CI / CD

This repository includes automated GitHub Actions workflows located in `.github/workflows/`:
- **`android.yml`:** Automatically tests and builds the debug APK on every `push` and `pull_request` to `main`/`master`, and uploads the APK as a downloadable artifact.
- **`build.yml`:** Builds release artifacts on version tags or manual workflow dispatch.

---

## 🔒 Permissions & Security

AIRMIND AI requests only standard and essential Android permissions:
- `BLUETOOTH_CONNECT` & `BLUETOOTH_SCAN` (Android 12+) to detect and pair with your earbuds.
- `RECORD_AUDIO` & `MODIFY_AUDIO_SETTINGS` for the Remote Listening engine and AI speech recognition.
- `FOREGROUND_SERVICE` for low-latency background audio routing.
