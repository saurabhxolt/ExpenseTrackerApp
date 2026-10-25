# 🌟 Master Feature Catalog — Expense Tracker App

> **Governance Rule**: 100% of all user-facing capabilities across each persona/subsystem must be documented here. Any time a feature is created, modified, or deprecated, `FEATURES.md` MUST be updated in the same commit.

---

## 👤 Persona 1: Personal Finance End-User

### 1. Dashboard & Cashflow Cockpit
* **Net Cashflow Hero Card**: Real-time display of `Income - Expenses` with color-coded status (emerald green for positive, ruby crimson for negative).
* **Period-Aware Horizon Filter**: Toggle between Today, This Week, This Month, Last Month, and Custom Date Ranges.
* **Financial Metrics Bar**: Displays granular summaries for Total Income (₹), Total Expenses (₹), Investments (₹), and calculated Savings Rate (%).
* **Average Daily Spend**: Automatic calculation of burn rate across the selected timeframe.
* **Safe-to-Spend Today Radar**: Real-time daily discretionary allowance calculation pacing remaining monthly budgets against remaining calendar days in the month, dynamically deducting committed upcoming subscriptions/bills with status badges (Abundant, Balanced, Tight, Exhausted).
* **Recent Activity Feed**: Chronological transaction stream with 44dp vibrant category avatars, formatted timestamps, and container pill badges.

### 2. Transaction Ledger & In-Place Management
* **Dual-Channel Historical Log**: Unified chronological feed of all parsed SMS, push notifications, and manual entries.
* **Dynamic Search & Filtering**: Instant search by merchant name, reference note, amount, or date.
* **Bank / Account Chip Bar**: Instant filtering by financial institution (e.g., HDFC, SBI, ICICI, Axis, Kotak, BOB, CRED, Paytm).
* **In-Place Category Override**: 1-tap category editor sheet allowing user reclassification; automatically triggers merchant memory learning.
* **Manual Transaction Entry**: Floating Action Button modal to manually record cash expenses or offline transfers with category assignment.

### 3. Budgets & Category Spending Caps
* **Monthly Spending Thresholds**: Define custom monthly spending limits per category (e.g. ₹10,000 for Food & Dining), strictly scoped to the active calendar month.
* **Dynamic Category Loading & Safe Upsert**: Pulls real categories dynamically from `CategoryRepository` (including custom user categories) rather than hardcoded lists, with safe upsert logic preventing duplicate category budget entries.
* **Dynamic Visual Progress Indicators**: Animated progress bars color-coded by utilization:
  * 🟢 Green: < 80% of limit
  * 🟡 Amber: 80% – 99% of limit (warning zone)
  * 🔴 Red: ≥ 100% of limit (over-budget alert)
* **Collision-Free Header Actions**: Delete/edit actions pinned to card headers to avoid Floating Action Button interference.

### 4. Home Screen Live Cashflow Widget
* **Glanceable Financial Stats**: Live Android Home Screen App Widget rendering current month net cashflow and today's total spend.
* **Automatic Live Updates Across All Writes**: Instant widget synchronization triggered across all transaction operations (SMS/notification ingestion, manual additions, category edits, and deletions) via repository hook.
* **Direct App Launch**: Interactive tap anywhere on the widget immediately brings the main app to the foreground.

### 4. Financial Analytics & Deep Insights
* **Category Spending Leaderboard**: Ranked bar chart showing highest spending categories for the active period.
* **Spending Distribution Breakdown**: Percentage-based visual share analysis per category.
* **Income vs. Outflow Comparative Cards**: Side-by-side comparative cards with cashflow ratio analytics.

### 5. Recurring Mandates & Subscription Tracker
* **Brand-Normalized Detection**: Automatically identifies recurring mandate patterns (SIPs, OTT streaming, utility bills).
* **Due Date Calculation**: Automatic monthly/yearly cycle advancement.
* **1-Tap "Mark Paid" Action**: Manual override button to advance cycle dates if auto-deduction occurs via non-SMS channel.
* **Late-Message Deduplication**: Prevents duplicate subscription creation when multiple notifications arrive for the same cycle.
* **Stealth Inflation & Subscription Price Creep Detector**: Evaluates historical debits for repeat merchants and active subscriptions, flagging price jumps (>3% and >= ₹5) with visual warning alerts and percentage increase indicators.

