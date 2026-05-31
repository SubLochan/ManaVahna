# Android Studio Flamingo & Panda 4 Compatibility & Quickstart Cheat Sheet

This project has been fully optimized to compile cleanly under both **Android Studio Flamingo (2022.2.x)** and newer modern releases like **Android Studio Panda 4 (2026.1.x)** using **Java JDK 17**, **Android SDK 34**, and **Android NDK 25.1** out of the box.

---

## 🛠️ Project Environment Specs
* **Android Studio Version**: Android Studio Flamingo (2022.2.1+) OR Android Studio Panda 4 (2026.1.4+)
* **JDK**: Strictly Java JDK 17
* **Android SDK**: `compileSdk = 34`, `targetSdk = 34`
* **Android NDK**: `25.1.8937393`
* **Gradle Build Tool**: Gradle `8.5` (via Gradle Wrapper)
* **Android Gradle Plugin (AGP)**: `8.5.0`
* **Kotlin Version**: `2.2.10`

---

## 🚀 4-Step Local Import & Run Guide

> [!NOTE]
> **Resolved ClassNotFoundException Bug**: Initially, the root project configuration was missing the explicit `kotlin-android` compilation plugin reference. We have successfully registered and applied `kotlin-android` across the gradle catalog and build modules. All Kotlin activities and application classes (like `ManaVahanaApplication` and `MainActivity`) are now compiled and bundled into the build classes.dex.

### Step 1: Open the Project in Android Studio
1. Open **Android Studio Flamingo** or **Android Studio Panda 4**.
2. Click **File -> Open...** (or **Open an Existing Project** on the welcome screen).
3. Navigate to the extracted project folder and select the root directory (containing `settings.gradle.kts` and `build.gradle.kts`).
4. Click **OK** to load.

### Step 2: Configure JDK 17 as the Gradle Runtime JDK
To ensure Java 17 compatibility, configure Android Studio to use JDK 17 for Gradle:
1. Open Settings/Preferences:
   * **Windows/Linux**: `File -> Settings...`
   * **macOS**: `Android Studio -> Settings...`
2. In the left panel, navigate to:  
   `Build, Execution, Deployment -> Build Tools -> Gradle`.
3. In the **Gradle JDK** dropdown select/add **JDK 17** (or the integrated Android Studio JDK 17 / JetBrains Runtime 17).
4. Click **Apply** and **OK**.

### Step 3: Install Required SDK & NDK Versions
Ensure you have Android SDK 34 and NDK 25.1 installed in Android Studio:
1. Open **SDK Manager** (`Tools -> SDK Manager` or the settings gear -> `SDK Manager`).
2. On the **SDK Platforms** tab, check **Android 14.0 (UpsideDownCake) (API Level 34)** to install.
3. Switch to the **SDK Tools** tab:
   * Check **Show Package Details** in the bottom right.
   * Scroll to **NDK (Side by side)**.
   * Expand it and check version `25.1.8937393`.
4. Click **OK** and let Android Studio download and install them.

### Step 4: Sync & Build Project
1. Press the **Sync Project with Gradle Files** elephant icon in the top toolbar (or click `File -> Sync Project with Gradle Files`).
2. Once the sync finishes without errors, build the APK:  
   `Build -> Make Project` or click the green **Run** triangle icon to launch on your physical device or configured Android emulator.

---

## 🔐 Managing API Keys & Secrets
The project uses the **Secrets Gradle Plugin** to load access keys cleanly via standard `.env` and `.env.example` configurations, mimicking advanced full-stack environments safely.

1. Locate the file `.env.example` in your project root to see required fields.
2. Duplicate it and rename it to `.env` in the root folder.
3. Replace the placeholder values with your real API credentials or credentials in the newly created `.env` file.
4. The Gradle secrets plugin will automatically inject them into the `BuildConfig` file during build-time securely!
