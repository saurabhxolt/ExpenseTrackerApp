# Expense Tracker Platform - Product Roadmap
Version: 3.0 | Multi-Phase Long-Term Product Strategy

# Development Roadmap Status

- **Phase 1: Ingestion & Storage Core** — ✅ COMPLETED & ENHANCED
  - Multi-channel SMS & Notification Listener + Generic Multi-Stage NLP Parser (News Digest & Bill Due Rejection, Credit Card Swipe DEBIT vs Bill Payment TRANSFER Classification, Universal VPA Handle Regex Engine, Mandatory Execution Verbs) + Timeless & Windowed Anti-Duplication Engine with DB Cleanup + Monthwise Inbox Scanner.
- **Phase 2: Budgets & Search** — ✅ COMPLETED
  - Header Settings IconButton + Bottom Navigation Bar + Category Budget Caps + Transaction Search & Unified 18-Category Selector.
- **Phase 3: Offline Peer-to-Peer Split Expenses** — ✅ COMPLETED
  - Room v2 Group Ledger Entities + Simplified Debt Minimization Algorithm ("Who Owes Whom") + 1-Click WhatsApp Settlement Summary Exporter + Direct Settlement Payments.
- **Phase 4: Security & Export** — ✅ COMPLETED
  - Biometric App Lock + Encrypted JSON Backup/Restore + CSV Exporter.
- **Phase 5: Customization & Subscriptions** — ✅ COMPLETED & OVERHAULED
  - Custom Category Creator + Color Pickers + Database-backed Subscriptions & Mandates Architecture (`SubscriptionEntity`, `SubscriptionDao`, `SubscriptionDetector`) with automated NLP detection, manual creation dialogs, cycle billing, pause/resume toggles, and status due badges + Custom Transaction Creation Date Selector.
- **Phase 6: Visual Charts & Quick Stats** — ✅ COMPLETED
  - Canvas Donut & Bar Charts + Financial Analytics Date & Period Filter Bar ("This Month", "Quarter", "Yearly", "Custom") + Today/Weekly Spending Cards + Category Filter Chips + Undo Delete.
- **Phase 7: Reminders & Widgets** — ✅ COMPLETED
  - Bill & EMI Due Date Push Reminders + Android Home Screen Widget.
- **Phase 8: In-House Promotion Engine** — ✅ COMPLETED
  - Backend-free static JSON promotion manager promoting ecosystem features & announcements (Zero third-party ads).
- **Phase 9: Pluggable On-Device AI Engine** — ⏳ ARCHITECTURE DESIGNED
  - Abstract `AiEngine` plugin interface for Gemma, Phi, Qwen, and MediaPipe on-device LLMs.
- **Phase 10: PDF Export & Investments** — ⏳ PLANNED
  - Native Formatted PDF Report Exporter + Investment Portfolio Tracker.