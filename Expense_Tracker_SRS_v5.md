# Expense Tracker App - Software Requirements Specification (SRS)
Version: 5.0 | Platform Target: Android Native (Kotlin + Compose)

This document serves as the master blueprint for developing an offline-first, privacy-focused Android expense tracker with automated multi-channel transaction parsing, SQLCipher encryption, on-device machine learning, smart NLP regex engines, monthwise SMS scanning, Budgets Engine, Analytics Charts, Encrypted Backup & Restore, Custom Categories, Subscriptions Tracker, and Transaction Details with Original SMS View launcher.

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

# 4. Functional Requirements

## Automated Transaction Reader & Smart NLP Engine
- Parse transaction amount, merchant/VPA, account/card ending digits, balance, transaction type (DEBIT/CREDIT), and timestamp.
- Exclusion Filter Rules: Rejects non-transaction messages (e.g. account balance inquiries, e-mandates, upcoming bill reminders).
- Precise Amount Extraction: Isolates actual transaction spend/credit and ignores embedded Available Credit Limits or Current Outstanding balances.
- Support dual-channel detection: Android SMS Receiver and NotificationListenerService for banking/UPI apps (GPay, PhonePe, Paytm, CRED, HDFC, ICICI, SBI, Axis, BOBCARD, Kotak, etc.).
- Historical Monthwise & Incremental SMS Scanner: Scans inbox from the 1st of the current month on first run, and incrementally scans only new messages received after lastScanTimestamp on subsequent runs.

## Transactions Management, Search, Filters & Details
- Full Manual CRUD: Add, edit, delete, and duplicate transactions via FloatingActionButton (+) dialog.
- Live Custom Categories: Dynamically populates newly created custom categories in transaction add and edit modals.
- Edit & Re-Assign Category: Rename merchant titles, edit amounts, and reassign categories for any transaction.
- Transaction Details Modal: Full-screen modal displaying merchant name, category, exact timestamp, amount, transaction hash, and the raw SMS/statement string.
- View Original SMS Button: One-tap launcher button inside details modal to open the device native Messages application.
- Month-Year Filtering: Filter transactions by month and year (e.g., Jul 2026, Jun 2026, All).
- Sorting Controls: Sort transactions by Newest First, Oldest First, Amount: High to Low, or Amount: Low to High.

## Budgets & Spending Caps Engine
- Category monthly spending caps (e.g., ₹10,000 max for Food & Dining).
- Color-coded progress bars (Green < 80%, Amber 80-99%, Red >= 100%).
- Modal dialog to set or update category monthly spending limits.

## Custom Categories & Color Pickers
- Create, edit, and delete custom expense and income categories.
- Custom color picker palette selection for visual identification.

## Recurring Subscriptions & Mandates Tracker
- Autopay & Subscription Detection: Tracks monthly recurring payments (Netflix, Spotify, Rent, Broadband, Mutual Fund SIPs).
- Total Monthly Recurring Cost Header and due date badges.

## Analytics & Visual Breakdown Charts
- Cashflow Summary: Total Income vs Total Expenses comparison.
- Top Spending Category highlight badge.
- Category spending distribution leaderboard with percentage breakdown bars.

## Security & Backup
- Biometric App Lock (Fingerprint, Face Unlock) with PIN fallback and FragmentActivity lifecycle handling.
- Encrypted Backup & Restore: Export/Import JSON backup snapshots via Storage Access Framework (SAF).
- CSV Spreadsheet Reports Exporter.