# 🧪 QA Testing Playbook & Test Matrix

This playbook outlines manual and automated verification procedures for the **Expense Tracker App**.

---

## 🏃 1. Automated Test Suite Execution

Run all local unit tests via Gradle:

```powershell
.\gradlew.bat test --info
```

Unit test classes of interest:
* `RegexTransactionParserTest.kt`: Validates extraction of amounts, merchants, categories, and noise rejection.
* `SubscriptionsTest.kt`: Validates brand normalization, cycle advancement, and mandate deduplication.

---

## 📱 2. ADB SMS Emulation (Local Testing)

Test live ingestion without a physical SIM card using the Android Emulator CLI:

```bash
# 1. HDFC Debit Card Outflow
adb emu sms send "VM-HDFCBK" "Sent Rs.450.00 from HDFC Bank A/C ending 4321 to SWIGGY on 25-OCT-26. Bal: Rs.12500."

# 2. SBI UPI Payment
adb emu sms send "VK-SBIUPI" "Dear UPI user A/C 9876 debited by Rs.120.00 on 25Oct26 transfer to Chaayos Ref No 628372."

# 3. ICICI Salary / Income Credit
adb emu sms send "AD-ICICIB" "Your A/C 5544 is credited with INR 75,000.00 on 25-Oct-26 by SALARY OCT. Avail Bal INR 1,12,000."

# 4. CRED Club Credit Card Settlement (Transfer)
adb emu sms send "JM-HDFCBK" "Sent Rs.2393.71 from HDFC Bank A/C ending 4321 to CRED Club on 25-OCT-26."
```

---

## 🚫 3. Zero-False-Positive Test Matrix (Must NOT be Ingested)

Send the following alerts to verify they are **rejected** by the engine:

| Message Type | Sample SMS Content | Expected Ingestion Result |
| :--- | :--- | :--- |
| **OTP / Auth** | `Your OTP for NetBanking login is 839201. Do not share this with anyone.` | ❌ Dropped (Ignored) |
| **Loan Offer** | `Congratulations! You are pre-approved for a personal loan of Rs 5,00,000 at 10.5% interest. Apply now.` | ❌ Dropped (Ignored) |
| **EMI Pitch** | `Convert your recent spend of Rs. 4,500 into easy 6-month EMIs of Rs. 780/mo. Click link.` | ❌ Dropped (Ignored) |
| **Balance Alert** | `Dear Customer, available balance in your A/C 1234 as of 25-Oct is Rs. 15,200.` | ❌ Dropped (Ignored) |
| **Credit Limit Notice** | `Your credit card ending 9988 has an available limit of Rs. 85,000 out of Rs. 1,00,000.` | ❌ Dropped (Ignored) |

---

## 🔒 4. Biometric Security & Fallback Scenarios

1. **Cold Launch**:
   - Open app -> BiometricPrompt appears immediately.
   - Cancel prompt -> Verifies app remains locked with option to use Device PIN.
   - Successful auth -> Unlocks directly to Dashboard.
2. **Background Grace Period**:
   - Minimize app for 10 seconds -> Return -> Remains unlocked if within grace period.
   - Minimize app for > 5 minutes -> Return -> Prompts for biometric unlock again.

---

## 💾 5. SAF Backup & CSV Exporter Verification

1. Go to **Settings** -> **Export Backup (JSON)** -> Pick storage folder via SAF.
2. Verify exported `.json` file contains AES-GCM encrypted payload and correct checksum.
3. Test **Restore Backup**: Wipe app data, launch, select exported file -> All transactions, budgets, and merchant memory restored accurately.
4. Test **Export CSV**: Open generated `.csv` in Excel / Google Sheets -> Confirm headers, negative signs on expenses, and dates format properly.
