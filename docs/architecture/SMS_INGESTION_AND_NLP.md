# 📨 SMS Ingestion & NLP Architecture

> **Rule**: Keep under 80 lines. Specialized skill: [.agents/skills/sms_transaction_parser/SKILL.md](file:///e:/ExpenseTrackerApp/.agents/skills/sms_transaction_parser/SKILL.md).

## 1. Dual-Channel Ingestion Pipeline
- **SMS Receiver (`SmsReceiver.kt`)**: Intercepts `android.provider.Telephony.SMS_RECEIVED`. Extracts sender address and SMS PDU body.
- **Notification Service (`ExpenseNotificationListenerService.kt`)**: Listens for push notifications from banking and UPI apps (PhonePe, GPay, Paytm).
- **Inbox Scanner (`SmsScanner.kt`)**: Performs first-of-month and incremental inbox delta scans.

## 2. Noise Filter & Exclusion Rules (Zero False Positives)
- Pre-filtering drops non-financial messages before regex execution:
  - OTPs, login verification codes, 2FA tokens.
  - Promotional loans, pre-approved credit limits, insurance marketing.
  - Account balance inquiry notices, credit card payment due reminders.
- **Rule**: If an SMS contains no balance debit/credit execution, it MUST NOT become a transaction.

## 3. Regular Expression Engine & Word Boundaries
- Amount extraction isolates spend amounts; ignores embedded available credit lines or current outstanding sums.
- **Word Boundary Protection**: Short service matchers (`cred`, `cheq`, `upi`) MUST use `\b` word boundaries (e.g., `\bcred\b`) to prevent collisions with phrases like "credited to".
- Multi-Bank Patterns: Tailored patterns for HDFC, SBI, ICICI, Axis, Kotak, BOB, CRED Club, and UPI handles.

## 4. Merchant Category Memory & Preference Hierarchy
- Classification resolution order:
  1. **User Merchant Memory**: Exact match in `merchant_category_mappings` table.
  2. **Regex Substring Rules**: Pattern matches for known merchants (e.g. Swiggy -> Food).
  3. **Keyword Heuristics**: Category keyword dictionaries.
  4. **Fallback**: `Other Expenses`.
