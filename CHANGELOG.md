# 📜 Changelog — Expense Tracker App

All notable changes to the Expense Tracker App are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to Semantic Versioning.

---

## [Unreleased] - Branch `Dev`

### Added
- **3-Month Historical SMS Lookback on Install**: Configured initial inbox scan in `SmsScanner` to fetch from the 1st of 3 months prior (e.g. July 1st if installed in October) instead of only the current month.
- **Home Screen App Widget Live Data**: Connected `ExpenseWidgetProvider.kt` with encrypted Room database queries to dynamically render current net balance and today's total spend, with automatic updates triggered upon transaction ingestion.
- **Multi-AI Governance Framework**: Initialized `AGENTS.md`, `.cursorrules`, `CLAUDE.md`, `.github/copilot-instructions.md`, and modular `docs/`.
- **Android KeyStore SQLCipher Key Derivation**: Implemented `SecurityKeyManager.kt` sealing 256-bit AES master keys in hardware KeyStore.
- **Biometric App Lock Background Grace Period**: Added 30-second lifecycle background tracking in `MainActivity.kt` to re-lock automatically on return.
- **Multi-Entity Pure-Kotlin Backup Engine**: Upgraded `BackupManager.kt` to support all 5 entities with multi-line SMS handling and zero stub dependencies.
- **Merchant Category Memory in Live Ingestion**: Connected `merchantCategoryMappingDao` to `SmsReceiver`, `ExpenseNotificationListenerService`, and `SmsScanner`.

### Changed
- **Budgets & Dashboard FAB Collision Fix**: Shifted delete action icon to the left header row and added generous bottom spacers to prevent Floating Action Button clipping.
- **Category Badges & Avatars**: Upgraded to 44dp vibrant avatar circles and soft container amount pill badges across all transaction lists.
- **System Back Gesture Routing**: Unified Android system back gestures to return from sub-screens/tabs to the primary Dashboard.
- **Ingestion Subscription Class Disambiguation**: Renamed ingestion `SubscriptionDetector` to `RecurringMandateDetector`.
- **Filter Loop Performance Optimization**: Precomputed calendar period bounds in `TransactionsViewModel` to eliminate allocations in search/filter loops.

### Fixed
- **Timeless Duplicate Suppression & Repeat Purchase Dropping**: Replaced blind `LIMIT 1` duplicate query with windowed checks (2-hour dual-channel window and 4-day CC settlement window), ensuring repeat legitimate purchases are never suppressed.
- **WorkManager Crash in `BillReminderWorker`**: Removed unneeded `transactionDao` argument from constructor to eliminate reflection `NoSuchMethodException`.
- **Hardcoded Plaintext Passphrase**: Replaced static string passphrase across all database call sites with KeyStore-derived keys.
- **DailyReviewWorker Rescheduling**: Switched `ExistingPeriodicWorkPolicy` to `KEEP` on app launch so launching the app does not disrupt the evening review schedule.
- **CRED Club / CheQ Transfer Misclassification**: Corrected credit card bill settlement categorization to `TRANSFER` with `Bills & Utilities`.
- **Short Matcher Substring Collision**: Enforced word boundary protection (`\b`) on `cred`, `cheq`, and `upi` to avoid false triggers on "to credit".

---

## [1.2.0] - 2026-08-15

### Added
- **Daily Review Worker**: Periodic WorkManager worker triggering evening expense review notifications.
- **Bank / Account Chip Filter**: Dynamic bank origin filtering (HDFC, SBI, ICICI, Axis, Kotak, CRED) in transaction history.
- **Android 13+ Notification Permission Flow**: Dynamic `POST_NOTIFICATIONS` runtime permission prompts.

### Security
- **SQLCipher AES-256 Hardening**: Android Keystore backed 256-bit passphrase generation for Room database encryption.
- **Zero-Network Audit**: Confirmed zero internet access declarations in `AndroidManifest.xml`.

---

## [1.1.0] - 2026-07-20

### Added
- **SAF Backup & Restore**: Encrypted JSON import/export via Android Storage Access Framework.
- **CSV Ledger Export**: Formatted spreadsheet exporter for accounting and tax records.
- **Biometric Authentication**: Fingerprint/Face unlock gatekeeper with device PIN/Pattern fallback.

---

## [1.0.0] - 2026-06-01

### Added
- **Initial Core Ingestion Engine**: Dual-channel `SmsReceiver` and `ExpenseNotificationListenerService`.
- **Jetpack Compose Material3 UI**: Dashboard, Transactions Ledger, Budget Caps, and Analytics distribution.
- **Room Database Layer**: Core entities (`TransactionEntity`, `CategoryEntity`, `BudgetEntity`, `SubscriptionEntity`).