### 6. Visual Sinking Funds & Savings Goals (Interactive "Jars")
* **Interactive Sinking Fund Jars**: Create visual savings goals with custom target amounts, emojis, colors, and deadline targets (Emergency Fund, Vacation, Down Payment, Gadgets).
* **Dynamic Monthly Savings Pace**: Automatically calculates exact monthly savings pace required to hit the target before deadline.
* **1-Tap Quick Deposit & Withdraw**: Fast deposit/withdrawal actions with quick rupee chips (`+₹500`, `+₹1,000`, `+₹2,000`, `+₹5,000`) and auto-completion when the target is achieved.
* **Dashboard Live Overview Card**: Glanceable visual card on Dashboard displaying total saved across all jars, overall progress bar, and quick-tap jar chips.
* **One-Tap Starter Templates**: 4 built-in starter templates (`🛡️ Emergency Fund`, `🏖️ Vacation Trip`, `💻 Gadget & Tech`, `🚗 Vehicle Down Payment`) for instant jar creation with pre-calibrated targets and emojis.
* **Dual Ledger Synchronization**: Optional ledger-linking toggle (`recordInLedger`) recording deposits as `INVESTMENT` transactions and withdrawals as `CREDIT` transactions into the main financial ledger.

### 7. Indian Tax Season Deductions Organizer (80C / 80D / HRA)
* **Automated Exemption Inference**: Identifies tax-deductible transactions under Sections 80C (PPF, ELSS, LIC, tuition fees), 80D (health insurance/mediclaim), Section 10(13A) (HRA rent payments), 80CCD (NPS), 80E, and 80G.
* **Statutory Limit Tracking**: Live progress bars against official ceilings (₹1,50,000 for 80C; ₹25,000 for 80D) and estimated tax savings computation at ~20% tax slab.
* **1-Tap CA / ITR Report Exporter**: Exports formatted WhatsApp and text summaries detailing eligible debits by section for Chartered Accountant filing.

### 8. Itemized Restaurant & Group Bill Splitter (Proportional Tax & Tip)
* **Proportional Tax & Tip Distribution**: Eliminates awkward dining math by allocating individual and shared dishes, then proportionally dividing GST, VAT, service charges, and tips based on each diner's exact subtotal ratio.
* **1-Click Ledger Import & WhatsApp Summary**: Converts computed itemized shares directly into an active group expense or copies a clean, formatted breakdown to the clipboard for group chats.

### 9. Credit Card Grace Period & 30% Utilization Radar
* **30% CIBIL Utilization Guard**: Tracks active credit limits against current statement cycle debit spend, warning when utilization crosses 30% or 50% to shield credit scores from bureau penalties.
* **Interest-Free Grace Period Countdown**: Computes dynamic statement cycles and payment deadlines, displaying exact remaining grace days before interest accrues.
* **Auto-Discovery of Unregistered Cards**: Detects credit card mentions in incoming transactions and prompts 1-tap onboarding to track limits.
* **Multi-Attribute Cycle Transaction Tracking**: Accurately attributes transactions to specific cards using cleaned account digits, note tags, SMS raw text masked patterns (`**4321`, `xx4321`), merchants, and bank identifiers.
* **Expandable Cycle Transactions Drawer**: Dedicated collapsible drawer on every card showing all attributed transactions with amounts, merchants, dates, and empty state troubleshooting guides.
* **Educational Issuer Setup**: Card setup dialog featuring 11 bank issuer chips (`HDFC`, `SBI`, `ICICI`, `Axis`, `Kotak`, `OneCard`, `BOB`, `Amex`, `RBL`, `IndusInd`, `Federal`), statement date vs due date educational helper texts, and offline security card.
* **Linked Card Transaction Tagging**: 1-tap card selector in Dashboard Add Transaction dialog enabling manual expenses to be billed directly against registered credit cards.

### 10. Quick Settings Tile & Instant Cash Logging (<3s)
* **Android Quick Settings Tile (`CashQuickLogTileService`)**: Launch rapid cash expense logging straight from Android notification shade / lockscreen.
* **1-Tap Fast Presets**: Rapidly logs common cash outlays (`☕ Chai ₹20`, `🛺 Auto ₹50`, `🥪 Snacks ₹100`, `🍛 Lunch ₹150`, `🛒 Groceries ₹500`, `⛽ Fuel ₹500`) with zero app navigation.

### 11. Air-Gapped Offline QR Group Sync (Zero-Internet Splitwise)
* **100% Serverless Peer-to-Peer Sync**: Compresses entire group ledgers (members, expenses, proportional shares, and debt settlements) into high-density GZIP-compressed QR codes via ZXing (`GroupQrSyncEngine`).
* **Live CameraX Scanner & Screenshot Importer**: Scan a friend's phone screen directly, import a QR screenshot from device gallery, or paste a raw sync code.
* **Smart Identity Perspective Resolution**: Symmetrically remaps "You" between sender and recipient phones so balances and debt minimization are preserved down to the paisa.
* **Idempotent Merge Engine**: Prevents duplicate insertions when re-scanning active groups, automatically creating missing members and syncing new expenses.

