# Expense Tracker App - Implementation Blueprint
Version: 5.0 | Technical Stack & Architectural Guidelines

# 1. Recommended Technology Stack
- Language: Kotlin 1.9+ (Coroutines, Flow, StateFlow)
- UI Framework: Jetpack Compose (Material3 Design)
- Database: Room + SQLCipher for Android (AES-256 Encryption)
- Architecture: MVVM + Clean Architecture + Hilt DI
- Security: Android BiometricPrompt API + SQLCipher Hardware-backed encryption
- Split Ledger Engine: Offline P2P Group Expense & Debt Minimization Algorithm (`SplitRepository.kt`)
- Promotion System: Static `promotions.json` + `PromotionManager.kt` (No third-party ad SDKs)

# 2. Modular Package Structure
- features/main/ (MainScreen, Top Header Settings Button, Bottom NavigationBar)
- features/dashboard/ (DashboardScreen, DashboardViewModel, TransactionDetailsDialog, QuickStatsCards)
- features/transactions/ (TransactionsScreen, TransactionsViewModel, CategoryFilterChips)
- features/split/ (SplitScreen, SplitViewModel: Group List, Group Detail, Add Expense, Settle Up, WhatsApp Exporter)
- features/budgets/ (BudgetsScreen, BudgetsViewModel)
- features/analytics/ (AnalyticsScreen, AnalyticsViewModel, CanvasDonutChart, CanvasBarChart, DatePeriodFilterBar)
- features/categories/ (CategoriesScreen, CategoriesViewModel, CategoryColorProvider)
- features/subscriptions/ (SubscriptionsScreen, SubscriptionsViewModel, AddEditSubscriptionDialog, StatusBadges)
- features/settings/ (SettingsScreen, SettingsViewModel, EcosystemPromotionCard)
- features/security/ (BiometricLockManager)
- features/backup/ (BackupManager)
- features/reports/ (ReportsExporter)
- core/database/ (ExpenseDatabase v3, GroupEntity, GroupMemberEntity, SplitExpenseEntity, ExpenseSplitShareEntity, GroupSettlementEntity, SubscriptionEntity, SplitDao, SubscriptionDao)
- core/data/repository/ (SplitRepository, SubscriptionRepository: Mandates & Recurring Commitments)
- core/promotions/ (PromotionModel, PromotionManager)
- ingestion/parser/ (RegexTransactionParser, ParsedTransaction: Multi-Stage News Digest & Bill Due Rejection Engine, Mandatory Execution Action Validation)
- ingestion/deduplication/ (TransactionDeduplicator: Timeless & Windowed Deduplication & DB False Positive Purging)
- ingestion/worker/ (BillReminderWorker)
- widget/ (ExpenseWidgetProvider)

# 3. Parser Intelligence & Analytics Architecture
- **RegexTransactionParser**: 6-stage semantic pipeline filtering promotional offers, news/market digests ("$X billion inflow", "cr order"), and bill payment due reminders ("due in X days", "pay instantly on Paytm"), enforcing explicit execution action verbs. Classifies Credit Card swipes, online purchases, and RuPay UPI spends as `DEBIT` (Expenses), while classifying Credit Card Bill Payments (from Bank/CRED/CheQ/BillDesk/BBPS) and credit receipts as `TRANSFER` to prevent expense double-counting and income inflation. Universal VPA handle regex (`(?i)\b[a-z0-9._-]*?(ccpay|cardpay|cardbill|billdesk|bbps|cred|cheq)[a-z0-9._-]*?@[a-z]+\b`) dynamically handles unknown third-party payment aggregators.
- **Multi-Channel Ingestion & Deduplication**: Integrates `NotificationListenerService` and `SmsReceiver` for dual-channel ingestion, ensuring micro-transactions without bank SMS are captured via local push notifications. `TransactionDeduplicator` enforces a 60-minute matching window to prevent duplicate notification/SMS entries.
- **Offline Peer-to-Peer Group Ledger**: Room v2 entities storing split groups, members, shared expenses, and settlements. Computes real-time net member balances and minimal settlement debt pairs.
- **Header Settings & Bottom Navigation**: Settings moved to a prominent top app bar IconButton (`Icons.Default.Settings`). Bottom Navigation Bar updated to 5 main tabs: Dashboard, History, Split Bills, Budgets, Analytics.
- **Unified Categories**: Standardized 18 system category definitions across ViewModels and UI modals with dynamic color assignment via `CategoryColorProvider`.
- **Analytics Date Filter Pipeline**: StateFlow-driven reactive period filtering ("ALL", "THIS_MONTH", "LAST_MONTH", "LAST_3_MONTHS", "LAST_6_MONTHS", "YEARLY", "CUSTOM") connected directly to Canvas donut and bar chart calculations.