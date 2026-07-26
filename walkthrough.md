# Semantic NLP Financial Message Classifier & UI Layout Fixes - Walkthrough

## Summary of Refinements (Fixing 8 New Screenshots)

### 1. Initiated Refund Rejection (`RegexTransactionParser.kt`)
- **Screenshots 161107, 161111, 161115**: `"Dear user, refund of Rs 45.92 for your Zepto order UHISJBLBL99775 has been initiated. It should reflect in your account within 5-7 business days!"`
- **Behavior**: Filtered out by `INFORMATIONAL_STATEMENT_PATTERNS` (`has been initiated`, `refund.*initiated`).
- **Result**: Initiated refund alerts are **ignored / rejected**, preventing premature or triple-counting of uncredited refunds.

### 2. Bill Payment Receipts Classified as Expenses (`RegexTransactionParser.kt`)
- **Screenshot 161120**: `"Hi Saurabh Kishor Sonwal, we have received a payment of Rs. 347.16 for your Airtel Black ID 10101029041868..."`
  - **Behavior**: Categorized as `DEBIT` / Expense (Merchant: `Airtel Black`, Category: `Bills & Utilities`).
- **Screenshot 161125**: `"Hi Saurabh Kishor Sonwal, we have received a payment of Rs. 370.52 for your Airtel Wi-Fi ID 07214505992..."`
  - **Behavior**: Categorized as `DEBIT` / Expense (Merchant: `Airtel Wi-Fi`, Category: `Bills & Utilities`).
- **Result**: Correctly recorded as **Expenses**, **NEVER Income**.

### 3. Dialog Amount Text Vertical Wrapping UI Fix (`DashboardScreen.kt`)
- **Screenshots 161212, 161217, 161222**: Long merchant names (e.g. `SAURABH KISHOR SONWAL`, `Airtel Black ID 10101029041868`) were squeezing the amount text `+₹23224.00` / `+₹1500.00` to the far right, causing it to wrap vertically letter-by-letter down 9 lines.
- **Fix**: Applied `Modifier.weight(1f)` to the merchant title column and `maxLines = 1, softWrap = false` to `Text(amount)`, guaranteeing clean single-line amount rendering regardless of merchant name length.

---

## Verification Results

### Automated Unit Tests
- `RegexTransactionParserTest`: PASS (31 test cases executed).
- `TransactionDeduplicatorTest`: PASS.
- `gradlew test`: SUCCESSFUL.

### APK Package Output
- Debug APK compiled successfully: `app-debug.apk` (117 MB).