### 12. 100% On-Device Receipt & Invoice OCR Scanner
* **Offline Optical Character Recognition**: Leverages Google ML Kit Text Recognition with bundled offline model (`com.google.mlkit:text-recognition:16.0.0`) packaged within the APK, eliminating Google Play Services internet model download requirements.
* **Sensor & EXIF Upright Rotation Correction**: Automatically compensates for Android camera landscape sensor mounting (`imageProxy.imageInfo.rotationDegrees`) and image gallery EXIF orientation (`android.media.ExifInterface`), preventing sideways OCR decoding errors.
* **Multi-Line Proximity Total & Noise Rejection**: Identifies totals on adjacent lines, rejects item count / quantity noise ("Total items: 3"), and filters out year/pin code false amounts.
* **Interactive Detected Amounts Chips**: Renders all candidate amounts found on the receipt as 1-tap selectable chips, allowing instant correction if ML Kit picked a subtotal.
* **Paid With / Linked Card Selector**: Allows scanned receipts to be directly attributed to a registered credit card, automatically updating credit card radar utilization.
* **Transparent Raw Text Drawer**: Expandable bottom sheet displaying full raw extracted OCR text for verification and debugging.

### 13. Recurring Inflow Engine & Salary Day Radar
* **Scheduled Anticipated Inflows**: Configure recurring inflows across common Indian income types (Monthly Salary, Rental Income, Dividends, LIC Annuity Payouts, Freelance Retainers) with cycle frequencies (Monthly, Quarterly, Yearly, Bi-Weekly).
* **Zero Phantom Income Reconciliation**: Multi-attribute matching engine (`RecurringInflowReconciler`) automatically links real incoming bank `CREDIT` transactions with expected recurring streams by amount tolerance (±5%), merchant/employer keywords, and settlement mode, advancing the cycle date without generating duplicate ledger entries.
* **Dashboard Scheduled Inflows Horizon Card**: Displays total expected monthly inflows vs realized inflows this cycle, progress gauge, days until salary credit, and 1-tap "Mark Received" quick action.
* **Peak Liquidity Radar**: Synchronizes with bank salary credit alerts to calculate monthly cashflow runway before discretionary spending starts.

### 14. Annual Insurance & Policy Vault with Salary Day Smart Stash
* **All-in-One Policy Vault**: Centrally track Health Insurance (Mediclaim 80D), Term Life Insurance (80C), Car Insurance, Bike Insurance, and LIC / Endowment policies with insurer details, policy numbers, and renewal due dates.
* **1-Tap Sinking Jar Auto-Creation**: Seamlessly creates a dedicated visual Sinking Fund Jar for each policy (e.g. ₹24,000 Car Insurance Jar) with pre-calibrated target amounts, custom color, and emoji.
* **Dynamic Monthly Provisioning Pace Calculator**: Automatically divides remaining deficit by months until renewal, calculating exact monthly stash required to prevent annual lump-sum premium shock.
* **Salary Day High-Priority Alert**: Automatically triggers a local high-priority alert (`SalaryProvisioningNotificationHelper`) when salary is credited, prompting the user to allocate monthly insurance portions immediately into linked jars.
* **1-Tap Dashboard Stash Dialog**: Interactive deposit modal with optional ledger sync (`recordInLedger`) to record stashes as `INVESTMENT` transactions in the main financial ledger.

---

## 🤖 Persona 2: Autonomous Ingestion Daemon

### 1. Dual-Channel Ingestion Pipeline
* **Broadcast SMS Ingestion (`SmsReceiver`)**: Catches incoming bank SMS PDUs in real time.
* **Push Notification Listener (`ExpenseNotificationListenerService`)**: Captures status bar push notifications from banking and UPI apps (PhonePe, GPay, Paytm, CRED).
* **Inbox History Scanner**: Scans inbox starting from the 1st of 3 months prior on first install (e.g., Oct -> July 1st), followed by incremental delta scans and automatic home widget updates.

