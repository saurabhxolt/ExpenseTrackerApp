# 📱 Expense Tracker App — Privacy-First Android Expense Manager

![Android Native](https://img.shields.io/badge/Platform-Android_Native-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin_1.9-blue.svg)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_Material3-purple.svg)
![SQLCipher](https://img.shields.io/badge/Security-SQLCipher_AES--256-red.svg)
![Build Status](https://img.shields.io/badge/Build-SUCCESSFUL-success.svg)

An **offline-first, privacy-focused Android application** built with Kotlin, Jetpack Compose Material3, Hilt, and Room encrypted with **SQLCipher AES-256**. Automatically captures bank SMS and UPI push notifications, parses transactions with intelligent NLP rules, manages category budgets, displays financial analytics, and secures user data with Biometric authentication.

---

## ✨ Key Features

### ⚡ Smart Multi-Channel Auto-Tracking & NLP Engine
* **Dual-Channel Ingestion**: Captures financial transactions via `SmsReceiver` (SMS messages) and `ExpenseNotificationListenerService` (bank/UPI push notifications).
* **Broad UPI & Bank Support**: Pre-configured parsing adapters for HDFC, SBI, ICICI, BOBCARD, Kotak, Axis, GPay, PhonePe, Paytm, CRED, etc.
* **Exclusion Filters**: Ignores non-transaction messages (account balance inquiry alerts, e-mandate reminders, promotional notifications).
* **Precise Spend Extraction**: Isolates actual transaction amounts and ignores embedded Available Credit Limits or Current Outstanding numbers.
* **Monthwise & Incremental SMS Inbox Scanner**: Automatically scans inbox starting from the 1st day of the current month on initial run, and incrementally scans only new messages received thereafter.

### 💼 Budgets & Category Spending Caps
* Set monthly spending limits per category (e.g. ₹10,000 max for Food & Dining).
* Animated, color-coded progress bars (Green < 80%, Amber 80–99%, Red ≥ 100%).

### 📈 Financial Analytics & Category Leaderboard
* Real-time Income vs. Expense cashflow summary.
* Top Spending Category highlight badge.
* Category spending distribution breakdown with visual percentage bars.

### 🔒 Privacy & Biometric Security
* **100% Offline**: Zero external servers; 100% of user data remains on the user's device.
* **SQLCipher AES-256 Encryption**: Hardware-backed database encryption via Android Keystore.
* **Biometric & PIN Lock**: Prompt for Fingerprint, Face Unlock, or PIN on app open.

### 💾 Backup & Data Export
* **Encrypted JSON Backup & Restore**: Export/Import full backup snapshots via Android Storage Access Framework (SAF).
* **CSV Spreadsheet Exporter**: Export transaction history into formatted CSV spreadsheets for accounting and tax records.

---

## 🏗️ Architecture & Tech Stack

```
com.expensetracker.app/
├── core/
│   ├── data/repository/    # TransactionRepository, BudgetRepository
│   ├── database/           # ExpenseDatabase (SQLCipher), DAOs, Entities
│   ├── di/                 # Hilt Dependency Injection Modules
│   ├── ui/theme/           # Color, Type, Theme Design System Tokens
│   └── utils/              # PermissionUtils
├── ingestion/
│   ├── notification/       # ExpenseNotificationListenerService
│   ├── parser/             # RegexTransactionParser, ParsedTransaction
│   └── sms/                # SmsReceiver, SmsScanner
└── features/
    ├── main/               # MainScreen (Material3 Bottom NavigationBar)
    ├── dashboard/          # DashboardScreen, DashboardViewModel
    ├── transactions/       # TransactionsScreen, TransactionsViewModel
    ├── budgets/            # BudgetsScreen, BudgetsViewModel
    ├── analytics/          # AnalyticsScreen, AnalyticsViewModel
    ├── categories/         # CategoriesScreen, CategoriesViewModel
    ├── subscriptions/      # SubscriptionsScreen, SubscriptionsViewModel
    ├── settings/           # SettingsScreen, SettingsViewModel
    ├── security/           # BiometricLockManager
    ├── backup/             # BackupManager
    └── reports/            # ReportsExporter
```

---

## 🛠️ Building & Running Locally

### Prerequisites
* JDK 17+
* Android SDK 34+

### Build Commands

```bash
# Set JAVA_HOME (Windows PowerShell)
$env:JAVA_HOME="E:\ExpenseTrackerApp\jdk-tmp\jdk-17.0.10+7"

# Compile Debug APK
.\gradlew.bat assembleDebug

# Run All Unit Tests
.\gradlew.bat test

# Compile Release APK
.\gradlew.bat assembleRelease
```

---

## 📄 Documentation

All master specification documents are maintained in GitHub-flavored Markdown:
* [Expense_Tracker_SRS_v5.md](file:///e:/ExpenseTrackerApp/Expense_Tracker_SRS_v5.md) — Software Requirements Specification
* [Expense_Tracker_Implementation_Document_v4.md](file:///e:/ExpenseTrackerApp/Expense_Tracker_Implementation_Document_v4.md) — Technical Stack & Modular Architecture
* [Expense_Tracker_Idea_Document_v4.md](file:///e:/ExpenseTrackerApp/Expense_Tracker_Idea_Document_v4.md) — Vision Statement & Privacy Model
* [Expense_Tracker_Product_Roadmap.md](file:///e:/ExpenseTrackerApp/Expense_Tracker_Product_Roadmap.md) — Multi-Phase Development Roadmap
