# Expense Tracker App - Implementation Blueprint
Version: 4.0 | Technical Stack & Architectural Guidelines

# 1. Recommended Technology Stack
- Language: Kotlin 1.9+ (Coroutines, Flow, StateFlow)
- UI Framework: Jetpack Compose (Material3 Design)
- Database: Room + SQLCipher for Android (AES-256 Encryption)
- Architecture: MVVM + Clean Architecture + Hilt DI
- Security: Android BiometricPrompt API + SQLCipher Hardware-backed encryption

# 2. Modular Package Structure
- features/main/ (MainScreen, Bottom NavigationBar)
- features/dashboard/ (DashboardScreen, DashboardViewModel, TransactionDetailsDialog, QuickStatsCards)
- features/transactions/ (TransactionsScreen, TransactionsViewModel, CategoryFilterChips)
- features/budgets/ (BudgetsScreen, BudgetsViewModel)
- features/analytics/ (AnalyticsScreen, AnalyticsViewModel, CanvasDonutChart, CanvasBarChart)
- features/categories/ (CategoriesScreen, CategoriesViewModel)
- features/subscriptions/ (SubscriptionsScreen, SubscriptionsViewModel)
- features/settings/ (SettingsScreen, SettingsViewModel)
- features/security/ (BiometricLockManager)
- features/backup/ (BackupManager)
- features/reports/ (ReportsExporter)
- ingestion/worker/ (BillReminderWorker)
- widget/ (ExpenseWidgetProvider)