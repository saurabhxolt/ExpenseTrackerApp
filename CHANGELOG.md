# 📜 Changelog — Expense Tracker App

All notable changes to the Expense Tracker App are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to Semantic Versioning.

---

## [Unreleased] - Branch `Dev`

### Added
- **Air-Gapped Offline QR Group Sync (Zero-Internet Splitwise)**: Implemented `GroupQrSyncEngine`, `GroupQrShareDialog`, and `GroupQrScannerDialog`. Features ZXing-based high-contrast QR generation, GZIP Base64 dense payload compression, live CameraX viewfinder scanner with barcode analysis, gallery screenshot importer, clipboard paste fallback, smart identity perspective remapping ("You" <-> Friend), and idempotent Room database merging.
- **100% On-Device Receipt & Invoice OCR Scanner**: Implemented `ReceiptOcrParser` and `ReceiptScannerDialog` utilizing Google ML Kit Text Recognition running entirely on-device without network permissions. Accurately extracts total amounts, merchant names, receipt dates, and inferred categories from restaurant bills, pharmacy receipts, and supermarket invoices, with an interactive draft review sheet and 1-tap persistence into encrypted Room transactions.
- **Room v7 Schema Migration & CreditCardEntity**: Upgraded Room database schema to version 7 with `MIGRATION_6_7`, adding the `credit_cards` table for persistent credit card account tracking.
- **Credit Card Grace Period & 30% Utilization Radar**: Implemented `CreditCardRadarCalculator`, `CreditCardRepository`, `CreditCardViewModel`, and `CreditCardRadarCard`. Features real-time statement cycle tracking, interest-free grace days countdown ticker, 30% CIBIL utilization limit guard with headroom calculation, multi-bank gradient visual card carousel, auto-discovery of unregistered cards from SMS history, and full backup/restore integration.
- **Android Quick Settings Tile & Instant Cash Logging (<3s)**: Implemented `CashQuickLogTileService` for the notification shade and `QuickCashLogDialog` with 1-tap fast presets (`☕ Chai ₹20`, `🛺 Auto ₹50`, `🥪 Snacks ₹100`, `🍛 Lunch ₹150`, `🛒 Groceries ₹500`, `⛽ Fuel ₹500`) directly integrated into `MainActivity`, `MainScreen`, and `DashboardScreen`.
- **Room v6 Schema Migration & SavingsGoalEntity**: Upgraded Room database schema to version 6 with `MIGRATION_5_6`, adding the `savings_goals` table and nullable `taxDeductibleSection` column to `transactions`.
- **Visual Sinking Funds & Savings Goals (Interactive "Jars")**: Implemented `SavingsGoalEntity`, `SavingsGoalDao`, `SavingsGoalRepository`, `SavingsGoalsViewModel`, and `SavingsGoalsScreen`. Integrated interactive sinking fund jars in `BudgetsScreen` with target amounts, progress gauges, monthly savings pace calculators, and 1-tap deposit/withdraw dialogs.
- **Indian Tax Season Deductions Organizer (80C / 80D / HRA)**: Implemented `TaxDeductionAnalyzer` and `TaxSeasonDeductionsCard` in Analytics. Automatically infers tax-exempt debits under Sections 80C, 80D, Section 10(13A) HRA, 80CCD, 80E, and 80G, tracks statutory limits, estimates tax savings at 20% slab, and provides 1-tap CA/ITR export.
- **Itemized Restaurant & Group Bill Splitter**: Implemented `ItemizedSplitCalculator` and `ItemizedBillSplitterDialog` in `SplitScreen`. Proportionally divides dining bills, GST/taxes, and tips according to individual dish consumption, with 1-click import into group ledgers and formatted WhatsApp summaries.
- **Automated Local Rolling Backups (`ScheduledBackupWorker`)**: Added periodic weekly WorkManager worker snapshotting the complete encrypted Room database to the local sandbox with 5-version rolling retention, on-demand execution, and an in-app restore dialog in Settings.
- **Stealth Inflation & Subscription Price Creep Detector (`PriceCreepDetector`)**: Real-time analysis engine identifying price hikes (>3% and >= ₹5) across repeat merchants and subscriptions, rendering alert badges in Subscriptions and Transaction Details.
- **Peek Privacy Mode & Shake-to-Hide**: Introduced `PrivacyModeManager` and `ShakeDetector` with hardware accelerometer listener (2.6g threshold, 1200ms debounce, haptic feedback) and top bar eye toggle. Exposes `LocalPrivacyMode` CompositionLocal to mask all sensitive balances, budgets, and transaction amounts with `₹•••••` across Dashboard, History, and Budgets, with configurable user preferences in Settings.
- **Safe-to-Spend Today Radar**: Added daily discretionary spending calculator and UI card in `DashboardViewModel` and `DashboardScreen`. Dynamically paces remaining monthly budget against calendar days remaining in the month while factoring in reserved committed subscriptions and bills, complete with visual status chips (*Abundant*, *Balanced*, *Tight*, *Budget Exhausted*).
- **3-Month Historical SMS Lookback on Install**: Configured initial inbox scan in `SmsScanner` to fetch from the 1st of 3 months prior (e.g. July 1st if installed in October) instead of only the current month.
- **Auto-Detection of Subscriptions on Historical Scan**: Configured `SmsScanner` to run `RecurringMandateDetector.autoDetectAndSave` during inbox scan.
- **Full Splitwise P2P Subsystem Backup & Disaster Recovery (v3)**: Added support in `BackupManager` and `SettingsViewModel` for groups, group members, split expenses, split shares, and group settlements with complete roundtrip fidelity.
- **High-Volume Database Indexing (v5 Migration)**: Added database indexes on `timestamp`, `(type, timestamp)`, and `category` in `TransactionEntity` with non-destructive `MIGRATION_4_5`.
- **Home Screen App Widget Live Data**: Connected `ExpenseWidgetProvider.kt` with encrypted Room database queries to dynamically render current month's net cashflow and today's total spend, with automatic updates triggered upon transaction ingestion.
- **Multi-AI Governance Framework**: Initialized `AGENTS.md`, `.cursorrules`, `CLAUDE.md`, `.github/copilot-instructions.md`, and modular `docs/`.
- **Android KeyStore SQLCipher Key Derivation**: Implemented `SecurityKeyManager.kt` sealing 256-bit AES master keys in hardware KeyStore.
- **Biometric App Lock Background Grace Period**: Added 30-second lifecycle background tracking in `MainActivity.kt` to re-lock automatically on return.
- **Merchant Category Memory in Live Ingestion**: Connected `merchantCategoryMappingDao` to `SmsReceiver`, `ExpenseNotificationListenerService`, and `SmsScanner`.
- **Dynamic Category Loading in Budgets**: Connected `BudgetsViewModel` and `BudgetsScreen` to `CategoryRepository` to dynamically populate custom and system categories in the Add/Edit Budget dialog.
- **Homescreen Widget Full-Spectrum Sync Hook**: Integrated `ExpenseWidgetProvider.updateAllWidgets(context)` into `TransactionRepository` across all manual and ingestion writes, updates, and deletes.
- **Homescreen Widget Root Click-Through**: Bound `R.id.widget_root` with a `PendingIntent` to launch `MainActivity` directly upon tapping the widget surface.

