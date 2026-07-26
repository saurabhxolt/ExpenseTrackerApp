# Expense Tracker App - Software Requirements Specification (SRS)
Version: 5.0 | Platform Target: Android Native (Kotlin + Compose)

This document serves as the master blueprint for developing an offline-first, privacy-focused Android expense tracker with automated multi-channel transaction parsing, SQLCipher encryption, on-device machine learning, smart NLP regex engines, monthwise SMS scanning, Budgets Engine, Analytics Charts, Encrypted Backup & Restore, Custom Categories, Subscriptions Tracker, Transaction Details with Original SMS View launcher, Today/Weekly Spending Cards, Interactive Canvas Charts, Bill & EMI Push Notification Reminders, Home Screen App Widgets, Pluggable On-Device AI Engine Architecture, and In-House Privacy-First Promotion System.

# 1. Vision
Build a privacy-first personal expense tracker that automatically captures bank and UPI financial transactions via SMS and push notifications, categorizes transactions using lightweight local ML and smart NLP rules, provides budgeting and analytics, and never requires company-hosted storage for user financial data.

# 2. Objectives
- Strictly offline-first core operating model.
- No mandatory user login or account creation.
- Local Room database protected with SQLCipher hardware-backed AES-256 encryption.
- Fast (<2 second launch time) and lightweight execution footprint.
- User owns 100% of their data with private user-controlled cloud backups.
- Multi-channel automated transaction ingestion (SMS BroadcastReceiver + NotificationListenerService).
- Historical monthwise SMS inbox scanning starting from 1st day of the current month with incremental timestamp updates.

# 3. Target Users
Students, salaried employees, families, freelancers, and small business owners seeking automated, private expense tracking.

# 4. Functional Requirements & Roadmap Phases

## Phase 1: Core Automated Ingestion & Parsing (COMPLETED & REFINED)
- Multi-Stage Semantic NLP Financial Message Classifier Engine (`RegexTransactionParser.kt`) recognizing 5 message categories:
  1. Real Outgoing Expense (`DEBIT`)
  2. Genuine External Income (`CREDIT`)
  3. Internal Account Transfers (`TRANSFER`)
  4. Informational Statement & Initiated Refund Alerts (Rejection Filter: statement sent, min due, has been initiated)
  5. Promotional Loan / Disbursement Consent Alerts (Rejection Filter: require consent, disbursement, pre-approved)
- Payment Receipt Detection: Merchant/utility/insurance/government fee payment confirmations ("we have received a payment of Rs 347.16 for your Airtel Black ID", "We received payment for your bike insurance") are categorized as `DEBIT` / Expense.
- Beneficiary & Entity Extraction: Strips `HH:MM:SS` timestamps to prevent time strings from becoming merchant names, extracting true payee/beneficiary names (e.g. `ARCHIS KISHORRAO SONWAL`, `Airtel Black`, `Airtel Wi-Fi`).
- Timeless Anti-Duplication Engine (`TransactionDeduplicator.kt`) preventing double-counting of Credit Card bill payments across bank debit SMS and card issuer confirmation notifications regardless of days delayed, while allowing distinct same-amount transactions for different merchants.
- Responsive UI Layout (`DashboardScreen.kt`): Added flex weights and vertical wrapping prevention (`maxLines = 1, softWrap = false`) for transaction amounts in `TransactionDetailsDialog`.
- Precise Amount Extraction ignoring trailing available balance and credit limit clauses.
- Dual-channel detection: Android SMS Receiver and NotificationListenerService.
- Historical Monthwise & Incremental SMS Scanner.

## Phase 2: Navigation, Budgets & Search (COMPLETED)
- Material3 Bottom Navigation Bar with 5 main tabs.
- Category monthly spending caps with animated progress bars & color alerts (Green, Amber, Red).
- Search transactions by merchant or category.

## Phase 3: Privacy, Security & Data Export (COMPLETED)
- Biometric App Lock (Fingerprint, Face Unlock, PIN).
- Encrypted JSON Backup & Restore via SAF.
- CSV Spreadsheet Reports Exporter.

## Phase 4: Customization & Subscriptions (COMPLETED)
- Custom Category Creator & Color Picker.
- Recurring Subscriptions & Mandates Tracker (Netflix, Rent, SIP, Autopay).

## Phase 5: Visual Canvas Charts & Quick Stats (COMPLETED)
- Interactive Jetpack Compose Canvas Donut/Pie Chart for category spending distribution.
- Interactive Jetpack Compose Canvas Income vs Expense Bar Chart.
- Today's Spend & This Week's Spend quick summary cards on Dashboard.
- Category Filter Chips bar on Transactions screen.
- Undo / Restore Deleted Transaction snackbar functionality.

## Phase 6: Push Reminders & Home Screen Widget (COMPLETED)
- Bill & EMI Due Date Notification Reminders (scheduled 24-hourly via WorkManager).
- Android Home Screen App Widget for quick balance checking & 1-tap app launching.

## Phase 7: In-House Privacy-First Promotion System (COMPLETED)
- Backend-Independent Static JSON (`PromotionManager.kt` downloading `promotions.json` from static CDN/GitHub Pages/Firebase).
- Privacy-First: Zero third-party ad networks (No AdMob, No Audience Network, No user tracking).
- In-House Ecosystem Feature Cards on Settings tab promoting ecosystem products, plugins, and feature announcements.
- 100% Offline-Friendly: Caches JSON locally; displays nothing if offline without cache; zero disruption to core features.

## Phase 8: Pluggable On-Device AI Subsystem (PLANNED ARCHITECTURE)
- Common `AiEngine` Interface for Google Gemma 2B, Phi-3, Qwen 1.5, and MediaPipe LLM Inference API.
- Dynamic Hardware Detection & Graceful Fallback to Regex Parser.

## Phase 9: PDF Reports & Investments Portfolio (PLANNED)
- Formatted PDF Monthly Report Exporter with tables and visual charts.
- Investments Portfolio Tracker (Mutual Fund SIPs, FDs, and Stocks).