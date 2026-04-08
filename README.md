# DateWise

**Innovative Software Ecosystem for Expiry Date Tracking**

> An automated hardware-software bridge that transforms the physical product label into an intelligent data carrier — eliminating manual date entry and reducing household food waste.

---

| | |
|---|---|
| **Author** | Eva Georgieva ([e.ivaylova@gmail.com](mailto:e.ivaylova@gmail.com)) |
| **School** | Technology School "Electronic Systems" at TU-Sofia |
| **Date** | March 2026 |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose |
| **Architecture** | MVVM + Clean Architecture |
| **Min SDK** | Android 7.0 (API 24) |

---

## 📑 Table of Contents

- [Overview](#-overview)
- [The Problem](#-the-problem)
- [How It Works](#-how-it-works)
- [System Architecture](#-system-architecture)
- [DateWise PDA — Retailer Terminal](#-datewise-pda--retailer-terminal)
- [DateWise Consumer App — Household Assistant](#-datewise-consumer-app--household-assistant)
- [Architecture Pattern — MVVM](#-architecture-pattern--mvvm)
- [Technology Stack](#-technology-stack)
- [Installation & Usage](#-installation--usage)
- [Key Technical Challenges](#-key-technical-challenges)
- [Development Stages](#-development-stages)
- [Results & Future Development](#-results--future-development)
- [Project Structure](#-project-structure)

---

## 🔍 Overview

DateWise is a practical ecosystem for **automated expiry date tracking** that eliminates the need for users to manually enter dates after shopping. The solution consists of two interconnected components:

- **DateWise PDA** — a terminal application (optimized for Sunmi V2 Pro), used at the retail point of sale.
- **DateWise Consumer App** — a mobile Android application designed for the end consumer.

When a sale occurs, the retailer scans the product and enters the expiry date. The terminal prints a label containing the standard barcode along with an additional **DataMatrix code** encoding the expiry date for that specific batch. The customer scans the label with the mobile app, and the product is automatically added to a local virtual **Fridge**. The app tracks remaining days and sends timely reminders when a product is approaching its expiry date.

---

## ⚠️ The Problem

Global research indicates that **over one-third of all food produced worldwide is discarded**. A significant portion of this waste is generated at the household level, primarily due to:

1. **Forgetting** about products buried at the back of the fridge or pantry.
2. **Misunderstanding** or overlooking expiry dates printed on packaging (confusion between *"Use By"* and *"Best Before"*).

### Why Existing Solutions Fail

Although the market offers dozens of kitchen inventory apps, they all suffer from a critical flaw — **data entry friction**:

- Users must manually enter product names after every shopping trip
- Scan barcodes individually, then find and input the date from the packaging
- **Over 90% of users abandon** such apps within the first month

### Current Market Landscape

| Solution Type | Limitation |
|---|---|
| **Standalone pantry apps** | Rely entirely on user input. Can find product names via EAN-13 barcode, but expiry dates are *not* encoded in factory barcodes (unique per batch). |
| **Industrial systems (GS1)** | Large retail chains use GS1 DataBar/GS1-128 standards, but this data serves only internal logistics — it **never reaches the consumer's phone**. |

**DateWise bridges this gap** by combining the hardware PDA approach with consumer software. The decentralized approach requires no expensive server infrastructure and is **fully GDPR-compliant**.

---

## ⚙️ How It Works

### Retailer Side (DateWise PDA)

1. Open DateWise PDA — the barcode scanner is available on the main screen.
2. Upon scanning, the app **automatically fetches** the product photo and name from [Open Food Facts](https://openfoodfacts.org).
3. The cashier selects or enters the expiry date via a system **Date Picker**. The product is added to the digital transaction list.
4. Upon finalizing, the cashier presses the **Print** option.
5. The printer outputs a label containing the standard barcode and a **DataMatrix code** with the expiry date. The label is attached to the product.

### Consumer Side (DateWise App)

1. Open the app and navigate to the **Scan** screen via the bottom navigation bar.
2. Point the phone camera at the product label.
3. Data is decoded automatically and the app switches to the **Fridge** screen.
4. The **Fridge** screen displays all products with remaining days and color indicators:
   - 🔴 **Red** — expires today or tomorrow
   - 🟡 **Yellow** — expiring soon
   - 🟢 **Green** — within safe range
5. The **Calendar** screen provides a monthly view of expiry date peaks for better meal planning.
6. The system generates **local notifications** even when the app is closed: *"The expiry date of [product] is approaching."*
7. The **Shopping List** screen closes the user cycle, allowing organization of future purchases.

---

## 🏗️ System Architecture

DateWise is designed as a **Stateless Distributed Architecture** without a centralized data store, based on the **"Data-as-a-Token"** concept. The physical carrier of transport data is the product label — **no cloud REST API is required**.

```
┌─────────────────┐                           ┌─────────────────────┐
│  DateWise PDA   │    DataMatrix Label        │  DateWise Consumer  │
│  (Sunmi V2 Pro) │ ──────────────────────►    │  (Android Phone)    │
│                 │    (Physical Medium)        │                     │
│  • Scans        │                            │  • Scans label      │
│  • Encodes      │                            │  • Decodes data     │
│  • Prints       │                            │  • Tracks expiry    │
│    labels       │                            │  • Sends reminders  │
└─────────────────┘                            └─────────────────────┘
```

---

## 📟 DateWise PDA — Retailer Terminal

Designed for retail store employees on the **Sunmi V2 Pro** terminal.

| Module | Functionality |
|---|---|
| **UI / Presentation** | Jetpack Compose + Navigation Compose. Routes to product entry screens, current transaction list, and print finalization screen. |
| **Camera & Scan** | CameraX (Preview) + ML Kit (ImageAnalyzer). Real-time EAN-13 and EAN-8 barcode recognition. |
| **Network (Open Food Facts)** | Retrofit + OkHttp. Async GET request to `api/v0/product/{barcode}.json`. Deserialization via Gson; image rendering via Coil. |
| **Data Encoding** | Collects Product objects from the transaction, serializes them into a structured string, and generates a DataMatrix Bitmap via ZXing Core. |
| **Hardware Printer** | `SunmiPrinterHelper` — manages the thermal printer via Sunmi PrinterX SDK. Sends monochrome Bitmap matrix to the print head. |

---

## 📱 DateWise Consumer App — Household Assistant

Designed for household use on any **Android smartphone** (API 24+).

| Module | Functionality |
|---|---|
| **Local Storage (Room)** | SQLite database with `Product` and `ShoppingItem` entities. DAO interfaces accessible via `suspend` functions. Singleton `AppDatabase`. |
| **Dashboard / Fridge** | Main screen. Observes Room DB via Kotlin `Flow`. On scanning a new product, `FridgeScreen` updates instantly, sorting items by expiry date. |
| **Calendar UI** | `composecalendar` library. Visualizes expiry dates by month with color coding based on expiry urgency. |
| **Notification / Alert** | `AlarmManager` / `WorkManager`. Analyzes expiry dates even when the app is closed and generates local system notifications. |

---

## 🧩 Architecture Pattern — MVVM

Both applications strictly follow the **Model–View–ViewModel (MVVM)** pattern, recommended by Google for modern Android apps.

```
┌──────────────────────────────────────────────────────────────┐
│                     View (Compose)                           │
│  @Composable functions • Collects StateFlow • No logic      │
│  Captures user actions → passes as Events to ViewModel      │
├──────────────────────────────────────────────────────────────┤
│                      ViewModel                               │
│  Retrieves data from Repository • Business logic             │
│  Exposes results via StateFlow • Survives rotation           │
├──────────────────────────────────────────────────────────────┤
│                    Model (Data Layer)                         │
│  Room Entities (@Entity) • DAO interfaces (suspend)          │
│  Repository — Single Source of Truth                         │
└──────────────────────────────────────────────────────────────┘
```

- **Model (Data Layer):** Handles business logic and data management. Includes Room Entities (`@Entity`), DAO interfaces with SQL queries as Kotlin `suspend` functions, and `Repository` — a Single Source of Truth abstracting the local database and network requests.

- **ViewModel (Logic Layer):** Retrieves data from the Repository, applies business logic (calculating days until expiry, filtering, aggregation for QR codes) and exposes results via reactive `StateFlow` streams. Survives screen rotation, preventing data loss.

- **View (Presentation Layer):** Pure `@Composable` visualization functions with no business logic. They `collect` StateFlow from the ViewModel and automatically re-render only the changed elements.

---

## 🛠️ Technology Stack

| Technology | Rationale |
|---|---|
| **Kotlin** | Official Google-recommended language for Android since 2019. Coroutines eliminate Callback Hell for async operations. Type & Null Safety prevents `NullPointerException` at compile time. |
| **Jetpack Compose** | Declarative UI framework replacing XML files. Describes UI through Kotlin functions that accept State parameters. Automatically re-renders only changed elements. |
| **Room Database** | ORM layer over SQLite. Uses annotations (`@Entity`, `@Dao`) and KSP for auto-generating boilerplate. Validates SQL queries at compile time. `Flow` representations notify Compose on every data change. |
| **CameraX + ML Kit** | CameraX abstracts Camera2 API complexity. ML Kit Barcode Scanning recognizes 1D and 2D codes at any angle and in low light. On-device inference ensures offline operation. |
| **ZXing Core** | Open-source standard for generating Code 128 and DataMatrix codes. Provides control over `ErrorCorrectionLevel` — critical for quality thermal printing. |
| **Retrofit + OkHttp** | Type-safe HTTP client. Combined with `GsonConverterFactory`, translates JSON responses directly into Kotlin Data Classes. OkHttp provides reliable caching. |
| **Sunmi PrinterX SDK** | Official hardware SDK for managing the built-in thermal printer on Sunmi V2 Pro. Sends monochrome Bitmap matrices to the print head via ESC/POS protocol. |
| **Coil** | Image loading library optimized for Jetpack Compose infrastructure. |

---

## 📦 Installation & Usage

### DateWise PDA (For Retailers)

The app is packaged as a standard `.apk` file and is installed directly on the **Sunmi V2 Pro** terminal. On startup, it requires only system permissions for:
- 📷 Camera access
- 🖨️ Printer management

> **No account registration is required** to maintain checkout speed.

### DateWise Consumer App (For Households)

The app is intended for distribution via **Google Play Store**. Compatible with Android devices running **version 7.0 (API 24)** and newer.

### Maintenance

Product support is ensured through continuous updates following the latest Gradle and Android SDK versions. The modular architecture allows updating individual segments — for example, migrating to a more stable code generation algorithm — without affecting other functionality.

---

## 🧪 Key Technical Challenges

### Challenge 1 — Optical Data Transfer Limitations (Payload Size)

The amount of information encoded in a DataMatrix code is limited by the physical characteristics of the print. **Solution:** Implemented a data minification mechanism with specialized mappers. When a standard EAN barcode exists, the consumer app retrieves the product name locally via Open Food Facts instead of encoding it in the label.

### Challenge 2 — Hardware Integration via Poorly Documented SDK

The Sunmi V2 Pro's thermal printer SDK (`sunmi.printerx`) is poorly documented and does not cover edge cases in Jetpack Compose architectures. **Solution:** Custom handling of async print jobs to prevent queue loss during app sleep, and ZXing matrix-to-monochrome conversion for thermal paper output.

### Challenge 3 — Optical Recognition Reliability

Reading thermally printed codes with a mobile phone camera depends on lighting and reflections. The classic ZXing library did not produce satisfactory results. **Solution:** Refactored to **Google ML Kit Barcode Scanning API**, which significantly increased the success rate — including slightly damaged or skewed labels.

### Challenge 4 — Lifecycle Management and UI State Preservation

Android Compose requires state-driven design thinking. Issues were discovered with State Hoisting during screen rotation or app minimization. **Solution:** The combination of Room Database, Kotlin Coroutines, and Kotlin `Flow` ensured reactive UI notification on every data change.

---

## 📅 Development Stages

The development lifecycle follows **Agile methodologies** across five sequential stages:

| Stage | Description |
|---|---|
| **1. Research** | Analyzed market needs, identified the data entry bottleneck, selected Kotlin + Jetpack Compose + DataMatrix specification. |
| **2. Architecture Design** | Defined database schemas for both apps. Built `Product` and `ShoppingItem` models. Applied Clean Architecture principles. |
| **3. PDA Development** | Implemented real-time barcode scanning (CameraX + ML Kit), Open Food Facts API (Retrofit2), purchase aggregation, and Sunmi printer integration. |
| **4. Consumer App Development** | Built optimized scan screen, virtual Fridge with color-coded indicators, Calendar module, and Shopping List. |
| **5. Integration Testing** | Integrated and tested both components. Resolved date format issues via unified `DateFormatter` class. Fixed printer communication instabilities. |

---

## 🚀 Results & Future Development

### Key Result

DateWise creates an innovative hardware-software link, transforming the physical product label into an intelligent data carrier. The implementation proves that a complex logistics task can be executed **entirely offline** — without centralized server infrastructure.

### Available Implementations

- ✅ **DateWise PDA** — intelligent checkout solution for Sunmi V2 Pro, tested in a real retail environment.
- ✅ **DateWise (Client)** — mobile app for Android smartphones, functioning as a digital household assistant.

### Future Development Roadmap

| Feature | Description |
|---|---|
| 🤖 **AI Recipes** | LLM integration suggesting recipes based on products expiring within 24 hours. Transforms the app from an information tool into an active kitchen assistant. |
| ☁️ **Cloud Sync & Shared Fridge** | Firebase Firestore integration for shared inventory among household members in real time, eliminating duplicate purchases. |
| 🖥️ **Expanded PDA Compatibility** | Standardized API endpoints for integration with Windows-based POS systems of global supermarket chains. |
| 💰 **Dynamic Pricing** | Automatically reducing the product price proportionally as it approaches its expiry date. |

---

## 📁 Project Structure

```
DateWiseFull/
├── datewiseDocumentation.pdf       # Full project documentation (Bulgarian)
├── README.md                       # This file
└── DateWiseFull/
    ├── DateWise/                   # 📱 Consumer mobile app (Android)
    │   ├── app/
    │   │   └── src/main/
    │   │       ├── java/com/       # Kotlin source code
    │   │       ├── res/            # Android resources
    │   │       └── AndroidManifest.xml
    │   ├── build.gradle.kts
    │   ├── gradle/
    │   └── settings.gradle.kts
    │
    └── DateWisePOS/                # 📟 PDA terminal app (Sunmi V2 Pro)
        ├── app/
        │   └── src/main/
        │       ├── java/com/       # Kotlin source code
        │       ├── res/            # Android resources
        │       └── AndroidManifest.xml
        ├── build.gradle.kts
        ├── gradle/
        └── settings.gradle.kts
```

---

<p align="center">
  <em>DateWise demonstrates that with the right combination of hardware and software technologies, it is possible to achieve a real digital green transition toward more conscious mass consumption, laying the foundation for a zero-waste eco-culture.</em>
</p>
