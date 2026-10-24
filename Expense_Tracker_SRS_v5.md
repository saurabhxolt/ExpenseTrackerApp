# Expense Tracker App - Software Requirements Specification (SRS)
Version: 6.5 | Platform Target: Android Native (Kotlin + Compose)

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
- Historical monthwise SMS inbox scanning starting from 1st day of the current month with incremental timestamp updates.

# 3. Target Users
Students, salaried employees, families, freelancers, room-mates, trip groups, and small business owners seeking automated, private expense tracking and offline bill splitting.

# 4. Functional Requirements & Roadmap Phases

## Phase 1: Core Automated Ingestion & Parsing (COMPLETED & REFINED)
- Dedicated SMS Transaction Parser Agent Infrastructure (`.agents/skills/sms_transaction_parser/SKILL.md`): Custom subagent skill providing automated benchmarking, regex safety validation, edge-case generation, and test suite auditing to prevent parser regression.
- Multi-Stage Semantic NLP Financial Message Classifier Engine (`RegexTransactionParser.kt`) recognizing 6 message categories:
  1. Real Outgoing Expense (`DEBIT`)
  2. Genuine External Income (`CREDIT`)
  3. Investments (`INVESTMENT`: SIP, Mutual Funds, Zerodha, Groww, INDmoney, Stocks, NPS, PPF, FD, Gold, LIC)
  4. Internal Account Transfers (`TRANSFER`)
  5. Informational Statement & Initiated Refund Alerts (Rejection Filter: statement sent, min due, has been initiated)
  6. Promotional Loan / Festive Offer / EMI Marketing / News Digest / Bill Due Reminder Filter (Rejection Filter: news articles, Groww/Market digests, "$X billion capital inflow", "cr order", Sensex/Nifty alerts, "bill due in X days", "pay instantly on Paytm to avoid late fee", "YES BANK Festive Offer", "festive loan", "digital authorization via link below", "convert your transaction of X into EMI", "1 step away", "no paperwork needed").
- Regex Substring Safety & Word Boundary Protection: Strict word boundary checks (`\b`) prevent short service matchers (e.g. `\bcred\b`, `\bcheq\b`) from colliding with natural language verbs like `"to credit"`.
- Mandatory Execution Action Verification: Ensures investment and credit card payment messages explicitly contain execution action verbs ("debited", "spent", "invested", "units allocated", "order executed", "paid") before parsing as completed transactions.
- Payment Receipt Detection: Merchant/utility/insurance/government fee payment confirmations ("we have received a payment of Rs 347.16 for your Airtel Black ID", "We received payment for your bike insurance") are categorized as `DEBIT` / Expense.
- Beneficiary & Entity Extraction: Strips `HH:MM:SS` timestamps to prevent time strings from becoming merchant names, extracting true payee/beneficiary names (e.g. `ARCHIS KISHORRAO SONWAL`, `Airtel Black`, `Airtel Wi-Fi`).
- Credit Card & Bill App Accounting Intelligence (`RegexTransactionParser.kt`): Individual card purchases (Swipes, Online Gateways, RuPay UPI on CC) are categorized as `DEBIT` (Real Expense). Credit Card Bill Payments via third-party apps (`CRED`, `CheQ`, `BillDesk`, `BBPS`, NetBanking) and credit card receipts are categorized as `Merchant: Credit Card Bill`, `Category: Bills & Utilities`, `Type: TRANSFER` (Debt Repayment), preventing double-counting in Total Monthly Expense calculations and suppressing artificial income inflation. Universal VPA handle pattern matching (`(?i)\b[a-z0-9._-]*?(ccpay|cardpay|cardbill|billdesk|bbps|cred|cheq)[a-z0-9._-]*?@[a-z]+\b`) dynamically captures payments across present and future third-party payment aggregators.
- Windowed Dual-Channel Anti-Duplication Engine (`TransactionDeduplicator.kt`): Enforces a 2-hour dual-channel matching window (`DEFAULT_DEDUPLICATION_WINDOW_MS = 2h`) between SMS and status bar push notifications, and a 4-day credit card settlement window (`CC_PAYMENT_DEDUPLICATION_WINDOW_MS = 4d`) for bank debits vs card receipt confirmations via `findDuplicatesInWindow` and `findCreditCardPaymentsInWindow`. Resolves timeless duplicate suppression so legitimate repeat purchases across days are never dropped.
- Hardware-Backed SQLCipher AES-256 (`SecurityKeyManager.kt`): Android KeyStore master key generation and derivation, eliminating static passphrases for Room encryption.
- Responsive UI Layout (`DashboardScreen.kt`): Added flex weights, `contentPadding`, and vertical wrapping prevention (`maxLines = 1, softWrap = false`) for transaction buttons and dialogs.
- Precise Amount Extraction ignoring trailing available balance, credit limit, and magnitude scale clauses (billion, million, crore, cr, lakh, points, %).
- Dual-channel detection: Android SMS Receiver and NotificationListenerService.
- Historical Monthwise & Incremental SMS Scanner.

