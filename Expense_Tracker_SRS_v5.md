# Expense Tracker App - Software Requirements Specification (SRS)
Version: 6.6 | Platform Target: Android Native (Kotlin + Compose)

This document serves as the master blueprint for developing an offline-first, privacy-focused Android expense tracker with automated multi-channel transaction parsing, SQLCipher encryption, on-device machine learning, smart NLP regex engines, monthwise SMS scanning, Budgets Engine, Analytics Charts, Encrypted Backup & Restore, Unified Editable Category Manager, Category Memory & Preference Learning Agent, Intelligent Subscriptions & Mandates Engine (Auto SMS Matching, Smart Subscription Radar, Dashboard Commitment Cards), Automatic First-Launch Permission Prompting, Offline Peer-to-Peer Split Expenses (Splitwise alternative), Header Settings Navigation, Today/Weekly Spending Cards, Interactive Canvas Charts, Bill & EMI Push Notification Reminders, Home Screen App Widgets, Pluggable On-Device AI Engine Architecture, In-House Privacy-First Promotion System, and Dedicated SMS Parser Agent Auditing Infrastructure.

## Phase 1: Ingestion & Storage Core (COMPLETED & ENHANCED)
- Automatic First-Launch Permission Prompting (`DashboardRoute` in `DashboardScreen.kt`): Automatically triggers system runtime permission dialogs (`READ_SMS`, `RECEIVE_SMS`, `POST_NOTIFICATIONS`) on first app launch when permissions are missing, immediately initiating current month SMS scanning upon user consent without requiring manual button clicks.

# 1. Vision
Build a privacy-first personal expense tracker that automatically captures bank and UPI financial transactions via SMS and push notifications, categorizes transactions using lightweight local ML and smart NLP rules, provides group bill splitting, budgeting, and analytics, and never requires company-hosted storage for user financial data.

# 2. Objectives
- Strictly offline-first core operating model.
- No mandatory user login or account creation.
- Local Room database protected with SQLCipher hardware-backed AES-256 encryption.
- Fast (<2 second launch time) and lightweight execution footprint.
- User owns 100% of their data with private user-controlled cloud backups.
- Multi-channel automated transaction ingestion (SMS BroadcastReceiver + NotificationListenerService).
- Historical 3-month SMS inbox scanning starting from the 1st of 3 months prior upon first install (e.g., Oct -> July 1st), with incremental timestamp updates thereafter.

# 3. Target Users
Students, salaried employees, families, freelancers, room-mates, trip groups, and small business owners seeking automated, private expense tracking and offline bill splitting.

# 4. Functional Requirements & Roadmap Phases

## Phase 1: Core Automated Ingestion & Parsing (COMPLETED & REFINED)
- Dedicated SMS Transaction Parser Agent Infrastructure (`.agents/skills/sms_transaction_parser/SKILL.md`): Custom subagent skill providing automated benchmarking, regex safety validation, edge-case generation, and test suite auditing to prevent parser regression.
- Multi-Stage Semantic NLP Financial Message Classifier Engine (`RegexTransactionParser.kt`) recognizing 6 message categories:
  1. Real Outgoing Expense (`DEBIT`)
  2. Genuine External Income (`CREDIT`)
  3. Investments (`INVESTMENT`: SIP, Mutual Funds, Zerodha, Groww, INDmoney, Stocks, NPS, PPF, FD, Gold, LIC)
  4. Internal Account Transfers & Card Bill Settlements (`TRANSFER`)
  5. Informational Statement & Initiated Refund Alerts (Rejection Filter: statement sent, min due, has been initiated)
  6. Promotional Loan / Festive Offer / EMI Marketing / News Digest / Bill Due Reminder Filter (Rejection Filter: news articles, Groww/Market digests, "$X billion capital inflow", "cr order", Sensex/Nifty alerts, "bill due in X days", "pay instantly on Paytm to avoid late fee", "YES BANK Festive Offer", "festive loan", "digital authorization via link below", "convert your transaction of X into EMI", "1 step away", "no paperwork needed").
