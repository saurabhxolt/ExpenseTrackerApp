# Expense Tracker App - Software Requirements Specification (SRS)
Version: 5.0 | Platform Target: Android Native (Kotlin + Compose)

This document serves as the master blueprint for developing an offline-first, privacy-focused Android expense tracker with automated multi-channel transaction parsing, SQLCipher encryption, on-device machine learning, smart NLP regex engines, monthwise SMS scanning, Budgets Engine, Analytics Charts, Encrypted Backup & Restore, Custom Categories, Subscriptions Tracker, Transaction Details with Original SMS View launcher, Today/Weekly Spending Cards, Interactive Canvas Charts, Bill & EMI Push Notification Reminders, Home Screen App Widgets, and Pluggable On-Device AI Engine Architecture.

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

## Phase 1: Core Automated Ingestion & Parsing (COMPLETED)
- Parse amount, merchant/VPA, account/card ending digits, balance, transaction type (DEBIT/CREDIT), and timestamp.
- Exclusion Filter Rules: Rejects non-transaction messages.
- Precise Amount Extraction.
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

## Phase 7: Pluggable On-Device AI Subsystem (PLANNED ARCHITECTURE)
- Common `AiEngine` Interface: Decouples AI model inference from UI, database, and business logic.
- Multi-Model Swappability: Support for local LLMs (Google Gemma 2B, Phi-3, Qwen 1.5, MediaPipe LLM Inference API).
- Dynamic Hardware Detection: Detects available NPU/GPU/RAM resources; gracefully disables AI on unsupported low-end devices.
- Optional Asset Downloads & Model Upgrades: Download model weights on-demand over Wi-Fi without app re-installation.
- 100% Offline & Zero Server Leak: All inference runs locally on-device. Fallback to smart regex parser if AI is unavailable.
- Features: "Where did my money go?" conversational assistant, automated spending forecasts, and anomaly detection.

## Phase 8: PDF Reports & Investments Portfolio (PLANNED)
- Formatted PDF Monthly Report Exporter with tables and visual charts.
- Investments Portfolio Tracker (Mutual Fund SIPs, FDs, and Stocks).