## Phase 2: Navigation & Navigation Structure (COMPLETED)
- Header Settings Navigation: Top app bar includes an explicit Settings IconButton (`Icons.Default.Settings`) accessible across all tabs.
- Material3 Bottom Navigation Bar with 5 dedicated main tabs:
  1. **Dashboard** (`Icons.Default.Dashboard`)
  2. **History** (`Icons.Default.ReceiptLong`)
  3. **Split Bills** (`Icons.Default.Group`) — *Group Expenses & Ledger*
  4. **Budgets** (`Icons.Default.AccountBalanceWallet`)
  5. **Analytics** (`Icons.Default.PieChart`)

## Phase 3: Offline Peer-to-Peer Split Expenses (Splitwise Alternative) (COMPLETED)
- **100% Offline Group Ledger Engine**: Track shared trip expenses, apartment bills, and dinner splits without external cloud servers or user registration.
- **Room Entities**: `GroupEntity`, `GroupMemberEntity`, `SplitExpenseEntity`, `ExpenseSplitShareEntity`, `GroupSettlementEntity`.
- **Auto-Calculated Net Balances & Debt Minimization Algorithm**: Calculates exact net member balances and minimal transfers ("Who Owes Whom").
- **1-Click WhatsApp Settlement Summary Exporter**: Exports formatted markdown summary directly to Android clipboard for instant WhatsApp group pasting.
- **Settlement Payments**: Record direct settlement payments between group members.

## Phase 4: Privacy, Security & Data Export (COMPLETED)
- Biometric App Lock (Fingerprint, Face Unlock, PIN).
- Encrypted JSON Backup & Restore via SAF.
- CSV Spreadsheet Reports Exporter.

## Phase 5: Customization & Subscriptions (COMPLETED & OVERHAULED)
- Unified Editable Category Management (`CategoriesScreen.kt`, `CategoriesViewModel.kt`): Merged system and custom categories into a single editable management list where users can edit category names, transaction types, and emojis.
- System Color Auto-Assignment (`CategoryColorProvider.kt`): Guarantees unique, non-conflicting vibrant colors for all categories, preventing duplicate color assignments across the app.
- Smart Name-Based Category Emojis (`CategoryEmojiProvider.kt`): Maps category names and keywords to visual emojis (🍕 Food, 🛒 Groceries, 🚗 Transportation, ⛽ Fuel, 💡 Bills, 📈 Investments, 🏠 Rent, 💳 EMI, 💰 Income, 💸 Transfers).
- Recurring Subscriptions & Mandates Architecture (`SubscriptionEntity`, `SubscriptionDao`, `SubscriptionDetector`): Full database-backed management of recurring mandates (Netflix, Rent, SIPs, Autopays) with automated SMS/notification NLP detection, manual creation dialogs, cycle billing (Monthly, Yearly, Weekly, Quarterly), pause/resume toggles, and status due badges (*Due Today*, *Overdue*, *Due in X Days*).
- Custom Transaction Date Selection: `AddTransactionDialog` supports an interactive Date Picker allowing manual logging of expenses, income, or investments for any historical date/time.

## Phase 6: Visual Canvas Charts & Quick Stats (COMPLETED & ENHANCED)
- Interactive Donut Breakdown by Transaction Type (`AnalyticsScreen.kt`, `AnalyticsViewModel.kt`): Calculates Donut chart percentages strictly relative to the selected Transaction Type (`Expenses 💸`, `Income 💰`, `Investments 📈`, `Transfers 🔄`, or `All 🌐`).
- Interactive Jetpack Compose Canvas Income vs Expense Bar Chart.
- Financial Analytics Date & Period Filtering Bar: Single unified Period Selector dropdown ("All Time", "This Month", "Last Month", "Quarter", "6 Months", "Yearly", "Custom Dates", Specific Month choices).
- Today's Spend & This Week's Spend quick summary cards on Dashboard.

## Phase 7: Push Reminders & Widget (COMPLETED)
- Bill & EMI Due Date Notification Reminders (scheduled 24-hourly via WorkManager).
- Android Home Screen App Widget.

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