- Regex Substring Safety & Word Boundary Protection: Strict word boundary checks (`\b`) prevent short service matchers (e.g. `\bcred\b`, `\bcheq\b`) from colliding with natural language verbs like `"to credit"`.
- Mandatory Execution Action Verification: Ensures investment and credit card payment messages explicitly contain execution action verbs ("debited", "spent", "invested", "units allocated", "order executed", "paid") before parsing as completed transactions.
- Payment Receipt Detection: Merchant/utility/insurance/government fee payment confirmations ("we have received a payment of Rs 347.16 for your Airtel Black ID", "We received payment for your bike insurance") are categorized as `DEBIT` / Expense, with strict card settlement exclusion guards.
- Beneficiary & Entity Extraction: Strips `HH:MM:SS` timestamps to prevent time strings from becoming merchant names, extracting true payee/beneficiary names (e.g. `ARCHIS KISHORRAO SONWAL`, `Airtel Black`, `Airtel Wi-Fi`).
- Credit Card & Bill App Accounting Intelligence (`RegexTransactionParser.kt`): Individual card purchases (Swipes, Online Gateways, RuPay UPI on CC) are categorized as `DEBIT` (Real Expense). Credit Card Bill Payments via third-party apps (`CRED`, `CheQ`, `BillDesk`, `BBPS`, NetBanking) and card issuer payment confirmations (HDFC, SBI Card, ICICI, Axis, Kotak, BOBCARD, OneCard, Amex) are categorized as `Merchant: Credit Card Bill`, `Category: Bills & Utilities`, `Type: TRANSFER` (Debt Repayment), preventing double-counting in Total Monthly Expense calculations and suppressing artificial income inflation. Universal VPA handle pattern matching (`(?i)\b[a-z0-9._-]*?(ccpay|cardpay|cardbill|billdesk|bbps|cred|cheq)[a-z0-9._-]*?@[a-z]+\b`) dynamically captures payments across present and future third-party payment aggregators. Dashboard presents transfers with `↔ ₹` in neutral secondary styling, with self-healing migration in `TransactionDeduplicator`.
- Windowed Dual-Channel Anti-Duplication Engine (`TransactionDeduplicator.kt`): Enforces a 2-hour dual-channel matching window (`DEFAULT_DEDUPLICATION_WINDOW_MS = 2h`) between SMS and status bar push notifications, and a 4-day credit card settlement window (`CC_PAYMENT_DEDUPLICATION_WINDOW_MS = 4d`) for bank debits vs card receipt confirmations via `findDuplicatesInWindow` and `findCreditCardPaymentsInWindow`. Resolves timeless duplicate suppression so legitimate repeat purchases across days are never dropped.
- Hardware-Backed SQLCipher AES-256 (`SecurityKeyManager.kt`): Android KeyStore master key generation and derivation, eliminating static passphrases for Room encryption.
- Responsive UI Layout (`DashboardScreen.kt`): Added flex weights, `contentPadding`, and vertical wrapping prevention (`maxLines = 1, softWrap = false`) for transaction buttons and dialogs.
- Precise Amount Extraction ignoring trailing available balance, credit limit, and magnitude scale clauses (billion, million, crore, cr, lakh, points, %).
- 100% On-Device Receipt & Invoice OCR Scanner (`ReceiptOcrParser.kt`, `ReceiptScannerDialog.kt`): Offline optical character recognition via bundled Google ML Kit Text Recognition (`com.google.mlkit:text-recognition:16.0.0`) packaged within the APK with zero network permissions. Includes camera sensor & EXIF upright rotation compensation, multi-line proximity total matching, item count noise rejection ("Total items: 3"), detected amount chips for 1-tap selection, linked credit card payment method selector, and expandable raw extracted text drawer before encrypted Room persistence.
- Dual-channel detection: Android SMS Receiver and NotificationListenerService.
- Historical 3-Month & Incremental SMS Scanner (`SmsScanner.kt`).

