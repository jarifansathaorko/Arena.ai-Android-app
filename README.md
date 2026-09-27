# Arena AI Mobile Companion (Android)

A high-performance, native Android companion application for **[arena.ai](https://arena.ai/)** (formerly LMSYS Chatbot Arena). Built with Jetpack Compose, Kotlin Coroutines, Room Database, and an optimized Chromium WebView engine.

---

## 📱 Features Overview

### 1. Integrated Arena Experience (`arena.ai`)
- **Native Web Engine**: Custom-tuned Android WebView with hardware-accelerated rendering and DOM storage.
- **Desktop & Mobile Mode Switcher**: Instantly toggle between a responsive mobile viewport and a full desktop dual-column comparison layout with custom User-Agent spoofing.
- **Client-Side History & SPA Sync**: Deep integration with Next.js/React routing via `doUpdateVisitedHistory`, ensuring native Android back buttons work seamlessly with web navigation.
- **Offline & Error Resilience**: Automatic connectivity detection with graceful error screens and quick-retry capabilities.
- **Process Crash Recovery**: Implements `onRenderProcessGone` to recover seamlessly if the underlying Chromium render process runs low on memory.

### 2. Prompt Library & One-Tap Injection
- **Curated Benchmark Prompts**: Built-in prompts designed to test LLMs across **Coding**, **Reasoning / Logic**, **Creative Writing**, and **Safety / Guardrails**.
- **React Synthetic Event Injector**: Employs prototype property descriptors (`HTMLTextAreaElement.prototype.value`) to bypass React synthetic event blocks and reliably fill the Arena input field while immediately enabling the Submit button.
- **Custom Prompts & Favorites**: Add, edit, favorite, and organize your own custom benchmark prompts.
- **Quick Copy**: One-tap clipboard copy if you prefer pasting manually.

### 3. Local Battle Tracker (Room Database)
- **Log Comparisons**: Record model matchups (Model A vs. Model B), winner selection (Model A, Model B, Tie, Both Bad), category tags, and notes.
- **Offline Persistence**: Fully offline-capable local SQLite storage powered by Android Jetpack Room.
- **History View**: Search and review previous model showdowns anytime.

### 4. Navigation & Tools
- **Quick Jump Chips**:
  - `⚔️ Arena Battle`: Direct jump to model battles (`https://arena.ai/`)
  - `🏆 Leaderboard`: View the Elo ratings and win rates (`https://arena.ai/leaderboard`)
  - `📜 History`: Browse past community battles (`https://arena.ai/history/search`)
  - `📝 Battle Log`: Open local showdown tracker
  - `💡 Prompts Library`: Open the benchmark prompt injector
- **Top Bar Controls**: Back, Forward, SSL Security indicator, Reload, Viewport Mode Toggle, and App Settings.
- **Privacy & Cache Tools**: Clear browsing cache, reset cookies, and wipe storage from the Settings sheet.

---

## 🛠 Architecture & Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.2 |
| **UI Framework** | Jetpack Compose (Material Design 3) |
| **Architecture** | MVVM (Model-View-ViewModel) + StateFlow |
| **Local Database** | Jetpack Room 2.7.0 (KSP code-gen) |
| **Web Rendering** | Android WebView with WebChromeClient & WebViewClient |
| **Testing** | Robolectric 4.16 & JUnit 4 |
| **Build System** | Gradle (Kotlin DSL) |

---

## 📂 Project Structure

```text
/
├── ArenaAI-debug.apk                # Ready-to-install Android APK artifact
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt                  # App entry point & scaffold
│   │   │   │   ├── data/
│   │   │   │   │   ├── db/
│   │   │   │   │   │   ├── AppDatabase.kt          # Room Database definition
│   │   │   │   │   │   ├── BattleLogDao.kt         # Battle logs CRUD queries
│   │   │   │   │   │   └── PromptDao.kt            # Prompts CRUD queries
│   │   │   │   │   └── model/
│   │   │   │   │       ├── BattleRecord.kt         # Battle entity model
│   │   │   │   │       └── PromptItem.kt           # Benchmark prompt entity model
│   │   │   │   └── ui/
│   │   │   │       ├── ArenaViewModel.kt           # App state & business logic
│   │   │   │       ├── components/
│   │   │   │       │   ├── ArenaTopBar.kt          # Top navigation bar & quick chips
│   │   │   │       │   ├── ArenaWebView.kt         # Optimized WebView composable
│   │   │   │       │   ├── PromptLibrarySheet.kt   # Benchmark prompts bottom sheet
│   │   │   │       │   ├── BattleTrackerDialog.kt  # Match logger modal dialog
│   │   │   │       │   └── SettingsBottomSheet.kt  # Cache & viewport preferences
│   │   │   │       └── theme/
│   │   │   │           ├── Color.kt                # Material 3 & Arena color tokens
│   │   │   │           ├── Theme.kt                # App color schemes (Dark/Light)
│   │   │   │           └── Type.kt                 # Typography definitions
│   │   │   ├── res/                                # Strings, adaptive icons & drawables
│   │   │   └── AndroidManifest.xml                 # App manifest & permissions
│   │   └── test/                                   # Local JVM & Robolectric unit tests
│   └── build.gradle.kts                            # App module dependencies
└── settings.gradle.kts                             # Gradle project configuration
```

---

## 📦 How to Download & Install the APK

1. Locate **`ArenaAI-debug.apk`** in the project's root folder in the left file explorer.
2. Right-click (or click the three dots `⋮`) and select **Download**.
3. Transfer the `.apk` file to your Android smartphone or tablet (or open inside an emulator).
4. Tap the file to install (enable *"Install unknown apps"* if prompted).
