<div align="center">

<img width="1280" height="640" alt="Expressive Island" src="https://github.com/user-attachments/assets/d801de28-eac6-4ffd-8474-55d9a8af4dc3" />

# Expressive Island

**A Material Expressive dynamic island for Android.**

Expressive Island is an actively developed fork of [Expressive Cutout](https://github.com/EvanKoe/expressive-cutout) by [EvanKoe](https://github.com/EvanKoe).

It transforms the camera cutout on your Android phone into a small, living island for notifications, music, calls, timers, charging events, and other system activity.

Built with **Jetpack Compose** and inspired by Google's **Material Expressive** design language, Expressive Island focuses on expressive animations, flexible customisation, and a polished Dynamic Island experience that feels native to Android.

The project is completely offline. **Expressive Island does not request the `INTERNET` permission**, meaning the app has no ability to upload your notification content, usage data, analytics, or other information to a remote server.

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
* [Expressive Island vs Original](#expressive-island-vs-original)
* [Original Expressive Cutout](#original-expressive-cutout)
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
| **Privacy**             | No INTERNET permission · no analytics · no tracking · no remote data collection                                                                                                          |
| **Testing**             | Test notifications · notification actions · inline replies · test ongoing calls · test incoming calls · system-event previews                                                            |

---

# Screenshots

|                                                                                                                                                             |                                                                                                                                                             |                                                                                                                                                             |
| ----------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| <img width="864" height="1939" alt="Expressive Island screenshot" src="https://github.com/user-attachments/assets/9f7e4724-bd91-4486-8f0e-9500906024d2" />  | <img width="1080" height="2424" alt="Expressive Island screenshot" src="https://github.com/user-attachments/assets/e803d2a2-8e9b-41e0-8688-54affaefa355" /> | <img width="864" height="1939" alt="Expressive Island screenshot" src="https://github.com/user-attachments/assets/84f40ce3-7ef0-41b4-9d89-3790008de5bc" />  |
| <img width="1080" height="2424" alt="Expressive Island screenshot" src="https://github.com/user-attachments/assets/7c4b132c-54ec-471b-a4c3-bf7ddc7d8846" /> | <img width="1080" height="2424" alt="Expressive Island screenshot" src="https://github.com/user-attachments/assets/1d59ac95-0dfb-4b4b-9b8c-2814eb436da1" /> | <img width="1080" height="2424" alt="Expressive Island screenshot" src="https://github.com/user-attachments/assets/33542dec-9d6b-456a-bc14-c779c1d07a00" /> |
| <img width="1080" height="2424" alt="Expressive Island screenshot" src="https://github.com/user-attachments/assets/e33224c9-fe29-4fa2-b580-e0e8a10c7237" /> |                                                                                                                                                             |                                                                                                                                                             |

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

### Releases

Get the latest builds from the project's GitHub releases:

**[Expressive Island Releases](https://github.com/hustlewithvikram/expressive-island/releases)**

### Source code

The complete source code is available here:

**[Expressive Island on GitHub](https://github.com/hustlewithvikram/expressive-island)**

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
| **Notification access**         | Mirrors notifications into the island                         |
| **Accessibility service**       | Displays the island overlay                                   |
| **Ignore battery optimisation** | Helps prevent the app from being terminated in the background |
| **Post notifications**          | Used for testing functionality                                |
| **Network state**               | Detects Wi-Fi connectivity changes                            |

### Privacy

Expressive Island does **not** request the Android `INTERNET` permission.

That means the app cannot connect to a remote server to upload:

* Notification content
* Usage information
* Analytics
* Personal data
* Tracking information

The project is designed to keep its functionality on-device.

---

# Expressive Island vs Original

Expressive Island is a **fork**, not an independent project created from scratch.

It is based on the original work of **EvanKoe's Expressive Cutout**.

The original project provided the foundation for the Dynamic Island experience, including much of the original:

* Material Expressive design
* Notification handling
* Overlay architecture
* System events
* Music tile
* Phone tile
* Timer tile
* Animation system
* Customisation system
* Permission flow

Expressive Island exists to continue development from that foundation.

The fork may introduce changes, fixes, redesigns, experimental functionality, and features that differ from the original project.

---

# Original Expressive Cutout

## Created by EvanKoe

**Expressive Cutout** is the original project from which Expressive Island was forked.

The original project introduced the idea of an offline Android dynamic island built around Google's Material Expressive design language.

### Original project

**GitHub:**
https://github.com/EvanKoe/expressive-cutout

**Releases:**
https://github.com/EvanKoe/expressive-cutout/releases

**Issues:**
https://github.com/EvanKoe/expressive-cutout/issues

**Stars:**
https://github.com/EvanKoe/expressive-cutout/stargazers

**Author:**
https://github.com/EvanKoe

### Original Discord community

The original project also has a Discord community for feedback, feature requests, bug reports, discussion, and sharing:

https://discord.gg/SNfcuuJeYF

If you are looking for the original project, its original releases, or its original community, please use the links above.

---

# Credits & Inspiration

Expressive Island would not exist without the work that came before it.

## Original project

Special thanks to **EvanKoe** for creating Expressive Cutout and providing the foundation on which Expressive Island is built.

**Original repository:**
https://github.com/EvanKoe/expressive-cutout

## Inspiration

The project was also inspired by other Android applications and open-source projects, particularly in terms of Dynamic Island concepts, UI, and Material Expressive design.

### Dynamic Spot

https://play.google.com/store/apps/details?id=com.jamworks.dynamicspot&hl=en

### Material Capsule

https://play.google.com/store/apps/details?id=com.pryshedko.mtisland&hl=en

### Sameerasw's Essential

Special thanks to [Sameerasw's Essential](https://github.com/sameerasw/essentials) for its excellent Material You Expressive implementation.

It has been a major source of inspiration for the visual direction and design language of this project.

---

# Development

Expressive Island is developed as an open-source project.

Contributions, bug reports, feature requests, testing, and feedback are welcome.

### GitHub

**Repository:**
https://github.com/hustlewithvikram/expressive-island

**Issues:**
https://github.com/hustlewithvikram/expressive-island/issues

**Pull requests:**
https://github.com/hustlewithvikram/expressive-island/pulls

**Releases:**
https://github.com/hustlewithvikram/expressive-island/releases

**Discussions:**
https://github.com/hustlewithvikram/expressive-island/discussions

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

*An open-source, offline, Material Expressive dynamic island for Android.*

Built on the foundation of **Expressive Cutout by EvanKoe**.

[GitHub](https://github.com/hustlewithvikram/expressive-island) · [Releases](https://github.com/hustlewithvikram/expressive-island/releases) · [Issues](https://github.com/hustlewithvikram/expressive-island/issues)

</div>