## Phase 2: Navigation & Navigation Structure (COMPLETED)
- Header Settings Navigation: Top app bar includes an explicit Settings IconButton (`Icons.Default.Settings`) accessible across all tabs.
- Material3 Bottom Navigation Bar with 5 dedicated main tabs:
  1. **Dashboard** (`Icons.Default.Dashboard`)
  2. **History** (`Icons.Default.ReceiptLong`)
  3. **Split Bills** (`Icons.Default.Group`) — *Group Expenses & Ledger*
  4. **Budgets** (`Icons.Default.AccountBalanceWallet`)
  5. **Analytics** (`Icons.Default.PieChart`)

## Phase 3: Offline Peer-to-Peer Split Expenses (Splitwise Alternative) (COMPLETED & ENHANCED)
- **100% Offline Group Ledger Engine**: Track shared trip expenses, apartment bills, and dinner splits without external cloud servers or user registration.
- **Room Entities**: `GroupEntity`, `GroupMemberEntity`, `SplitExpenseEntity`, `ExpenseSplitShareEntity`, `GroupSettlementEntity`.
- **Auto-Calculated Net Balances & Debt Minimization Algorithm**: Calculates exact net member balances and minimal transfers ("Who Owes Whom").
- **Itemized Restaurant & Group Bill Splitter (`ItemizedSplitCalculator.kt`, `ItemizedBillSplitterDialog.kt`)**: Proportionally divides dining bills, GST/taxes, and tips according to individual dish consumption, with 1-click import into group ledgers and formatted WhatsApp summaries.
- **Air-Gapped Offline QR Group Sync (Zero-Internet Splitwise) (`GroupQrSyncEngine.kt`, `GroupQrShareDialog.kt`, `GroupQrScannerDialog.kt`)**: Serverless peer-to-peer sync compressing group ledgers into high-density GZIP Base64 QR codes via ZXing, with live CameraX barcode scanner, gallery screenshot picker, clipboard paste fallback, symmetric identity perspective remapping ("You" <-> Friend), and idempotent Room merging.
- **1-Click WhatsApp Settlement Summary Exporter**: Exports formatted markdown summary directly to Android clipboard for instant WhatsApp group pasting.
- **Settlement Payments**: Record direct settlement payments between group members.

## Phase 4: Privacy, Security & Data Export (COMPLETED & HARDENED)
- Biometric App Lock (Fingerprint, Face Unlock, PIN) with 30s background re-lock grace period and single-prompt debouncing to eliminate duplicate prompt race conditions during cold start.
- **Peek Privacy Mode & Shake-to-Hide (`PrivacyModeManager.kt`, `ShakeDetector.kt`)**: Hardware accelerometer listener (2.6g threshold, 1200ms debounce, haptic vibration feedback) and top bar eye toggle. Exposes `LocalPrivacyMode` CompositionLocal to mask all sensitive balances, budgets, and transaction amounts with `₹•••••` across Dashboard, History, and Budgets, with configurable user preferences in Settings.
- **Automated Local Rolling Backups (`ScheduledBackupWorker.kt`)**: Background WorkManager periodic worker snapshotting the complete encrypted Room database weekly to the local sandbox, retaining the latest 5 rolling snapshots and auto-purging older ones without cloud or internet dependencies, with in-app restoration dialog in Settings.
- **Android Quick Settings Tile & Instant Cash Logging (<3s) (`CashQuickLogTileService.kt`, `QuickCashLogDialog.kt`)** Notification quick tile enabling instant cash transaction logging with 1-tap presets (`☕ Chai ₹20`, `🛺 Auto ₹50`, `🥪 Snacks ₹100`, `🍛 Lunch ₹150`, `🛒 Groceries ₹500`, `⛽ Fuel ₹500`) directly from lockscreen or dashboard.
- Encrypted JSON Disaster-Recovery Backup & Restore via SAF (Schema v3): Complete snapshot covering transactions (with `taxDeductibleSection`), categories (with `isCustom`, `emoji`), budgets (with `period`), subscriptions (with `autoMatched`, `lastPaidDateMs`), merchant mappings, savings goals (`savingsGoals`), credit cards (`creditCards`), recurring incomes (`recurringIncomes`), and annual insurance policies (`insurancePolicies`), and all Splitwise P2P groups, members, split expenses, shares, and settlements, with name-based upsert avoiding duplicate key violations.
- High-Throughput Database Indexing & Schema v8: Added `credit_cards` table, `savings_goals` table, `recurring_incomes` table, `insurance_policies` table, `taxDeductibleSection` column on `transactions`, and indexes on `timestamp`, `(type, timestamp)`, and `category` via non-destructive `MIGRATION_4_5`, `MIGRATION_5_6`, `MIGRATION_6_7`, and `MIGRATION_7_8`.
- CSV & Formatted Summary Text Reports Exporter with dedicated transfer (`↔ `) and investment (`📈 `) indicators.

