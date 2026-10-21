# Daybell

> **Your day. On time. Offline.**  
> A calm, private and dependable task reminder for Android.

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Database-Room-FF6F00)](https://developer.android.com/training/data-storage/room)
[![DataStore](https://img.shields.io/badge/Preferences-DataStore-FF6F00)](https://developer.android.com/topic/libraries/architecture/datastore)
[![minSdk](https://img.shields.io/badge/minSdk-26-brightgreen)](https://developer.android.com/studio/releases/platforms)
[![targetSdk](https://img.shields.io/badge/targetSdk-34-blue)](https://developer.android.com/studio/releases/platforms)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[![Latest Release](https://img.shields.io/github/v/release/realximanta/daybell?include_prereleases&sort=semver&display_name=release)](https://github.com/realximanta/daybell/releases)
[![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/realximanta/daybell/.github/workflows/android.yml?branch=main&label=Build)](https://github.com/realximanta/daybell/actions)
[![GitHub last commit](https://img.shields.io/github/last-commit/realximanta/daybell/main)](https://github.com/realximanta/daybell/commits/main)

---

## ✨ Features

- **Task manager** – Add, edit, delete, complete and restore tasks. Each task supports a title, description, date, time, category and priority.
- **Flexible reminders** – One‑time or repeating alarms (daily, weekly, monthly, custom days). Multiple reminders per task, advance alerts, and snooze options (5 / 10 / 15 min).
- **Reliable notifications** – Exact alarms (with graceful fallback), notification channels, and action buttons (Done, Snooze, Open).
- **Offline‑first** – No account, no cloud, no network required. All data stays on your device.
- **Backup / export** – Local export and import so you can move tasks between phones.
- **Polished UI** – Graphite / Neon Teal / Soft Violet theme, dark & light mode, adaptive launcher icon, and a clean Compose‑based dashboard.

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|------------|
| Language | **Kotlin** |
| UI | **Jetpack Compose + Material 3** |
| Local storage | **Room** (tasks, reminders, categories) |
| Preferences | **DataStore** (theme, snooze, week start, …) |
| Reminders | **AlarmManager** (exact & inexact fallback) |
| Build & CI | **Gradle** + **GitHub Actions** (release builds on tags) |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio **Iguana** or newer
- JDK **17**
- Android SDK **34**

### Build from source
```bash
git clone https://github.com/realximanta/daybell.git
cd daybell
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Download a release
Head to the [Releases page](https://github.com/realximanta/daybell/releases) and grab the latest `daybell-vX.Y.Z.apk`.

---

## 📁 Project Structure

```
daybell/
├── .github/workflows/      # CI/CD – release on tag push
├── app/
│   ├── src/main/
│   │   ├── java/xyz/ximanta/daybell/
│   │   │   ├── data/       # Room entities, DAOs, repository, DataStore
│   │   │   ├── receivers/  # AlarmReceiver, SystemEventReceiver
│   │   │   ├── reminders/  # AlarmScheduler, NotificationHelper
│   │   │   ├── ui/         # Compose screens, theme, navigation, components
│   │   │   ├── MainActivity.kt
│   │   │   └── DaybellApplication.kt
│   │   ├── res/            # drawables, values, xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/wrapper/         # Gradle wrapper (jar + properties)
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── README.md
```

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.

---

## 🙏 Acknowledgements

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room](https://developer.android.com/training/data-storage/room)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [AlarmManager](https://developer.android.com/reference/android/app/AlarmManager)

---

**Daybell** – because your time matters.
