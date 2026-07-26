# Semantic NLP Financial Message Classifier Engine - Walkthrough

## Overview of Implemented Intelligent Engine

We built a **Semantic NLP Financial Message Classifier Engine** (`RegexTransactionParser.kt`) that analyzes the true intent of every financial message received via SMS or Push Notifications.

---

## The 5 Semantic Message Categories & Behaviors

### 1. Informational Statement Alerts (Category 4 -> REJECTED)
- **Screenshot 154708**: `"ICICI Bank Credit Card XX9000 Statement is sent to sa*******lt@gmail.com. Total of Rs 2,408.71 or minimum of Rs 130.00 is due by 03-AUG-26."`
- **Behavior**: Filtered out by `INFORMATIONAL_STATEMENT_PATTERNS` (`statement is sent`, `minimum ... is due`).
- **Result**: **Null / Ignored** (no false Income created).

### 2. Promotional Loan / Disbursement Consent Alerts (Category 5 -> REJECTED)
- **Screenshot 154723**: `"Service Alert: Funds of INR 4,67,000.00 on YES BANK Credit Card ending 0570 are available and require consent to continue disbursement..."`
- **Behavior**: Filtered out by `PROMOTIONAL_OFFER_PATTERNS` (`funds ... are available`, `require consent`, `disbursement`).
- **Result**: **Null / Ignored** (no false Income created).

### 3. Payment Confirmation Receipts for Expenses (Category 1 -> DEBIT / EXPENSE)
- **Screenshot 154735**: `"Received Rs.14099/- against new registration fee vide receipt no KA51D26070003689 . MoRTH."`
  - **Behavior**: Categorized as `DEBIT` / Expense (Merchant: `MoRTH`, Category: `Government & Fees`).
- **Screenshot 154753**: `"You're covered! Hi Saurabh Kishor Sonwal, we have received payment of Rs 5080.0 for your bike insurance NA..."`
  - **Behavior**: Categorized as `DEBIT` / Expense (Merchant: `ACKO Insurance`, Category: `Insurance`).
- **Result**: Correctly recorded as **Expenses**, **NEVER Income**.

### 4. Timestamp Merchant Name Stripping (Category 2 -> Genuine Beneficiary Extraction)
- **Screenshots 154822, 154836, 154841**: `"HDFC Bank : NEFT money transfer Txn No HDFCH01108338271 for Rs INR 3,400.00 has been credited to ARCHIS KISHORRAO SONWAL on 07-07-2026 at 02:40:16"`
- **Behavior**: Strips `at 02:40:16` time strings before merchant matching; extracts beneficiary name `ARCHIS KISHORRAO SONWAL` from `credited to <Name>`.
- **Result**: Clean Merchant Name: **ARCHIS KISHORRAO SONWAL** (not `02:40:16`!).

---

## Verification Results

### Automated Unit Tests
- `RegexTransactionParserTest`: PASS (covers all 5 screenshot test cases).
- `TransactionDeduplicatorTest`: PASS.
- `gradlew test`: SUCCESSFUL.

### APK Package Output
- Debug APK compiled successfully: `app-debug.apk` (117 MB).
