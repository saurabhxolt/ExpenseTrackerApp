# Walkthrough - Transaction Intent Classifier Engine & Dashboard Overhaul

## 1. Key Accomplishments

### 🧠 1. Smart Intent-Based Transaction Parser Engine
- **Prioritized Credit/Income Classification**: Updated [RegexTransactionParser.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/ingestion/parser/RegexTransactionParser.kt) so that incoming credits (`credited`, `received from`, `transferred to your A/c`, `added to account`, `cashback received`, `refund processed`) are evaluated BEFORE broad generic debit patterns, preventing incoming money from being misclassified as expense.
- **Enhanced Rejection Filters**: Added strict rejection rules for failed transactions (`failed`, `unsuccessful`), credit card statement generation alerts (`statement generated`, `statement is sent`), bill/loan due reminders (`has due of`, `is due on`, `due date`), pre-approved loan & credit card offer notices (`disbursal ready`, `funds available`, `personal loan offer`), and security/app notifications (`fingerprint login`, `biometric authentication`, `card usage settings`).
- **Comprehensive Merchant Extraction**: Extracted clean merchant and recipient names from UPI transfers, wallet payments, service receipts (ACT Fibernet, Airtel, Zepto Cash, CheQ, CRED), and investment platforms.

### 📊 2. Dashboard Period Filter Controls & Logical Metrics
- **Interactive Period Filters**: Added `PeriodFilter` chips (*This Month*, *Last Month*, *This Quarter*, *This Year*, *All Time*) in [DashboardScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt) and [DashboardViewModel.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardViewModel.kt).
- **Logical Financial Metrics**:
  - **Net Cashflow Hero Card**: Displays `Income - Expenses` for the selected time window, color-coded green for positive and red for negative.
  - **Financial Breakdown Row**: Displays Income (💰), Expenses (💸), and Investments (📈).
  - **Savings Rate Badge**: Displays Savings Rate % (`(Income - Expenses) / Income * 100%`).
  - **Avg Daily Spend**: Calculates exact average daily expenditure for the selected period.
- **Period-Aware Transaction Feed**: Replaced artificial 5-day transaction truncation with full period transaction feeds matching the active period filter.

### 🔄 3. Recurring Mandates & Subscription Anti-Duplication Engine
- **Brand-Normalized Smart Matching**: Updated [SubscriptionDetector.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/ingestion/parser/SubscriptionDetector.kt) with brand normalization (`normalizeBrandName`), stripping generic suffixes (`Au`, `Pvt Ltd`, `Mandate`, `Autopay`, `UPI`, `App`) to prevent duplicate subscriptions when SMS variations arrive for the same provider (e.g. `ICCL Mutual Funds Au` vs `ICCL Mutual Fund SIP`).
- **Late Message Deduplication Window**: If a recurring transaction SMS or notification arrives late for the same cycle, the engine updates the existing active subscription without creating a second entry or advancing the due date prematurely.
- **Auto-Cycle Due Date Advancement & Manual Action**: Active subscriptions auto-compute upcoming monthly/yearly cycle dates in [SubscriptionsViewModel.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/subscriptions/SubscriptionsViewModel.kt). Added an interactive **"Mark Paid"** action button in [SubscriptionsScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/subscriptions/SubscriptionsScreen.kt) to allow manual 1-tap cycle advancement.

### 🐞 4. Final Resolved Screenshot Bugs (`E:\ExpenseTrackerApp\bugs`)
1. **Budgets Screen FAB Trash Icon Interference (`IMG-20260728-WA0001.jpg`)**:
   - Relocated the trash delete icon in [BudgetsScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/budgets/BudgetsScreen.kt) to the left header row next to the category title.
   - Increased bottom `contentPadding` to `160.dp` and added a `100.dp` `Spacer` at the bottom of the list. The entire right side of the card now contains only the percentage text (`0%`), making it 100% impossible for the Floating Action Button (`+`) to block or interfere with deleting budget items.
