# Expense Tracker App - Implementation Blueprint
Version: 4.0 | Technical Stack & Architectural Guidelines

# 1. Recommended Technology Stack
- Language: Kotlin 1.9+ (Coroutines, Flow, StateFlow)
- UI Framework: Jetpack Compose (Material3 Design)
- Database: Room + SQLCipher for Android (AES-256 Encryption)
- Architecture: MVVM + Clean Architecture + Hilt DI
- Security: Android BiometricPrompt API + SQLCipher Hardware-backed encryption
- AI Subsystem: MediaPipe LLM Inference API / ONNX Runtime for local Gemma, Phi, and Qwen models

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
- ai/ (AiEngine, GemmaEngine, PhiEngine, MediaPipeEngine, ModelDownloader, HardwareDetector)

# 3. Pluggable AI Subsystem Architecture

```kotlin
interface AiEngine {
    val modelName: String
    val isModelLoaded: Boolean

    suspend fun initialize(context: Context, modelPath: String): Boolean
    suspend fun categorizeTransaction(rawText: String): CategoryPrediction
    suspend fun generateSpendingInsights(transactions: List<TransactionEntity>): String
    suspend fun predictForecast(transactions: List<TransactionEntity>): ForecastResult
    fun unload()
}
```

### Key Design Principles:
1. **Complete Decoupling**: Business logic interacts only through `AiEngine` contract.
2. **Dynamic Fallback**: If `isModelLoaded` is false, system silently falls back to `RegexTransactionParser`.
3. **Optional Downloads**: Download manager handles `.bin` / `.onnx` model weights over Wi-Fi.
4. **Hardware Safety**: Disables LLM execution on devices with < 4GB RAM to prevent Out-Of-Memory crashes.