# Expense Tracker App - Implementation Blueprint
Version: 5.5 | Technical Stack & Architectural Guidelines

# 1. Recommended Technology Stack
- Language: Kotlin 1.9+ (Coroutines, Flow, StateFlow)
- UI Framework: Jetpack Compose (Material3 Design)
- Database: Room + SQLCipher for Android (AES-256 Encryption v4)
- Architecture: MVVM + Clean Architecture + Hilt DI
- Security: Android BiometricPrompt API + SQLCipher Hardware-backed encryption
- Automatic First-Launch Permission Prompting: Automatic OS runtime permission dialog trigger (`DashboardRoute`, `rememberLauncherForActivityResult`, `READ_SMS`, `RECEIVE_SMS`, `POST_NOTIFICATIONS`)
- Dedicated Subagent Architecture: Custom Agent Skills (`.agents/skills/sms_transaction_parser/SKILL.md`, `.agents/skills/category_assignment_agent/SKILL.md`) for automated parsing and category assignment validation
- Intelligent Subscriptions Engine: Auto SMS Matching & Subscription Radar (`SubscriptionMatcher.kt`, `SubscriptionDetector.kt`)
- Category Memory System: Dynamic User Preference Learning (`CategoryClassifier.kt`, `MerchantCategoryMappingEntity`, `MerchantCategoryMappingDao`)
- Category System: Unified In-Place Editable Category Management (`CategoriesViewModel.kt`, `CategoryEmojiProvider.kt`, `CategoryColorProvider.kt`)
- Split Ledger Engine: Offline P2P Group Expense & Debt Minimization Algorithm (`SplitRepository.kt`)
- Promotion System: Static `promotions.json` + `PromotionManager.kt` (No third-party ad SDKs)

# 2. Modular Package Structure
- features/main/ (MainScreen, Top Header Settings Button, Bottom NavigationBar)
- features/dashboard/ (DashboardScreen, DashboardViewModel, TransactionDetailsDialog, QuickStatsCards, MonthlyMandatesCard, SafeToSpendCard)
- features/transactions/ (TransactionsScreen, TransactionsViewModel, CategoryFilterChips)
- features/split/ (SplitScreen, SplitViewModel: Group List, Group Detail, Add Expense, Settle Up, WhatsApp Exporter)
- features/budgets/ (BudgetsScreen, BudgetsViewModel)
- features/analytics/ (AnalyticsScreen, AnalyticsViewModel, CanvasDonutChart, CanvasBarChart, TransactionTypeFilterChips)
- features/categories/ (CategoriesScreen, CategoriesViewModel, CategoryEmojiProvider, CategoryColorProvider)
- features/subscriptions/ (SubscriptionsScreen, SubscriptionsViewModel, AddEditSubscriptionDialog, SubscriptionRadarBanner, AutoPaidBadges)
- features/settings/ (SettingsScreen, SettingsViewModel, EcosystemPromotionCard)
- features/security/ (BiometricLockManager)
- core/security/ (PrivacyModeManager, ShakeDetector)
- features/backup/ (BackupManager)
- features/reports/ (ReportsExporter)
- core/database/ (ExpenseDatabase v4, GroupEntity, GroupMemberEntity, SplitExpenseEntity, ExpenseSplitShareEntity, GroupSettlementEntity, SubscriptionEntity, CategoryEntity, MerchantCategoryMappingEntity, SplitDao, SubscriptionDao, CategoryDao, MerchantCategoryMappingDao)
- core/data/repository/ (SplitRepository, SubscriptionRepository, CategoryRepository, SubscriptionDetector)
- core/promotions/ (PromotionModel, PromotionManager)
- ingestion/parser/ (RegexTransactionParser, CategoryClassifier, SubscriptionMatcher: Auto SMS Mandate Matching & Cycle Advancement)
- ingestion/deduplication/ (TransactionDeduplicator: Timeless & Windowed Deduplication & DB False Positive Purging)
- ingestion/worker/ (BillReminderWorker)
- widget/ (ExpenseWidgetProvider)