2. **CRED Club & Credit Card Bill Outflows Categorized as Transfer (`Screenshot_20260811_224528_VaultExpense.jpg`)**:
   - Confirmed Step 3.1 in [RegexTransactionParser.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/ingestion/parser/RegexTransactionParser.kt) assigns `type = "TRANSFER"` with category `Bills & Utilities` to credit card bill payments and outflows to **CRED Club / CheQ** (`Sent Rs.2393.71 ... To CRED Club`), since paying a credit card bill moves money from your bank account to settle card liability rather than generating a direct new expense.

### 🎨 5. Major Category Avatar & Pill Badge UI Redesign
- **Vibrant Category Avatar Circles**: Applied 44.dp category avatar circles (🍕 Food, 🚗 Transport, 🛒 Shopping, 💡 Bills & Credit Card, 📈 Investments, 🏥 Health, 💰 Income, 💸 Transfers) across [DashboardScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt), [TransactionsScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/transactions/TransactionsScreen.kt), [BudgetsScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/budgets/BudgetsScreen.kt), and [AnalyticsScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/analytics/AnalyticsScreen.kt).
- **Vibrant Amount Pill Badges**: Replaced plain mono text with styled container pill badges (`-₹250.00` in soft crimson, `+₹173.00` in emerald green, `₹5000.00` in cyber teal).
- **Glassmorphic Hero Card & Mesh Gradients**: Built glassmorphic card overlays with subtle 1.dp border outlines and horizontal mesh gradient fills (`Color(0xFF0F2942)` for positive cashflow, `Color(0xFF3B1E2B)` for negative in Dark Mode).
- **100% Theme Contrast Integrity**: Tested and verified color contrast across both Light and Dark themes.

### 📱 6. Comprehensive Android System Back Gesture & Navigation Support
- **Settings Screen Back Gesture**: Added `BackHandler` in [MainScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/main/MainScreen.kt) so swiping back gesture on Android when Settings is open smoothly closes Settings and returns to the main app interface.
- **Tab Navigation Back Gesture**: When on any sub-tab (History, Split Bills, Budgets, Analytics), performing the system back gesture seamlessly navigates back to the main **Dashboard** tab (`selectedTab = 0`).

### 🏦 7. Bank-Wise Filter Option & Dashboard FAB Layout Fix
- **Bank-Wise Filter Bar on History Screen**: Added a dynamic **"🏦 Bank / Account"** horizontal chip selector in [TransactionsScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/transactions/TransactionsScreen.kt) and [TransactionsViewModel.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/transactions/TransactionsViewModel.kt). Automatically categorizes transactions by bank/card origin (`HDFC Bank`, `Axis Bank`, `ICICI Bank`, `SBI`, `Kotak Bank`, `BOB Card/Bank`, `Paytm`, `CRED`, etc.).
- **Dashboard FAB Delete Icon Elimination**: Relocated `onDelete` trash icon in [DashboardScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt) to the left header row next to the merchant name, added `PaddingValues(bottom = 140.dp)` to `LazyColumn`, and inserted a `100.dp` bottom spacer item. The rightmost edge near the FAB contains only the amount pill badge, making FAB interference 100% impossible on the Dashboard.

### ⏰ 8. Configurable Daily End-of-Day Expense Review Notification
- **Daily Review Worker**: Built [DailyReviewWorker.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/ingestion/worker/DailyReviewWorker.kt) with WorkManager `PeriodicWorkRequestBuilder` to trigger a daily evening reminder notification to review today's expenses.
- **Native System TimePicker Dialog**: Added full custom time selection via Android `TimePickerDialog` in [SettingsScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/settings/SettingsScreen.kt) and [SettingsViewModel.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/settings/SettingsViewModel.kt). Users can pick **ANY custom hour and minute** with standard AM/PM time selection (e.g. 7:15 PM, 8:45 PM, 9:15 PM, 10:20 PM) or tap quick 1-touch preset chips.
- **Android 13+ Runtime Notification Permission**: Added automatic `POST_NOTIFICATIONS` runtime permission prompts in [SettingsScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/settings/SettingsScreen.kt) and [DashboardScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt) on Android 13/14 (API 33+), ensuring notifications are never muted by Android OS.
- **Automatic Scheduling**: Auto-enqueued in [MainActivity.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/MainActivity.kt) on app launch and rescheduled immediately upon preference changes.