## Phase 5: Customization & Subscriptions (COMPLETED & OVERHAULED)
- **Recurring Inflow Engine & Salary Day Radar (`RecurringIncomeEntity.kt`, `RecurringIncomeDao`, `RecurringInflowReconciler.kt`)**: Automatically manages regular anticipated income streams (Monthly Salary, Rental Income, Dividends, LIC annuity payouts, Freelance retainers) with cycle frequencies (Monthly, Quarterly, Yearly, Bi-Weekly). Reconciles incoming bank `CREDIT` notifications against expected income streams by amount tolerance (±5%), merchant/employer keywords, and settlement mode without creating phantom double-counted transactions, and presents a live "Scheduled Inflows & Salary" horizon card on the Dashboard with progress bars and 1-tap "Mark Received" action.
- **Annual Insurance & Policy Vault with Salary Day Smart Stash (`InsurancePolicyEntity.kt`, `InsurancePolicyDao`, `SalaryProvisioningCalculator.kt`, `SalaryProvisioningNotificationHelper.kt`)**: Centrally tracks Health Insurance (Mediclaim 80D), Term Life (80C), Car Insurance, Bike Insurance, and LIC policies with 1-tap automated Sinking Jar auto-creation, dynamic monthly provisioning pace calculation, high-priority local Salary Day alerts (`SalaryProvisioningNotificationHelper`), and interactive Dashboard/Subscriptions stash dialogs with optional ledger sync.
- **Subscriptions Screen 3-Tab Overhaul (`SubscriptionsScreen.kt`, `SubscriptionsViewModel.kt`)**: Complete redesign into a unified 3-tab financial control center (`Mandates & Subscriptions`, `Recurring Inflow Horizon`, `Annual Insurance Policy Vault`) with custom preset dialogs, category/tax badges, and filtering.
- **Stealth Inflation & Subscription Price Creep Detector (`PriceCreepDetector.kt`)**: Real-time analysis engine identifying price hikes (>3% and >= ₹5) across repeat merchants and subscriptions, rendering subtle alert badges in Subscriptions and Transaction Details.
- Unified Editable Category Management (`CategoriesScreen.kt`, `CategoriesViewModel.kt`): Merged system and custom categories into a single editable management list where users can edit category names, transaction types, and emojis.
- Dynamic Budget Categories & Safe Upsert (`BudgetsViewModel.kt`, `BudgetRepository.kt`): Dynamically surfaces user custom and system categories in Add Budget dialog and ensures idempotent upserting by category name to eliminate duplicate entries.
- System Color Auto-Assignment (`CategoryColorProvider.kt`): Guarantees unique, non-conflicting vibrant colors for all categories, preventing duplicate color assignments across the app.
- Smart Name-Based Category Emojis (`CategoryEmojiProvider.kt`): Maps category names and keywords to visual emojis (🍕 Food, 🛒 Groceries, 🚗 Transportation, ⛽ Fuel, 💡 Bills, 📈 Investments, 🏠 Rent, 💳 EMI, 💰 Income, 💸 Transfers).
- Recurring Subscriptions & Mandates Architecture (`SubscriptionEntity`, `SubscriptionDao`, `RecurringMandateDetector`): Full database-backed management of recurring mandates (Netflix, Rent, SIPs, Autopays) with automated SMS/notification NLP detection, auto-detection during 3-month historical scan, manual creation dialogs with explicit Material3 `DatePickerDialog` modals (eliminating hardcoded offsets), cycle billing (Monthly, Yearly, Weekly, Quarterly), pause/resume toggles, and status due badges (*Due Today*, *Overdue*, *Due in X Days*).
- Recurring Inflows & Policy Vault Date Pickers (`SubscriptionsScreen.kt`): Added interactive Material3 calendar pickers across `AddEditRecurringIncomeDialog` (synchronizing cycle dates with days of month) and `AddEditInsurancePolicyDialog` (allowing custom policy renewal dates rather than hardcoded 180-day offsets).
- Monthly Budget Calendar Scoping: Category spent calculations in `BudgetsViewModel.kt` are strictly bounded to the active calendar month.
- **Visual Sinking Funds & Savings Goals (Interactive "Jars") (`SavingsGoalEntity.kt`, `SavingsGoalsViewModel.kt`, `SavingsGoalsScreen.kt`)**: Interactive savings jars with target amounts, visual progress gauges, monthly savings pace calculators, 1-tap deposit/withdraw dialogs integrated into `BudgetsScreen.kt` with isolated tabbed navigation preventing visual collision with Monthly Budgets, glanceable Dashboard live overview card, 4 built-in starter templates (`🛡️ Emergency Fund`, `🏖️ Vacation Trip`, `💻 Gadget & Tech`, `🚗 Vehicle Down Payment`), and optional dual ledger synchronization (`recordInLedger`) to automatically record deposits as `INVESTMENT` transactions and withdrawals as `CREDIT` transactions.
- Custom Transaction Date Selection: `AddTransactionDialog` supports an interactive Date Picker allowing manual logging of expenses, income, or investments for any historical date/time.