# 3. Parser Intelligence & Analytics Architecture
- **RegexTransactionParser & Dedicated Parser Skill**: 6-stage semantic pipeline filtering promotional loan offers, pre-approved card limits, festive offers ("YES BANK Festive Offer"), EMI conversion marketing ("convert your transaction of X into EMI", "1 step away", "no paperwork needed"), news/market digests ("$X billion inflow", "cr order"), and bill payment due reminders ("due in X days", "pay instantly on Paytm"), enforcing explicit execution action verbs and strict regex word boundaries (`\b`) to avoid `"to credit"` collisions with `cred`. Classifies Credit Card swipes, online purchases, and RuPay UPI spends as `DEBIT` (Expenses), while classifying Credit Card Bill Payments (from Bank/CRED/CheQ/BillDesk/BBPS) and issuer payment receipts (HDFC, SBI Card, ICICI, Axis, Kotak, BOBCARD, OneCard, Amex) as `TRANSFER` (`Credit Card Bill` / `Bills & Utilities`), preventing expense double-counting and income inflation. Universal VPA handle regex (`(?i)\b[a-z0-9._-]*?(ccpay|cardpay|cardbill|billdesk|bbps|cred|cheq)[a-z0-9._-]*?@[a-z]+\b`) dynamically handles third-party payment aggregators. Dashboard presents transfers with `↔ ₹` in neutral secondary styling, accompanied by self-healing deduplication migration.
- **Category Memory Classifier (`CategoryClassifier.kt`)**: Automatically remembers user-assigned category preferences per merchant (`merchant` -> `category`) in `MerchantCategoryMappingEntity`. On future repeated transactions, it overrides general default categories to automatically assign the exact last category chosen by the user.
- **Analytics Donut Chart Percentage Engine (`AnalyticsViewModel.kt`)**: Donut chart percentages are calculated strictly relative to the selected Transaction Type (`DEBIT` Expenses, `CREDIT` Income, `INVESTMENT` Investments, `TRANSFER` Transfers, or `ALL`). Interactive filter chips allow dynamic switching of breakdown views.
- **Unified Category Management & Emoji Engine (`CategoryEmojiProvider.kt`)**: Renders system and custom categories in a single editable screen with true SQL `UPDATE` in-place editing and historical transaction category name synchronization. `CategoryColorProvider` auto-assigns guaranteed distinct, non-conflicting colors to prevent duplicate color collisions.
- **Multi-Channel Ingestion & Windowed Deduplication**: Integrates `NotificationListenerService`, `SmsReceiver`, and `SmsScanner` (with 3-month lookback on initial install clamping to day 1 *before* subtracting months) for multi-channel ingestion. `TransactionDeduplicator` enforces a 2-hour matching window (`DEFAULT_DEDUPLICATION_WINDOW_MS`) to prevent duplicate notification/SMS entries, and a 4-day settlement window (`CC_PAYMENT_DEDUPLICATION_WINDOW_MS`) for credit card confirmations without suppressing repeat purchases on different dates.
- **Hardware-Backed KeyStore SQLCipher Encryption (`SecurityKeyManager.kt`)**: Derives 256-bit AES master key in `AndroidKeyStore`, replacing static passphrases across Room database builders and ingestion receivers.
- **Dynamic Category Budgets & Safe Upsert (`BudgetRepository.kt`, `BudgetsViewModel.kt`)**: Pulls real categories dynamically from `CategoryRepository` for budget creation and enforces safe upserting via `BudgetDao.getBudgetByCategory` to prevent duplicate primary key collisions.
- **Disaster-Recovery Full-Field Backup Engine (`BackupManager.kt`, `SettingsViewModel.kt`)**: Serializes and parses all entity fields including custom category flags, emojis, budget periods, and subscription mandate matches, using safe name-based upserts on restore to maintain relational integrity without crashing on existing entries.
- **Live Home Screen Widget Synchronization Hook (`TransactionRepository.kt`, `ExpenseWidgetProvider.kt`)**: Injects application context into `TransactionRepository` to trigger immediate widget recalculation on every transaction write, update, or deletion, bound to root launcher pending intents.
- **Offline Peer-to-Peer Group Ledger**: Room v2 entities storing split groups, members, shared expenses, and settlements. Computes real-time net member balances and minimal settlement debt pairs.
- **Header Settings & Bottom Navigation**: Settings moved to a prominent top app bar IconButton (`Icons.Default.Settings`). Bottom Navigation Bar updated to 5 main tabs: Dashboard, History, Split Bills, Budgets, Analytics.
- **Unified Categories**: Standardized 18 system category definitions across ViewModels and UI modals with dynamic color assignment via `CategoryColorProvider`.
- **Analytics Date Filter Pipeline**: StateFlow-driven reactive period filtering ("ALL", "THIS_MONTH", "LAST_MONTH", "LAST_3_MONTHS", "LAST_6_MONTHS", "YEARLY", "CUSTOM") connected directly to Canvas donut and bar chart calculations.
- **Peek Privacy Mode & Shake-to-Hide Subsystem (`PrivacyModeManager.kt`, `ShakeDetector.kt`)**: Leverages device accelerometer (`Sensor.TYPE_ACCELEROMETER`, 2.6g threshold, 1200ms debounce, haptic feedback) and top bar eye icon. Injects `LocalPrivacyMode` `CompositionLocal` across the Composable hierarchy to mask monetary figures (`₹•••••`) across Dashboard, History, and Budgets, with user preferences configured via DataStore.
- **Safe-to-Spend Today Radar (`DashboardViewModel.kt`, `DashboardScreen.kt`)**: Calculates real-time daily discretionary allowance pacing remaining monthly budget limits against remaining days in the active month while reserving funds for upcoming committed bills/mandates, surfacing status chips (*Abundant*, *Balanced*, *Tight*, *Budget Exhausted*).