### 🎨 9. Unified Collapsible Filters & Dynamic Executive Dashboard Overhaul
- **Reusable `UnifiedFilterBar` Composable**: Standardized filters across [DashboardScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt), [TransactionsScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/transactions/TransactionsScreen.kt), and [AnalyticsScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/analytics/AnalyticsScreen.kt) using compact, collapsible dropdown cards (`📅 Period`, `🏦 Bank`, `🏷️ Category`, `💳 Type`). Replaced long scrolling chip rows for a clean, consistent experience.
- **Dynamic Executive Financial Snapshot**: Replaced the static single Net Cashflow card with a **Dynamic Dual-Hero Snapshot** displaying:
  - **Net Cashflow & Wealth Flow**: Color-coded green for positive and red for negative.
  - **Financial Health Status Pill**: Dynamic status indicator (`Optimal Savings 🟢` for $\ge 30\%$, `Balanced Flow 🟡` for $0-30\%$, `High Outflow 🔴` for negative).
  - **Daily Spend & Savings Meter**: Displays `Avg Daily ₹.../day`, Savings Rate %, and a visual `LinearProgressIndicator` progress bar.
- **Dashboard Action Icon Alignment**: Relocated the `onDelete` trash icon in [DashboardScreen.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt) directly beside the `onEdit` pencil icon on the right side next to the amount pill badge, matching the layout on the History screen.

### 🤖 10. Dedicated SMS Parser Agent Skill & False Positive Resolution (`E:\ExpenseTrackerApp\bugs`)
- **Resolved False Positive Screenshots**:
  1. **Yes Bank Festive Offer (`Screenshot_20260812_215312_VaultExpense.jpg`)**: Fixed issue where `"YES BANK LTD Limited Time Festive Offer... to credit the festive loan of ₹3,22,000.00..."` was misclassified as a `Bills & Utilities` CRED transaction.
     - Root cause: `lower.contains(" to cred")` matched `"to credit"`. Replaced with strict word boundary patterns (`\bto cred\b`, `cred.club`, `cred app`, `cred club`). Added rejection patterns for `festive offer`, `festive loan`, `digital authorization`, and `link below`.
  2. **AU Credit Card EMI Conversion Offer (`Screenshot_20260812_215349_VaultExpense.jpg`)**: Fixed issue where `"Easy Payments with EMI is 1 step away! Convert your transaction of ₹7000 into EMI..."` was parsed as a `DEBIT` of ₹7000.
     - Root cause: Missing marketing keywords in `PROMOTIONAL_OFFER_PATTERNS`. Added rejection patterns for `convert.*into emi`, `convert your transaction`, `1 step away`, `easy payments with emi`, and `no paperwork needed`.