### 2. Intelligent NLP & Regex Parser
* **Multi-Bank Pattern Suite**: Pre-configured parsing patterns for HDFC, SBI, ICICI, Axis, Kotak, BOBCARD, CRED, and major UPI handles.
* **Word Boundary Protection (`\b`)**: Strict boundary enforcement on short matchers (`\bcred\b`, `\bcheq\b`, `\bupi\b`) preventing false collisions with "to credit".
* **Zero False Positive Rejection**: Discards promotional offers, loan approvals, EMI schemes, OTPs, and balance inquiry alerts.
* **Credit Card Bill & Transfer Identification Engine**: Accurately recognizes card bill settlements across issuers (HDFC, SBI, ICICI, Axis, Kotak, BOBCARD, OneCard, Amex) and payment rails (CRED, CheQ, BillDesk, BBPS, UPI, direct net banking auto-debit), classifying them as `TRANSFER` under `Bills & Utilities` with dedicated UI display (`↔ ₹`), badges, and self-healing historical deduplication repair.

### 3. Repeat Merchant Preference Memory
* **Merchant-to-Category Learning**: Remembers user manual category corrections for specific merchants.
* **Instant Fallback Hierarchy**: User Merchant Override > Regex Heuristics > Keyword Classification > General Expense.

---

## 🔒 Persona 3: Privacy & Security Guardian

### 1. Absolute Offline Perimeter
* **Zero Internet Permissions**: `android.permission.INTERNET` is completely absent from `AndroidManifest.xml`.
* **Zero External Telemetry**: No third-party SDKs, tracking pixels, or remote analytics.

### 2. Hardware-Backed Encryption
* **SQLCipher AES-256**: Room database encrypted at rest using SQLCipher.
* **Android Keystore Passphrase Management**: Encryption key derived and securely sealed inside the Android hardware Keystore.

### 3. Biometric Gatekeeper
* **BiometricPrompt Authentication**: Fingerprint and Face Unlock support on app launch with single-prompt debouncing to eliminate duplicate prompt race conditions during cold start.
* **Cryptographic Fallback**: Seamless fallback to device PIN, Pattern, or Password.
* **30-Second Background Grace Window**: Automatic re-lock tracking ensures sensitive financial data remains protected when the app is backgrounded.

### 4. Peek Privacy Mode & Shake-to-Hide
* **1-Tap Peek Privacy Toggle**: Top app bar eye toggle (`Visibility` / `VisibilityOff`) with `CompositionLocal` provider (`LocalPrivacyMode`) masking all sensitive currency values across Dashboard, Ledger, and Budgets to `₹•••••`.
* **Shake-to-Hide Accelerometer Gesture**: Device accelerometer listener detecting deliberate shake motions (2.6g threshold with 1200ms debounce and haptic feedback) to instantly mask or reveal financial figures.
* **Configurable Privacy Preferences**: Settings toggles for "Shake to Mask Amounts" and "Default to Masked Privacy" on launch.

---

## 💾 Persona 4: System Maintenance & Data Pipeline

### 1. Storage Access Framework (SAF) Backup & Export
* **Disaster-Recovery JSON Backup (Schema v3)**: Full snapshot export and restore via native Android SAF file picker, encompassing transactions, budgets (with `period`), categories (with `isCustom` and `emoji`), subscriptions (with `autoMatched` and `lastPaidDateMs`), merchant preference mappings, Splitwise P2P entities (groups, members, split expenses, shares, settlements), credit cards, savings jars, recurring incomes, and annual insurance policies.
* **Safe Name-Based Upsert on Restore**: Restores categories and budgets idempotently by name, preventing duplicate insertion crashes and preserving user-defined emojis.
* **Verified Restore Callbacks & Widget Auto-Sync**: Toast notifications are bound strictly to verified restore completion, automatically synchronizing widget balances post-import.
* **CSV & Summary Text Exporter**: Generates accounting-ready CSV files and formatted summary text reports with dedicated symbols for transfers and investments.
* **Automated Local Rolling Backups (`ScheduledBackupWorker`)**: Background WorkManager periodic worker performing weekly encrypted full-database JSON backups to the local sandbox, retaining the latest 5 rolling snapshots and auto-purging older ones without cloud or internet dependencies.
* **1-Tap Snapshot Restoration**: Settings interface provides on-demand "Backup Now" execution and an interactive "Restore Local Auto-Backup" dialog displaying snapshot timestamps and sizes for instant recovery.

### 2. Background Automation
* **Smart Bill Reminder Worker (`BillReminderWorker`)**: Database-aware WorkManager worker evaluating active subscriptions due in 48 hours and alerting users with customized bill details.
* **Contextual Daily Review Worker (`DailyReviewWorker`)**: WorkManager periodic evening notification reminding users to inspect day's expenses, dynamically tailored with today's actual spend amount.
* **Custom Time Selection**: Full 24-hour/12-hour AM/PM `TimePickerDialog` for reminder scheduling.