## Phase 6: Visual Canvas Charts & Quick Stats (COMPLETED & ENHANCED)
- **Executive Dashboard UI Overhaul & Above-the-Fold Transactions View (`DashboardScreen.kt`)**: Streamlined fintech architecture featuring a compact ~140dp gradient Net Cashflow hero card with an integrated 3-metric flow ribbon (Income 💰, Expenses 💸, Investments 📈), a concise ~75dp Daily Spending Radar bar (Safe-to-Spend Today, Spent Today, Unspent Pool, and cycle days remaining), compact monthly subscriptions strip, and complete removal of the 160dp empty Financial Horizons placeholder. Transactions feed begins immediately above the fold (~300dp from top), ensuring 5+ records are visible upon launch.
- **Safe-to-Spend Today Radar (`DashboardScreen.kt`, `DashboardViewModel.kt`)**: Real-time daily discretionary allowance calculation pacing remaining monthly budgets against remaining calendar days in the month, dynamically deducting committed upcoming subscriptions/bills with status badges (*Abundant*, *Balanced*, *Tight*, *Budget Exhausted*).
- **Credit Card Grace Period & 30% Utilization Radar (`CreditCardRadarCalculator.kt`, `CreditCardRadarCard.kt`)**: Tracks individual statement cycle dates, calculates interest-free grace days countdown, safeguards credit bureau score with 30% utilization threshold warnings, renders visual multi-bank gradient cards, accurately attributes transactions via multi-attribute tracking (account digits, notes, SMS text masked patterns, merchants, bank names), provides an expandable cycle transactions drawer, educational issuer setup dialog with 11 bank chips and statement vs due date guides, manual transaction card linking, and auto-discovers unregistered cards from SMS history.
- **Indian Tax Season Deductions Organizer (80C / 80D / HRA) (`TaxDeductionAnalyzer.kt`, `TaxSeasonDeductionsCard.kt`)**: Automatic inference and tracking of tax-exempt transactions under Sections 80C, 80D, Section 10(13A) HRA, 80CCD, 80E, 80G, statutory limits gauges, and 1-tap CA/ITR export.
- Interactive Donut Breakdown by Transaction Type (`AnalyticsScreen.kt`, `AnalyticsViewModel.kt`): Calculates Donut chart percentages strictly relative to the selected Transaction Type (`Expenses 💸`, `Income 💰`, `Investments 📈`, `Transfers 🔄`, or `All 🌐`).
- Interactive Jetpack Compose Canvas Income vs Expense Bar Chart.
- Financial Analytics Date & Period Filtering Bar: Single unified Period Selector dropdown ("All Time", "This Month", "Last Month", "Quarter", "6 Months", "Yearly", "Custom Dates", Specific Month choices).