### Changed
- **Receipt OCR Accuracy & Offline Model Overhaul**: Replaced unbundled `play-services-mlkit-text-recognition` with bundled offline `com.google.mlkit:text-recognition:16.0.0` (packaging `libmlkit_google_ocr_pipeline.so` directly inside APK), enabling 100% offline OCR without Google Play Services or network downloads. Corrected 90° sideways camera sensor orientation via `imageProxy.imageInfo.rotationDegrees` matrix rotation and gallery EXIF orientation handling. Added multi-line proximity total matching, item count noise rejection ("Total items: 3"), detected amount chips for 1-tap selection, linked credit card payment method selector, and expandable raw extracted text drawer.
- **Credit Card Radar Transaction Tracking & Education Overhaul**: Enhanced `CreditCardRadarCalculator.kt` to match transactions using cleaned account digits, note tags, SMS raw text masked patterns (`**4321`, `xx4321`), merchants, and bank identifiers. Added expandable "Cycle Transactions (N)" drawer to `CreditCardRadarCard.kt` with transaction breakdown and empty state guides. Overhauled `CreditCardEditDialog` with 11 bank issuer chips (`HDFC`, `SBI`, `ICICI`, `Axis`, `Kotak`, `OneCard`, `BOB`, `Amex`, `RBL`, `IndusInd`, `Federal`), statement date vs payment due date helper guidance, and offline privacy card. Added linked credit card selector chips to Dashboard `AddTransactionDialog`.
- **Visual Sinking Funds Dashboard Widget, Starter Templates & Ledger Sync**: Added "Savings Jars (Sinking Funds) Overview" snapshot card to `DashboardScreen.kt` showing total saved progress and quick-tap jar chips. Added 4 one-tap starter goal templates (`🛡️ Emergency Fund`, `🏖️ Vacation Trip`, `💻 Gadget & Tech`, `🚗 Vehicle Down Payment`) in `SavingsGoalsScreen.kt`. Implemented dual ledger sync with `recordInLedger` toggle to automatically record deposits as `INVESTMENT` transactions and withdrawals as `CREDIT` transactions in the main ledger.
- **Snappy Splash Screen Response**: Reduced hardcoded splash display delay from 5000ms to 1200ms in `SplashScreen.kt` for instant app responsiveness.
- **Room Database Migration Resilience**: Configured `fallbackToDestructiveMigration()` in `ExpenseDatabase.kt` builder to safeguard against crashes when migrating from legacy pre-v4 schemas.
- **Monthly Budgets Calendar Scoping**: Bound `BudgetsViewModel` category spend totals to the current active calendar month, preventing historical multi-month transaction scans from skewing monthly budget gauges.
- **Smart DB-Aware Background Workers**: Upgraded `BillReminderWorker` to evaluate upcoming active subscriptions within 48h and `DailyReviewWorker` to tailor evening reminders with actual recorded spend amounts.
- **Reports & Widget Alignment**: Enhanced `ReportsExporter` to render distinct symbols (`↔` and `📈`) and summary totals for transfers and investments, aligning with Dashboard cashflow metrics.
- **Budgets & Dashboard FAB Collision Fix**: Shifted delete action icon to the left header row and added generous bottom spacers to prevent Floating Action Button clipping.
- **Category Badges & Avatars**: Upgraded to 44dp vibrant avatar circles and soft container amount pill badges across all transaction lists.
- **System Back Gesture Routing**: Unified Android system back gestures to return from sub-screens/tabs to the primary Dashboard.
- **Ingestion Subscription Class Disambiguation**: Renamed ingestion `SubscriptionDetector` to `RecurringMandateDetector`.
- **Filter Loop Performance Optimization**: Precomputed calendar period bounds in `TransactionsViewModel` to eliminate allocations in search/filter loops.

