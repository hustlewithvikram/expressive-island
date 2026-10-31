<div align="center">

<img width="1280" height="640" alt="Expressive Island" src="./app/src/main/assets/expressive-island-banner.png" />

# Expressive Island

**A Material Expressive dynamic island for Android.**

Expressive Island turns your phone's camera cutout into a live area for notifications, music, calls, timers, and system events. Built with Jetpack Compose, it is a community-developed fork of [Expressive Cutout](https://github.com/EvanKoe/expressive-cutout), focused on customisation, smooth animations, and an Android-native feel.

Notification content is handled on-device. Internet access is used by the in-app updater to check for and download updates, not to upload notification content or usage analytics.

[![Download](https://img.shields.io/badge/Download-latest%20release-005AC1?style=for-the-badge\&logo=github\&logoColor=white)](https://github.com/hustlewithvikram/expressive-island/releases)
[![Stars](https://img.shields.io/github/stars/hustlewithvikram/expressive-island?style=for-the-badge)](https://github.com/hustlewithvikram/expressive-island/stargazers)
[![Fork](https://img.shields.io/github/forks/hustlewithvikram/expressive-island?style=for-the-badge)](https://github.com/hustlewithvikram/expressive-island/network/members)
[![Issues](https://img.shields.io/github/issues/hustlewithvikram/expressive-island?style=for-the-badge)](https://github.com/hustlewithvikram/expressive-island/issues)
[![License](https://img.shields.io/badge/license-GPLv3-blue?style=for-the-badge)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?style=for-the-badge\&logo=kotlin\&logoColor=white)](https://kotlinlang.org)

</div>

---

## Table of contents

* [About](#about)
* [Features](#features)
* [Screenshots](#screenshots)
* [Download](#download)
* [Requirements & Setup](#requirements--setup)
* [Permissions](#permissions)
* [Help with translations](#help-with-translations)
* [Expressive Island vs Expressive Cutout](#expressive-island-vs-expressive-cutout)
* [Credits & Inspiration](#credits--inspiration)
* [License](#license)

---

# About

Expressive Island is a community-driven continuation of the original **Expressive Cutout** project.

The project takes the original foundation and continues development under a new identity, with room for new features, improvements, bug fixes, experiments, and Android-specific enhancements.

The goal is simple:

> **Make Android's camera cutout feel like a useful, expressive part of the interface instead of just a hole in the screen.**

Notifications can appear directly around the cutout, while live tiles provide quick access to information such as:

* Currently playing music
* Active phone calls
* Incoming calls
* Running timers
* Charging state
* Battery warnings
* Wi-Fi changes
* Headphone connections
* USB connections
* Device unlock events

Everything is designed around Material Expressive shapes, motion, colours, and customisation.

---

# Features

| Category                | Features                                                                                                                                                                                 |
| ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Notifications**       | Mirrors notifications in the island · notification icons · automatic expansion · title and text · compact icon-only mode · notification actions · inline replies · swipe-to-dismiss      |
| **Music**               | Song title and artist · album artwork · optional spinning artwork · playback state · previous / play-pause / next controls                                                               |
| **Phone calls**         | Caller name · contact photo · live call duration · hang-up action · incoming call interface · answer / decline actions · open in-call screen · optional two-row incoming-call layout     |
| **Timers**              | Live countdown from the clock app · Android 16+ live-update timer support · real clock-app actions · Add 1 min · Reset · Pause / Resume state                                            |
| **System events**       | Charging started / stopped · low battery · Wi-Fi connected / disconnected · headphones connected / disconnected · USB connected / removed · device unlocked                              |
| **Event customisation** | Material icons · icon search · animated icons · animation looping · individual colours · event duration · Material You colours                                                           |
| **Island sizing**       | Independent normal / expanded width · independent normal / expanded height · corner radius controls · per-corner radius · horizontal position · vertical position                        |
| **Appearance**          | Solid backgrounds · gradients · gradient direction · opacity · shadows · outline stroke · custom stroke colour · Material You colours · preset colours · custom HEX colours              |
| **Themes**              | Light · dark · system                                                                                                                                                                    |
| **Action buttons**      | Expressive tonal · Expressive filled · Material You · outlined styles · multiple reply-field styles · button height · button colours · cancel button positioning · tile-specific colours |
| **Animations**          | Expressive spring · ease-in-out · slow / default / fast speeds · bounce size · configurable animation duration                                                                           |
| **Gestures**            | Auto-collapse · persistent expanded state · swipe up to shrink · swipe sideways to dismiss · configurable dismiss direction · configurable size behaviour                                |
| **Lockscreen**          | Optional complete island removal on the lockscreen to ensure the overlay is torn down when hidden                                                                                        |
| **Privacy**             | On-device notification handling · updater network access only · no analytics · no tracking · no upload of notification content or usage data                                               |
| **Testing**             | Test notifications · notification actions · inline replies · test ongoing calls · test incoming calls · system-event previews                                                            |

---

# Screenshots

| Screenshot 1 | Screenshot 2 | Screenshot 3 |
|:---:|:---:|:---:|
| <img alt="Expressive Island screenshot 1" src="https://github.com/user-attachments/assets/9f7e4724-bd91-4486-8f0e-9500906024d2" width="250" /> | <img alt="Expressive Island screenshot 2" src="https://github.com/user-attachments/assets/e803d2a2-8e9b-41e0-8688-54affaefa355" width="250" /> | <img alt="Expressive Island screenshot 3" src="https://github.com/user-attachments/assets/84f40ce3-7ef0-41b4-9d89-3790008de5bc" width="250" /> |
| Screenshot 4 | Screenshot 5 | Screenshot 6 |
| <img alt="Expressive Island screenshot 4" src="https://github.com/user-attachments/assets/7c4b132c-54ec-471b-a4c3-bf7ddc7d8846" width="250" /> | <img alt="Expressive Island screenshot 5" src="https://github.com/user-attachments/assets/1d59ac95-0dfb-4b4b-9b8c-2814eb436da1" width="250" /> | <img alt="Expressive Island screenshot 6" src="https://github.com/user-attachments/assets/33542dec-9d6b-456a-bc14-c779c1d07a00" width="250" /> |
| Screenshot 7 |  |  |
| <img alt="Expressive Island screenshot 7" src="https://github.com/user-attachments/assets/e33224c9-fe29-4fa2-b580-e0e8a10c7237" width="250" /> |  |  |

---

# Download

<div align="center">

[![Download the latest release](https://img.shields.io/badge/Download-latest%20release-005AC1?style=for-the-badge\&logo=github\&logoColor=white)](https://github.com/hustlewithvikram/expressive-island/releases)

</div>

### Android requirements

Expressive Island requires:

* **Android 10 (API 29) or newer**
* Notification access
* Accessibility service
* Optional battery optimisation exemption

On first launch, Expressive Island guides you through the permissions required for the island to work.

---

# Requirements & Setup

After installing Expressive Island, the app will guide you through the required setup.

### 1. Notification access

Required to mirror notifications into the island.

### 2. Accessibility service

Used to display the island as an overlay above other applications.

Expressive Island does **not** use the accessibility service to read your screen contents.

### 3. Battery optimisation

Optional, but recommended.

Some Android manufacturers aggressively terminate background applications. Allowing Expressive Island to ignore battery optimisation can improve reliability.

---

# Permissions

Expressive Island intentionally keeps its permissions to a minimum.

| Permission                      | Purpose                                                       |
| ------------------------------- | ------------------------------------------------------------- |
| **Notification access**         | Reads notifications so they can be displayed and interacted with in the island |
| **Accessibility service**       | Displays the island overlay and handles island gestures; it does not read screen contents |
| **Ignore battery optimisation** | Optional; helps prevent the overlay service from being stopped in the background |
| **Post notifications**          | Optional; posts test notifications when you explicitly trigger a test |
| **Internet**                    | Checks for and downloads app updates through the in-app updater |
| **Request install packages**    | Lets the updater open Android's installer for a downloaded APK; installation still requires confirmation |
| **Network state**               | Reads connection state for Wi-Fi/network events |
| **Wi-Fi state**                 | Reads Wi-Fi state and details needed by the Wi-Fi event tile |
| **Nearby Wi-Fi devices**        | Supports Wi-Fi APIs on Android 13+; marked `neverForLocation` |
| **Bluetooth connect / scan**    | Supports Bluetooth connection-state events; scan permission is marked `neverForLocation` |
| **Fine / approximate location** | Legacy compatibility for older Wi-Fi APIs; not used to record or track location |
| **Vibration**                   | Provides haptic feedback |
| **Shizuku API**                 | Declares optional integration with compatible Shizuku versions |

### Privacy

Expressive Island requests `INTERNET` access only for its in-app updater, which checks for and downloads app releases. The app does not upload notification content or usage analytics. Notification content is handled locally for display and is not sent to the updater.

The app also declares `REQUEST_INSTALL_PACKAGES` so the updater can hand a downloaded APK to Android's package installer. Android still requires you to confirm installation.

---

# Expressive Island vs Expressive Cutout

Expressive Island is a community-developed fork of **Expressive Cutout by EvanKoe**. It builds on the original project's notification, overlay, music, call, timer, event, animation, and customisation foundations while adding ongoing fixes and new features.

---

# Credits & Inspiration

Thanks to **[EvanKoe](https://github.com/EvanKoe)** for creating [Expressive Cutout](https://github.com/EvanKoe/expressive-cutout), the foundation of this project. Design inspiration also comes from [Dynamic Spot](https://play.google.com/store/apps/details?id=com.jamworks.dynamicspot), [Material Capsule](https://play.google.com/store/apps/details?id=com.pryshedko.mtisland), and [Sameerasw's Essentials](https://github.com/sameerasw/essentials), especially its Material You Expressive design.

---

# Development

Contributions, bug reports, feature requests, testing, and feedback are welcome. If you would like to contribute, open an issue or submit a pull request.

---

# Help with translations

Expressive Island currently includes **English, Hindi, and Marathi**. Would you like to help make the app accessible to more people? Contributions to improve existing translations or add another language are welcome.

- Browse the app's string resources in [`app/src/main/res/values/strings.xml`](app/src/main/res/values/strings.xml).
- Use the existing locale folders such as [`values-hi`](app/src/main/res/values-hi) and [`values-mr`](app/src/main/res/values-mr) as examples when adding or updating a translation.
- Keep every string resource name unchanged, preserve formatting placeholders such as `%1$s` and `%d`, and escape XML characters correctly.
- Test the app with the contributed language before opening a pull request.

Not sure where to start? Open an issue or pull request and mention the language you'd like to help with. Even small corrections are useful.

---

# License

Expressive Island is free software licensed under the **GNU General Public License v3.0**.

You may:

* Use the software
* Study the source code
* Modify the software
* Share the software
* Distribute modified versions

Any distributed derivative must remain under the same licence.

Because Expressive Island is a fork of Expressive Cutout, the original project's licensing and attribution requirements must also be respected.

See the [LICENSE](LICENSE) file for the complete terms.

---

<div align="center">

**Expressive Island**

*An open-source, Material Expressive dynamic island for Android.*

Built on the foundation of **Expressive Cutout by EvanKoe**.

[GitHub](https://github.com/hustlewithvikram/expressive-island) · [Releases](https://github.com/hustlewithvikram/expressive-island/releases) · [Issues](https://github.com/hustlewithvikram/expressive-island/issues)

</div>