## Phase 7: Push Reminders & Widget (COMPLETED & SMART-ENABLED)
- Smart Bill & EMI Notification Reminders (`BillReminderWorker.kt`): Database-aware WorkManager worker evaluating active subscriptions due in 48 hours and alerting users with customized bill details, suppressing false notifications when no bills are due.
- Contextual Daily Expense Review Notification (`DailyReviewWorker.kt`): Tailored evening reminder reflecting actual recorded spend for the day.
- Android Home Screen App Widget (`ExpenseWidgetProvider.kt`): Live widget dynamically rendering current month's net cashflow and today's spend with auto-refresh across all repository mutations (SMS/notifications, manual entry, edits, deletes) and direct root app launch intent.

## Phase 8: In-House Privacy-First Promotion System (COMPLETED)
- Backend-Independent Static JSON (`PromotionManager.kt`).
- Privacy-First: Zero third-party ad networks.

## Phase 9: Pluggable On-Device AI Subsystem (PLANNED ARCHITECTURE)
- Common `AiEngine` Interface for Google Gemma 2B, Phi-3, Qwen 1.5, and MediaPipe LLM Inference API.

## Phase 10: PDF Reports & Investment Portfolio (PLANNED)
- Formatted PDF Monthly Report Exporter with tables and visual charts.
- Investments Portfolio Tracker (Mutual Fund SIPs, FDs, and Stocks).

## Phase 11: Multi-AI Governance & Modular Documentation Framework (COMPLETED)
- Master Android AI Prompt: `PromptForCleanAndroidAppDevelopmentFromAIAgent.md`.
- Active Cockpit: `PROJECT_TRACKER.md` (< 150 lines).
- Master Feature Catalog: `FEATURES.md`.
- Historical Changelog Archive: `CHANGELOG.md`.
- Modular Architecture Cheatsheets (`docs/architecture/`): `PLATFORM_ARCHITECTURE.md`, `SMS_INGESTION_AND_NLP.md`, `DATABASE_AND_SQLCIPHER.md`, `COMPOSE_UI_AND_THEME.md`, `SECURITY_AND_BIOMETRICS.md`.
- Developer & QA Playbooks (`docs/development/`): `DEVELOPER_ONBOARDING.md`, `QA_TESTING_PLAYBOOK.md`, `INGESTION_CONTRACTS.md`, `DATABASE_WORKFLOW.md`, `SECURITY_AND_PRIVACY_AUDIT.md`.
- Pointer Rules: `AGENTS.md` (root), `.cursorrules`, `CLAUDE.md`, `.github/copilot-instructions.md`, and PR/Issue templates.