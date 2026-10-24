# Expense Tracker Platform - Product Roadmap
Version: 3.5 | Multi-Phase Long-Term Product Strategy

# Development Roadmap Status

- **Phase 1: Ingestion & Storage Core** — ✅ COMPLETED & ENHANCED
  - Automatic First-Launch Permission Prompting (`READ_SMS`, `RECEIVE_SMS`, `POST_NOTIFICATIONS`) + Dedicated SMS Transaction Parser Agent Infrastructure (`.agents/skills/sms_transaction_parser/SKILL.md`) + Multi-channel SMS & Notification Listener + Generic Multi-Stage NLP Parser (Festive Loan & EMI Conversion Marketing Rejection, Word Boundary Protection, News Digest & Bill Due Rejection, Credit Card Swipe DEBIT vs Bill Payment TRANSFER Classification, Universal VPA Handle Regex Engine, Mandatory Execution Verbs) + Timeless & Windowed Anti-Duplication Engine with DB Cleanup + 3-Month Historical Inbox Scanner on First Install + Credit Card Bill Identification Engine Hardening with UI Transfer Display (`↔ ₹`).
- **Phase 2: Budgets & Search** — ✅ COMPLETED
  - Header Settings IconButton + Bottom Navigation Bar + Category Budget Caps + Transaction Search & Unified Category Selector.
- **Phase 3: Offline Peer-to-Peer Split Expenses** — ✅ COMPLETED
  - Room v2 Group Ledger Entities + Simplified Debt Minimization Algorithm ("Who Owes Whom") + 1-Click WhatsApp Settlement Summary Exporter + Direct Settlement Payments.
- **Phase 4: Security & Export** — ✅ COMPLETED
  - Biometric App Lock + Encrypted JSON Backup/Restore + CSV Exporter.
- **Phase 5: Customization & Subscriptions** — ✅ COMPLETED & OVERHAULED
  - Intelligent Subscriptions & Mandates Engine (`SubscriptionMatcher.kt`, `SubscriptionDetector.kt`) with Auto SMS Transaction-to-Subscription Matching & Cycle Advancement, Smart Subscription Radar for 1-tap onboarding of detected recurring debits, Dashboard "Upcoming Mandates & Bills" Card, and Pre-Due Date Push Reminders + Dedicated Category Memory Agent Skill (`.agents/skills/category_assignment_agent/SKILL.md`) + Dynamic User Preference Memory Learning (`CategoryClassifier.kt`, `MerchantCategoryMappingEntity`) + Unified In-Place Editable Category Management (`CategoriesScreen.kt`, `CategoriesViewModel.kt`) + Historical Transaction Category Synchronization + `CategoryColorProvider` Auto-Color Assignment + Smart Category Emoji Engine (`CategoryEmojiProvider.kt`) + Custom Transaction Creation Date Selector.
- **Phase 6: Visual Charts & Quick Stats** — ✅ COMPLETED & ENHANCED
  - Donut Breakdown Chart by Transaction Type (`Expenses`, `Income`, `Investments`, `Transfers`, `All`) + Canvas Donut & Bar Charts + Financial Analytics Date & Period Filter Bar ("This Month", "Quarter", "Yearly", "Custom") + Today/Weekly Spending Cards + Category Filter Chips + Undo Delete.
- **Phase 7: Reminders & Widgets** — ✅ COMPLETED
  - Bill & EMI Due Date Push Reminders + Android Home Screen Widget.
- **Phase 8: In-House Promotion Engine** — ✅ COMPLETED
  - Backend-free static JSON promotion manager promoting ecosystem features & announcements (Zero third-party ads).
- **Phase 9: Pluggable On-Device AI Engine** — ⏳ ARCHITECTURE DESIGNED
  - Abstract `AiEngine` plugin interface for Gemma, Phi, Qwen, and MediaPipe on-device LLMs.
- **Phase 10: PDF Export & Investments** — ⏳ PLANNED
  - Native Formatted PDF Report Exporter + Investment Portfolio Tracker.