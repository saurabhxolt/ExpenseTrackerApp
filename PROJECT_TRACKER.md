# 🚀 Project Tracker: Expense Tracker App (Android Native)

> **Cockpit Rule**: Keep this file **strictly under 150 lines**. Move resolved milestones and long logs to [CHANGELOG.md](file:///e:/ExpenseTrackerApp/CHANGELOG.md).

---

## 🎯 Active Sprint Status (Branch: `Dev`)

| Task / Objective | Subsystem | Status | Owner / Skill |
| :--- | :--- | :--- | :--- |
| **Windowed Dual-Channel Deduplication** | Ingestion / DB | ✅ Completed | Core Architecture |
| **Android KeyStore SQLCipher Key Derivation** | Core Security | ✅ Completed | Security Auditor |
| **Biometric Lifecycle Re-Lock (30s Grace)** | Security / UI | ✅ Completed | Android System |
| **WorkManager Crash Fix (BillReminderWorker)**| Background | ✅ Completed | Android System |
| **Multi-Entity Pure-Kotlin Backup Engine (v3)**| Backup / SAF | ✅ Completed | Storage Engine |
| **Category Preference Learning in Ingestion** | Ingestion / DB | ✅ Completed | `category_assignment_agent` |
| **Home Screen App Widget Live Data Alignment** | UI / Widget | ✅ Completed | Android System |
| **3-Month SMS Historical Scan on Install** | Ingestion / SMS | ✅ Completed | `sms_transaction_parser` |
| **Credit Card Bill Identification Engine** | Ingestion / NLP | ✅ Completed | `sms_transaction_parser` |
| **Monthly Budgets Calendar Scoping** | Feature / Budget | ✅ Completed | Core Architecture |
| **Smart DB-Aware Bill & Review Workers** | Background | ✅ Completed | Android System |
| **Database Indices & Migration v5** | Core / Room | ✅ Completed | Storage Engine |
| **Cross-Card Deduplication Collision Guard** | Ingestion / DB | ✅ Completed | `sms_transaction_parser` |
| **Safe Upsert Budgets & Dynamic Category Loading** | Feature / Budget | ✅ Completed | Core Architecture |
| **Backup v3 Full Entity Serialization & Safe Upsert** | Backup / SAF | ✅ Completed | Storage Engine |
| **Cold Start Biometric Debounce & Widget Live Sync** | UI / Widget | ✅ Completed | Android System |
| **Fast Splash Launch & Dashboard Dialog Wiring** | UI / UX | ✅ Completed | Core Architecture |
| **Peek Privacy Mode & Shake-to-Hide** | Security / UI | ✅ Completed | Android System |
| **Safe-to-Spend Today Radar** | Feature / Dashboard | ✅ Completed | Core Architecture |
| **Automated Local Rolling Backups (Offline)** | Storage / Worker | ✅ Completed | Storage Engine |
| **Stealth Inflation & Price Creep Detector** | Analysis / NLP | ✅ Completed | Core Architecture |
| **Room v6 Schema Migration & SavingsGoalEntity**| Core / Room | ✅ Completed | Storage Engine |
| **Visual Sinking Funds & Savings Jars** | Feature / Savings | ✅ Completed | Core Architecture |
| **Indian Tax Season Deductions Organizer** | Analysis / Tax | ✅ Completed | Core Architecture |
| **Itemized Restaurant / Group Bill Splitter** | Feature / Split | ✅ Completed | Core Architecture |
| **Room v7 Schema Migration & CreditCardEntity**| Core / Room | ✅ Completed | Storage Engine |
| **Credit Card 30% Utilization & Grace Radar** | Feature / Cards | ✅ Completed | Core Architecture |
| **Quick Settings Tile & Instant Cash Logging** | Feature / QuickLog| ✅ Completed | Android System |
| **Air-Gapped Offline QR Group Sync** | Feature / Split | ✅ Completed | Core Architecture |
| **100% On-Device Receipt & Invoice OCR Scanner** | Feature / OCR | ✅ Completed | Core Architecture |
| **OCR, Card Radar & Savings Jars Usability Overhaul** | OCR / Cards / Sinking | ✅ Completed | Core Architecture |

---

## 🏗️ Subsystem Health & Architecture Status

| Layer / Subsystem | Primary Tech Stack | Status | Documentation Reference |
| :--- | :--- | :--- | :--- |
| **UI & Presentation** | Jetpack Compose M3, Material You | 🟢 Healthy | [COMPOSE_UI_AND_THEME.md](file:///e:/ExpenseTrackerApp/docs/architecture/COMPOSE_UI_AND_THEME.md) |
| **State Management** | ViewModel + StateFlow (MVI/MVVM) | 🟢 Healthy | [PLATFORM_ARCHITECTURE.md](file:///e:/ExpenseTrackerApp/docs/architecture/PLATFORM_ARCHITECTURE.md) |
| **SMS Ingestion & NLP** | RegexTransactionParser, Word Boundary `\b` | 🟢 Healthy | [SMS_INGESTION_AND_NLP.md](file:///e:/ExpenseTrackerApp/docs/architecture/SMS_INGESTION_AND_NLP.md) |
| **Notification Listener** | `ExpenseNotificationListenerService` | 🟢 Healthy | [INGESTION_CONTRACTS.md](file:///e:/ExpenseTrackerApp/docs/development/INGESTION_CONTRACTS.md) |
| **Encrypted Database** | Room 2.6 + SQLCipher AES-256 | 🟢 Healthy | [DATABASE_AND_SQLCIPHER.md](file:///e:/ExpenseTrackerApp/docs/architecture/DATABASE_AND_SQLCIPHER.md) |
| **Security & Privacy** | BiometricPrompt, Keystore, Zero Net | 🟢 Healthy | [SECURITY_AND_BIOMETRICS.md](file:///e:/ExpenseTrackerApp/docs/architecture/SECURITY_AND_BIOMETRICS.md) |

---

## ⚡ 1-Click CLI Launchers & Build Commands

```powershell
# Set JAVA_HOME (JDK 17)
$env:JAVA_HOME="E:\ExpenseTrackerApp\jdk-tmp\jdk-17.0.10+7"

# 1. Compile Debug APK
.\gradlew.bat assembleDebug

# 2. Run All Unit Tests
.\gradlew.bat test

# 3. Install on Connected Emulator / USB Device
.\gradlew.bat installDebug

# 4. Stream Logs (Sanitized Filter)
adb logcat -s ExpenseTracker:V RegexParser:V SQLCipher:V
```

---

## 🛡️ Critical Operating Guardrails

1. **Strict No-Push**: Never run `git push` without user prompt approval. All work stays on `Dev`.
2. **Zero False Positives**: Promotional SMS, OTPs, and balance alerts must never become transactions.
3. **Word Boundaries (`\b`)**: Short matchers (`cred`, `cheq`, `upi`) must use word boundary checks.
4. **Offline Boundary**: `android.permission.INTERNET` is strictly prohibited in `AndroidManifest.xml`.
5. **Continuous Doc Sync**: Sync [FEATURES.md](file:///e:/ExpenseTrackerApp/FEATURES.md) and SRS in same commit.