# 4. Multi-AI Governance & Modular Documentation Architecture
- **Master Adapted AI System Prompt**: `PromptForCleanAndroidAppDevelopmentFromAIAgent.md` custom-tailored for offline-first native Android architecture.
- **Lean Active Cockpit (< 150 lines)**: `PROJECT_TRACKER.md` tracking active sprint status, architecture subsystem health, and 1-click launcher commands.
- **Master Feature Catalog**: `FEATURES.md` documenting 100% of capabilities across End-User, Autonomous Ingestion Daemon, Privacy Guardian, and Storage Maintenance personas.
- **Historical Changelog Archive**: `CHANGELOG.md` adhering to Keep a Changelog standards.
- **Modular Architecture Cheatsheets (`docs/architecture/`, < 80 lines each)**:
  - `PLATFORM_ARCHITECTURE.md`: High-level system tiers, data flows, and ERD.
  - `SMS_INGESTION_AND_NLP.md`: Regex rules, word boundaries (`\b`), noise rejection, and merchant memory.
  - `DATABASE_AND_SQLCIPHER.md`: Entities, SQLCipher AES-256 Android Keystore integration, and Room migrations.
  - `COMPOSE_UI_AND_THEME.md`: Material3 tokens, MVI/MVVM StateFlows, and FAB clearance rules.
  - `SECURITY_AND_BIOMETRICS.md`: Zero network policy, Keystore encryption, and BiometricPrompt.
- **Team Onboarding & Testing Playbooks (`docs/development/`)**:
  - `DEVELOPER_ONBOARDING.md`: JDK 17, Android SDK 34, `local.properties`, and Gradle commands.
  - `QA_TESTING_PLAYBOOK.md`: Automated unit tests, SMS test vectors, ADB emulation, and zero false positive matrix.
  - `INGESTION_CONTRACTS.md`: Data models (`ParsedTransaction`, SMS PDUs, Notification bundles).
  - `DATABASE_WORKFLOW.md`: Room schema export, automated migration testing, and backup formatting.
  - `SECURITY_AND_PRIVACY_AUDIT.md`: Zero-network audit, PII redaction, and SAF sandboxing.
- **Multi-AI Governance Pointer Files**: `AGENTS.md` (root), `.cursorrules`, `CLAUDE.md`, `.github/copilot-instructions.md`, and `.github/PULL_REQUEST_TEMPLATE.md`.