- **Dedicated Agent Infrastructure (`.agents/skills/sms_transaction_parser/SKILL.md`)**:
  - Defined a dedicated SMS Transaction Parser Agent skill in [.agents/skills/sms_transaction_parser/SKILL.md](file:///E:/ExpenseTrackerApp/.agents/skills/sms_transaction_parser/SKILL.md) to enable continuous automated testing, edge-case generation, and regex safety auditing across financial message types.
  - Updated [.agents/AGENTS.md](file:///E:/ExpenseTrackerApp/.agents/AGENTS.md) with parser quality rules and zero false-positive requirements.

### 🎨 11. Transaction Type Donut Breakdown & Unified Editable Category Management
- **Transaction Type Donut Breakdown (`AnalyticsScreen.kt`, `AnalyticsViewModel.kt`)**:
  - Donut chart percentages now calculate strictly relative to the active Transaction Type (`Expenses 💸`, `Income 💰`, `Investments 📈`, `Transfers 🔄`, or `All 🌐`).
  - Added an interactive **Transaction Type Filter Bar** directly above the Donut chart for easy switching between breakdown views.
- **Unified Editable Category Management (`CategoriesScreen.kt`, `CategoriesViewModel.kt`)**:
  - Merged system default categories and custom user categories into a single unified editable category list.
  - Added `AddEditCategoryDialog` allowing users to edit category names, transaction types (Expense, Income, Investment, Transfer), and emojis.
  - **Color Protection**: Retained `CategoryColorProvider.getCategoryColor(categoryName)` auto-assignment system to guarantee unique non-conflicting colors and prevent duplicate color assignment collisions across categories.
- **Smart Category Emoji Engine (`CategoryEmojiProvider.kt`)**:
  - Created [CategoryEmojiProvider.kt](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/core/ui/theme/CategoryEmojiProvider.kt) mapping category names and keywords to emojis (🍕 Food, 🛒 Groceries, 🚗 Transportation, ⛽ Fuel, 💡 Bills, 📈 Investments, 🏠 Rent, 💳 EMI, 💰 Income, 💸 Transfers).
  - Applied dynamic emoji resolution across [DashboardScreen.kt](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/dashboard/DashboardScreen.kt), [TransactionsScreen.kt](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/transactions/TransactionsScreen.kt), [CategoriesScreen.kt](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/categories/CategoriesScreen.kt), and [AnalyticsScreen.kt](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/analytics/AnalyticsScreen.kt).

### 🧠 12. In-Place Category Editing Fix & Category Memory Learning Agent
- **In-Place Category Editing Bug Resolved (`CategoriesViewModel.kt`, `CategoryDao.kt`)**:
  - Seeded standard default categories into Room database (`categories` table) upon app launch, ensuring every category has a permanent, positive primary key `id > 0`.
  - Updated `updateCategory` to perform a true SQL `UPDATE` in-place (`updateCategoryInPlace`), eliminating duplicate category creation when editing existing categories.
  - Added historical transaction category synchronization (`updateTransactionCategoryName`), automatically updating past transactions matching the old category name to the new category name in Room.
- **Dedicated Category Memory Agent Skill & Preference Learning (`CategoryClassifier.kt`, `MerchantCategoryMappingEntity`)**:
  - Defined dedicated agent skill [.agents/skills/category_assignment_agent/SKILL.md](file:///E:/ExpenseTrackerApp/.agents/skills/category_assignment_agent/SKILL.md) for category memory and assignment auditing.
  - Automatically records user-assigned merchant category choices `(merchant -> category)` in `MerchantCategoryMappingEntity` whenever a transaction category is edited or added in `DashboardViewModel` or `TransactionsViewModel`.
  - When repeated transactions arrive from the same merchant (e.g., subsequent `Swiggy` or `Zepto` transactions), `CategoryClassifier` checks learned memories **first**, automatically assigning your last chosen category instead of falling back to general defaults.

### 🔄 13. Intelligent Subscriptions & Mandates Engine Overhaul
- **Auto Transaction-to-Subscription Matching (`SubscriptionMatcher.kt`)**:
  - Automatically matches incoming bank SMS or notification debits (e.g. `Netflix ₹499`, `Jio Fiber ₹999`, `Rent ₹15,000`, `SIP ₹5,000`) against active subscriptions.
  - Automatically marks the subscription as **"Auto-Paid via SMS 🟢"**, advances the `nextDueDateMs` by its billing cycle (Monthly/Yearly/Quarterly/Weekly), and links the transaction `id`.
- **Smart Subscription Radar (`SubscriptionDetector.kt`)**:
  - Scans historical transactions for repeated debits around monthly intervals, displaying 1-tap **"Detected Subscription Suggestions"** banners at the top of [SubscriptionsScreen.kt](file:///E:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/subscriptions/SubscriptionsScreen.kt).
- **Dashboard Monthly Mandates Card (`DashboardScreen.kt`, `DashboardViewModel.kt`)**:
  - Renders a dedicated **"Monthly Mandates & Subscriptions 🔄"** overview card on the Dashboard showing Total Committed Outflows, Amount Paid So Far, and Pending/Upcoming Bills.

### 🔐 14. Automatic First-Launch Permission Prompting (`DashboardScreen.kt`)
- **Automatic OS Permission Request on Launch**:
  - Implemented automatic runtime permission launching in `DashboardRoute` (`LaunchedEffect(Unit)`).
  - When the app is opened for the first time (or whenever `READ_SMS`, `RECEIVE_SMS`, or `POST_NOTIFICATIONS` permissions are missing), the OS permission dialog is triggered automatically without requiring the user to find and tap an "Allow Permission" button.
- **Immediate Post-Consent SMS Scanning**:
  - In the permission result callback, as soon as the user taps "Allow", `triggerSmsInboxScan(context)` automatically triggers to scan the current month's bank SMS transactions and instantly populate the Dashboard!

### 🏛️ 15. Modular AI Governance Framework & Engineering Standards Adaptation
- **Adapted Android AI Prompt (`PromptForCleanAndroidAppDevelopmentFromAIAgent.md`)**:
  - Adapted the master system prompt from web development into a specialized, offline-first Android Native architecture prompt covering Jetpack Compose, Room + SQLCipher AES-256, Biometrics, and dual-channel SMS/Notification regex ingestion.
- **Lean Active Cockpit & Catalogs**:
  - Initialized [PROJECT_TRACKER.md](file:///e:/ExpenseTrackerApp/PROJECT_TRACKER.md) (< 150 lines) with active sprint health, Gradle 1-click launchers, and subsystem statuses.
  - Initialized [CHANGELOG.md](file:///e:/ExpenseTrackerApp/CHANGELOG.md) following Keep a Changelog standards.
  - Initialized [FEATURES.md](file:///e:/ExpenseTrackerApp/FEATURES.md) documenting 100% of capabilities across End-User, Ingestion Daemon, Privacy Guardian, and Storage Maintenance personas.
- **Modular Architecture Cheatsheets (`docs/architecture/`, < 80 lines each)**:
  - Created [PLATFORM_ARCHITECTURE.md](file:///e:/ExpenseTrackerApp/docs/architecture/PLATFORM_ARCHITECTURE.md) with visual Mermaid diagrams (Clean Architecture & ERD).
  - Created [SMS_INGESTION_AND_NLP.md](file:///e:/ExpenseTrackerApp/docs/architecture/SMS_INGESTION_AND_NLP.md), [DATABASE_AND_SQLCIPHER.md](file:///e:/ExpenseTrackerApp/docs/architecture/DATABASE_AND_SQLCIPHER.md), [COMPOSE_UI_AND_THEME.md](file:///e:/ExpenseTrackerApp/docs/architecture/COMPOSE_UI_AND_THEME.md), and [SECURITY_AND_BIOMETRICS.md](file:///e:/ExpenseTrackerApp/docs/architecture/SECURITY_AND_BIOMETRICS.md).
- **Team Onboarding & Testing Playbooks (`docs/development/`)**:
  - Created [DEVELOPER_ONBOARDING.md](file:///e:/ExpenseTrackerApp/docs/development/DEVELOPER_ONBOARDING.md), [QA_TESTING_PLAYBOOK.md](file:///e:/ExpenseTrackerApp/docs/development/QA_TESTING_PLAYBOOK.md), [INGESTION_CONTRACTS.md](file:///e:/ExpenseTrackerApp/docs/development/INGESTION_CONTRACTS.md), [DATABASE_WORKFLOW.md](file:///e:/ExpenseTrackerApp/docs/development/DATABASE_WORKFLOW.md), and [SECURITY_AND_PRIVACY_AUDIT.md](file:///e:/ExpenseTrackerApp/docs/development/SECURITY_AND_PRIVACY_AUDIT.md).
- **Environment & Multi-AI Governance**:
  - Added [local.properties.example](file:///e:/ExpenseTrackerApp/local.properties.example), [.github/PULL_REQUEST_TEMPLATE.md](file:///e:/ExpenseTrackerApp/.github/PULL_REQUEST_TEMPLATE.md), [.github/ISSUE_TEMPLATE/bug_report.md](file:///e:/ExpenseTrackerApp/.github/ISSUE_TEMPLATE/bug_report.md), and [.github/ISSUE_TEMPLATE/parser_request.md](file:///e:/ExpenseTrackerApp/.github/ISSUE_TEMPLATE/parser_request.md).
  - Unified multi-agent rules across [AGENTS.md](file:///e:/ExpenseTrackerApp/AGENTS.md), [.cursorrules](file:///e:/ExpenseTrackerApp/.cursorrules), [CLAUDE.md](file:///e:/ExpenseTrackerApp/CLAUDE.md), and [.github/copilot-instructions.md](file:///e:/ExpenseTrackerApp/.github/copilot-instructions.md).

### 🛡️ 16. Windowed Deduplication, Flaw Remediations & System Hardening
- **Duplicate Transaction Catching & Suppression Resolution**:
  - **Root Cause**: Duplicate transactions were appearing because:
    1. `SmsReceiver` and `ExpenseNotificationListenerService` stamped transactions with `System.currentTimeMillis()`, causing disparate millisecond timestamps and hashes between an SMS and a push notification arriving seconds apart.
    2. `TransactionDao.findCandidateDuplicate` executed `WHERE type = :type AND ABS(amount - :amount) < 0.01 LIMIT 1` without ordering or time-window constraints. In any table with historical purchases of the same amount, SQLite consistently returned an ancient transaction from months ago, causing the deduplicator to compare against an unrelated merchant and miss the duplicate.
    3. Blind deletion in `cleanupExistingDuplicates` lacked time windows, mistakenly deleting legitimate repeat purchases made on different days.
  - **The Fix**:
    - Replaced blind queries with `findDuplicatesInWindow(type, amount, timestamp, windowMs)` and `findCreditCardPaymentsInWindow(amount, timestamp, windowMs)`.
    - Enforced a standard 2-hour dual-channel deduplication window (`DEFAULT_DEDUPLICATION_WINDOW_MS = 2 * 60 * 60 * 1000L`) and a 4-day credit card settlement window (`CC_PAYMENT_DEDUPLICATION_WINDOW_MS = 4 * 24 * 60 * 60 * 1000L`).
    - Guarded `cleanupExistingDuplicates` so historical repeat transactions outside the window are never deleted.
- **Android KeyStore SQLCipher Encryption**:
  - Replaced hardcoded plaintext passphrases (`"expense_tracker_secret_passphrase_key"`) with `SecurityKeyManager.kt`, which securely generates and seals a 256-bit AES master key in `AndroidKeyStore`.
- **Biometric App Lock Background Grace Period**:
  - Enhanced `MainActivity.kt` with `onStop` tracking: when backgrounded beyond the 30-second grace period (`lockGracePeriodMs = 30_000L`), `isUnlocked` resets to `false` and prompts biometric authentication upon return.
- **Live Ingestion Merchant Preference Learning**:
  - Connected `MerchantCategoryMappingDao` into `SmsReceiver`, `ExpenseNotificationListenerService`, and `SmsScanner` so user category customizations are applied in real-time during incoming transaction ingestion.
- **WorkManager Crash & Schedule Stability**:
  - Removed unused `transactionDao` argument from `BillReminderWorker` constructor that triggered reflection `NoSuchMethodException`.
  - Switched `DailyReviewWorker` enqueue policy to `ExistingPeriodicWorkPolicy.KEEP` to prevent resetting review notifications every time the user opens the app.
- **Pure-Kotlin Multi-Entity JSON Backup Engine**:
  - Rewrote `BackupManager.kt` using pure Kotlin string tokenization and JSON serialization, eliminating JVM stub exceptions and supporting all 5 entities (`transactions`, `categories`, `budgets`, `subscriptions`, and `merchantCategoryMappings`).
- **Live Home Screen App Widget Integration**:
  - Connected `ExpenseWidgetProvider.kt` with encrypted Room database queries to dynamically render current net balance and today's total spend, with automatic updates triggered upon transaction ingestion.
- **Room Database Hardening**:
  - Enabled Room schema export (`exportSchema = true`) and removed destructive migrations to protect user financial records.

---

## 2. Automated Test Verification

Executed Gradle unit test suite:
```cmd
cmd.exe /c "set JAVA_HOME=E:\ExpenseTrackerApp\jdk-tmp\jdk-17.0.10+7&& gradlew.bat testDebugUnitTest"
```
All unit tests compiled and passed cleanly with zero errors!






