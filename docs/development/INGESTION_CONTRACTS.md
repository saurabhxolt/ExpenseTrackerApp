# 📄 Ingestion Contracts & Data Models

Since the **Expense Tracker App** is an offline-first native application with zero remote REST APIs, our external integration contracts govern:
1. **SMS Message Payload Envelopes**
2. **Push Notification Status Bar Bundles**
3. **Internal Parsing Domain Models (`ParsedTransaction`)**

---

## 1. Domain Model Contract: `ParsedTransaction`

When raw SMS or push notifications are processed, they produce an immutable `ParsedTransaction` object:

```kotlin
data class ParsedTransaction(
    val amount: Double,              // Transaction amount in INR (always positive number)
    val merchant: String,            // Normalized merchant or payee name (e.g. "Swiggy", "CRED Club")
    val type: TransactionType,       // EXPENSE, INCOME, or TRANSFER
    val category: String,            // Assigned category (e.g. "Food & Dining", "Bills & Utilities")
    val timestamp: Long,             // Epoch millisecond timestamp of transaction
    val bankAccount: String?,        // Optional extracted account identifier (e.g. "HDFC ..4321")
    val rawBody: String,             // Original verbatim SMS or push text for debugging / auditing
    val referenceId: String? = null  // UPI RRN, UTR, or bank reference number if present
)
```

---

## 2. SMS PDU Envelope Contract

Broadcast receiver receives an `android.provider.Telephony.SMS_RECEIVED` intent with:

| Field | Type | Description | Example |
| :--- | :--- | :--- | :--- |
| `pdus` | `Array<ByteArray>` | Serialized PDU bytes | Standard GSM / 3GPP PDU |
| `format` | `String` | PDU protocol format | `"3gpp"` or `"3gpp2"` |
| `originatingAddress` | `String` | Sender SMS header | `"VM-HDFCBK"`, `"VK-SBIUPI"` |
| `messageBody` | `String` | Full plain text body | `"Sent Rs.450.00 to SWIGGY..."` |
| `timestampMillis` | `Long` | Message arrival timestamp | `1792882800000` |

---

## 3. Push Notification Envelope Contract

`ExpenseNotificationListenerService` captures `StatusBarNotification` events:

| Bundle Extra Key | Type | Description |
| :--- | :--- | :--- |
| `android.title` | `CharSequence?` | App title / sender headline (e.g. "PhonePe", "Google Pay") |
| `android.text` | `CharSequence?` | Body notification text containing transaction amount and recipient |
| `android.bigText` | `CharSequence?` | Expanded notification text if multiline |
| `notification.packageName` | `String` | Originating package (e.g. `com.phonepe.app`, `com.google.android.apps.nbu.paisa.user`) |

---

## 4. Merchant Preference Learning Contract

When a user modifies the category of a transaction in the UI:
1. The merchant string is normalized: trim whitespace, lowercase, remove punctuation.
2. A record is upserted into `merchant_category_mappings`:
   * `merchant_normalized`: Primary Key (`String`)
   * `category_name`: Target Category (`String`)
   * `updated_at`: Current Epoch Millis (`Long`)
3. Subsequent transactions matching this merchant instantly inherit this category override.
