
<div align="center">

# Daybell

**Your day. On time. Offline.**

A calm, private and dependable task reminder for Android.

<br />

<!-- Main Stack -->
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-101820?style=for-the-badge&logo=materialdesign&logoColor=white)

<br />

<!-- Architecture & Storage -->
![Room](https://img.shields.io/badge/Room-FF6F00?style=for-the-badge&logo=sqlite&logoColor=white)
![DataStore](https://img.shields.io/badge/DataStore-48E0D0?style=for-the-badge&logo=android&logoColor=black)
![MVVM](https://img.shields.io/badge/Architecture-MVVM-A78BFA?style=for-the-badge)

<br />

<!-- Project Meta -->
![API 36](https://img.shields.io/badge/API-36-48E0D0?style=for-the-badge)
![Min SDK 26](https://img.shields.io/badge/Min%20SDK-26-A78BFA?style=for-the-badge)
![License: MIT](https://img.shields.io/badge/License-MIT-101820?style=for-the-badge)

<br />

<!-- Status Badges -->
[![Build Status](https://img.shields.io/github/actions/workflow/status/realximanta/daybell/.github/workflows/android.yml?branch=main&style=for-the-badge&logo=github-actions&logoColor=white&label=Build)](https://github.com/realximanta/daybell/actions)
[![Latest Release](https://img.shields.io/github/v/release/realximanta/daybell?style=for-the-badge&color=48E0D0&logo=github&logoColor=white)](https://github.com/realximanta/daybell/releases)
[![Last Commit](https://img.shields.io/github/last-commit/realximanta/daybell/main?style=for-the-badge&color=A78BFA&logo=git&logoColor=white)](https://github.com/realximanta/daybell/commits/main)

</div>

---

## ✨ Features

- **Complete Task Manager** – Add, edit, delete, complete, and restore tasks. Each task supports a title, description, date, time, category, and priority level.
- **Flexible & Reliable Reminders** – One-time or repeating alarms (daily, weekly, monthly, custom days). Exact timing with automatic Doze mode bypass.
- **Advance Alerts & Snooze** – Set reminders to notify you before the event starts. Snooze directly from the notification without breaking your recurring schedule.
- **Fully Offline First** – No account, no cloud, no network required. Android's auto-backup is explicitly disabled to guarantee your personal tasks never leave your device.
- **Local Backup & Restore** – Export your entire database (tasks, categories, reminders) to a local JSON file, and import it anytime to move your data between phones.
- **Polished UI** – Graphite, Neon Teal, and Soft Violet theme with Material 3, dynamic dark & light modes, and a clean Compose-based dashboard.

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|------------|
| Language | **Kotlin** |
| UI | **Jetpack Compose + Material 3** |
| Local storage | **Room** (tasks, reminders, categories) |
| Preferences | **DataStore** (theme, snooze, defaults) |
| Reminders | **AlarmManager** (setExactAndAllowWhileIdle) |
| CI/CD | **GitHub Actions** (Tests, Lint, Release Builds) |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio **Iguana** or newer
- JDK **17**
- Android SDK **36** (Android 16)

### Build from source
```bash
git clone https://github.com/realximanta/daybell.git
cd daybell
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔒 Privacy & Permissions

Daybell asks for minimal permissions to function reliably:
- **Notifications (`POST_NOTIFICATIONS`)**: Required to show your task reminders.
- **Exact Alarms (`SCHEDULE_EXACT_ALARM`)**: Required for precise timing. The app gracefully falls back to inexact alarms if denied.
- **Battery Optimization Exemption**: Recommended so manufacturers (like Xiaomi/POCO) don't kill background alarms.

All data is stored locally using Room and DataStore. **No data ever leaves your device.**

---

## 📁 Project Structure

```
daybell/
├── .github/workflows/      # CI/CD – Tests, Lint, Release Builds
├── app/
│   ├── src/main/
│   │   ├── java/xyz/ximanta/daybell/
│   │   │   ├── data/       # Room entities, DAOs, repository, DataStore, BackupManager
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

<div align="center">
  <b>Daybell</b> – because your time matters.
</div>