### Fixed
- **Budget Category Duplication**: Implemented safe upsert logic in `BudgetRepository` via `BudgetDao.getBudgetByCategory` to update existing budget amounts instead of inserting duplicate rows for the same category.
- **Backup v3 Serialization Field Truncation**: Extended `BackupManager.kt` to serialize and parse `isCustom` and `emoji` for Categories, `period` for Budgets, and `autoMatched` and `lastPaidDateMs` for Subscriptions, ensuring complete roundtrip fidelity.
- **Backup Restore Constraint Violations & False Success Toast**: Overhauled `SettingsViewModel.restoreBackupJson` with safe entity upsert by name, robust `try/catch` error handling with informative error toasts, and automatic widget data synchronization upon restore.
- **Cold-Start Biometric Prompt Race Condition**: Added `isAuthenticating` atomic lifecycle guard in `MainActivity.kt` and consolidated prompt launching in `onResume()`, eliminating concurrent `BiometricPrompt.authenticate()` invocations on cold launch.
- **Dashboard Transaction Details Dialog Incomplete Actions**: Connected `onDelete` (with confirmation dialog) and `onOpenSmsApp` (with system SMS app launcher intent) in `DashboardScreen.kt`.
- **Subscription Radar Blank Brand Guard**: Added `targetNorm.isNotBlank()` safety guard in `SubscriptionDetector.kt` to prevent false matches against empty recipient fields.
- **Timeless Duplicate Suppression & Repeat Purchase Dropping**: Replaced blind `LIMIT 1` duplicate query with windowed checks (2-hour dual-channel window and 4-day CC settlement window), ensuring repeat legitimate purchases are never suppressed.
- **WorkManager Crash in `BillReminderWorker`**: Removed unneeded `transactionDao` argument from constructor to eliminate reflection `NoSuchMethodException`.
- **Hardcoded Plaintext Passphrase**: Replaced static string passphrase across all database call sites with KeyStore-derived keys.
- **DailyReviewWorker Rescheduling**: Switched `ExistingPeriodicWorkPolicy` to `KEEP` on app launch so launching the app does not disrupt the evening review schedule.
- **CRED Club / CheQ Transfer Misclassification**: Corrected credit card bill settlement categorization to `TRANSFER` with `Bills & Utilities`.
- **Credit Card Bill Payment False Expense Classification**: Hardened `RegexTransactionParser.isCreditCardBillPayment` and `DEBIT_PATTERNS` to catch issuer payment confirmations (HDFC, SBI, ICICI, Axis, Kotak, BOBCARD, OneCard, Amex) and outbound bank settlements, classifying them as `TRANSFER` rather than `DEBIT` expenses. Added receipt exclusion guards and self-healing historical deduplication migration in `TransactionDeduplicator`.
- **Dashboard Transfer UI Display**: Updated `DashboardScreen.kt` to render `TRANSFER` items with neutral prefix (`↔ ₹`), secondary accent coloring, a `"Transfer / CC Bill"` type badge in details dialog, and manual transfer editing support.
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
