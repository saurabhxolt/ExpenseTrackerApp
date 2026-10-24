# 🚀 Developer Onboarding Guide — Expense Tracker App

Welcome to the **Expense Tracker App** engineering team! This guide will get you set up and building within 15 minutes.

---

## 📋 Prerequisites & Tooling

| Component | Minimum Version | Recommended Path |
| :--- | :--- | :--- |
| **Operating System** | Windows 10/11 / macOS / Linux | Any modern development OS |
| **JDK** | JDK 17 (Temurin / Eclipse) | `E:\ExpenseTrackerApp\jdk-tmp\jdk-17.0.10+7` |
| **Android SDK** | API 34 (Android 14) | Installed via SDK Manager or CLI tools |
| **Build System** | Gradle 8.4 via Gradle Wrapper | `./gradlew` / `gradlew.bat` |
| **IDE** | Android Studio Iguana+ or VS Code / AGY | With Kotlin & Compose plugins |

---

## 🛠️ Step 1: Environment Configuration

Create a `local.properties` file in the root directory:

```properties
sdk.dir=C:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk
```
*(Or point to your local SDK location, e.g. `E:\\ExpenseTrackerApp\\android-sdk`).*

Ensure `JAVA_HOME` points to JDK 17 in PowerShell:
```powershell
$env:JAVA_HOME="E:\ExpenseTrackerApp\jdk-tmp\jdk-17.0.10+7"
```

---

## ⚡ Step 2: 1-Click Build & Test Commands

Run the following commands in the project root:

```powershell
# 1. Clean and build Debug APK
.\gradlew.bat assembleDebug

# 2. Run all unit tests (Parser, Repositories, ViewModels)
.\gradlew.bat test

# 3. Install Debug APK directly to connected device or emulator
.\gradlew.bat installDebug
```

Output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📂 Source Code Tour

```
app/src/main/java/com/expensetracker/app/
├── core/
│   ├── data/repository/       # TransactionRepository, BudgetRepository
│   ├── database/              # Room database, SQLCipher factory, DAOs, Entities
│   ├── di/                    # Hilt dependency injection modules (DatabaseModule)
│   ├── ui/theme/              # Design system: Color, Type, Theme, CategoryEmojiProvider
│   └── utils/                 # Permission and date utilities
├── ingestion/
│   ├── notification/          # ExpenseNotificationListenerService (Status bar push capture)
│   ├── parser/                # RegexTransactionParser, CategoryClassifier, SubscriptionMatcher
│   ├── sms/                   # SmsReceiver (SMS PDU broadcast), SmsScanner (Inbox sync)
│   └── worker/                # DailyReviewWorker (WorkManager periodic evening review)
└── features/
    ├── main/                  # MainScreen (Material3 NavigationBar, BackHandler)
    ├── dashboard/             # DashboardScreen, DashboardViewModel
    ├── transactions/          # TransactionsScreen, TransactionsViewModel
    ├── budgets/               # BudgetsScreen, BudgetsViewModel
    ├── analytics/             # AnalyticsScreen, AnalyticsViewModel
    ├── categories/            # CategoriesScreen, CategoriesViewModel
    ├── subscriptions/         # SubscriptionsScreen, SubscriptionsViewModel
    ├── settings/              # SettingsScreen, SettingsViewModel
    ├── security/              # BiometricLockManager
    ├── backup/                # BackupManager (SAF JSON exporter)
    └── reports/               # ReportsExporter (CSV spreadsheet generator)
```

---

## 📏 Kotlin & Compose Coding Standards

1. **Unidirectional Data Flow**: Composables receive immutable StateFlow models and emit event callbacks. Never pass ViewModels deeply down the composable tree.
2. **Explicit Nullability**: Always leverage Kotlin's null-safety. Avoid forced unwrapping (`!!`).
3. **Coroutine Dispatchers**: Never perform disk I/O or database queries on `Dispatchers.Main`. All DAOs and repository calls must execute on `Dispatchers.IO`.
4. **No Network Imports**: Never import `java.net.*`, `okhttp3.*`, `retrofit2.*`, or any networking libraries. This app is strictly offline.
