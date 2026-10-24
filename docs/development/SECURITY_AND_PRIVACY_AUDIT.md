# 🔒 Security & Privacy Audit Playbook

The **Expense Tracker App** is built upon an unyielding commitment to user privacy and local-only data ownership. This document defines the privacy guarantees and mandatory security audits required for every release.

---

## 🚫 1. Absolute Offline Validation (Zero-Network Audit)

### Rule: No Internet Permission
Under no circumstances may `android.permission.INTERNET` or `android.permission.ACCESS_NETWORK_STATE` be added to `AndroidManifest.xml`.

### Automated Verification Command
Run the following PowerShell check before every release build:
```powershell
$manifest = Get-Content app\src\main\AndroidManifest.xml -Raw
if ($manifest -match "android.permission.INTERNET") {
    Write-Error "CRITICAL SECURITY VIOLATION: Internet permission detected in AndroidManifest.xml!"
    exit 1
} else {
    Write-Host "✅ Zero-Network Audit Passed: App has no internet permission."
}
```

---

## 🛡️ 2. Encryption at Rest Audit (SQLCipher AES-256)

1. **Verify Binary Encryption**:
   - Pull database file using ADB from rooted emulator:
     ```bash
     adb pull /data/data/com.expensetracker.app/databases/expense_tracker.db ./test_db.sqlite
     ```
   - Attempt to open with standard SQLite CLI:
     ```bash
     sqlite3 ./test_db.sqlite "SELECT * FROM transactions;"
     ```
   - **Expected Result**: `Error: file is not a database` (verifying 100% ciphertext encryption).
2. **Key Storage**:
   - Master key must be generated using `KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")`.
   - Never write keys to SharedPreferences or disk unencrypted.

---

## 🧼 3. PII Redaction & Sanitization

1. **Account Number Masking**: Account numbers in transaction bodies must display only trailing 4 digits (`..4321`).
2. **OTP Scrubbing**: If an OTP message accidentally enters the parsing pipeline, the noise rejection engine must drop it immediately without logging the code to logcat.
3. **Logcat Sanitization**: In release builds (`assembleRelease`), all logging must be stripped via Proguard/R8 rules:
   ```proguard
   -assumenosideeffects class android.util.Log {
       public static *** d(...);
       public static *** v(...);
       public static *** i(...);
   }
   ```

---

## 📂 4. Storage Access Framework (SAF) Sandboxing

- Do NOT request `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE`.
- File exports and imports must exclusively leverage Android SAF document pickers (`ActivityResultContracts.CreateDocument` and `ActivityResultContracts.OpenDocument`).
- Temporary cache files created during export must be deleted immediately after writing to the user-selected URI.
