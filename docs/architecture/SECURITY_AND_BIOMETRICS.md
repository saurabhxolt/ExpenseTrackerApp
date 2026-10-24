# 🛡️ Security & Biometrics Architecture

> **Rule**: Keep under 80 lines. Implementation in [BiometricLockManager.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/features/security/BiometricLockManager.kt).

## 1. Absolute Offline Perimeter
- **Zero Internet Permissions**: `android.permission.INTERNET` is strictly absent from `AndroidManifest.xml`.
- Zero network socket calls, zero HTTP/gRPC clients, zero third-party telemetry.
- All ingestion, parsing, database queries, and analytics calculations execute 100% on-device.

## 2. Hardware Keystore & SQLCipher Encryption
- Master key generated via Android `KeyStore` using AES/GCM/NoPadding (256-bit).
- Key requires hardware-backed StrongBox / TEE whenever available on device.
- SQLCipher encrypts database pages at rest, protecting SQLite files from root extraction or ADB backup pulls.

## 3. Biometric Gatekeeper & Fallback
- Uses AndroidX `BiometricPrompt` with `BIOMETRIC_STRONG` and `DEVICE_CREDENTIAL`.
- Supports Fingerprint, Face Unlock, and seamless fallback to system PIN/Pattern.
- Lock triggers on app cold launch and when returning from background after configurable grace period.

## 4. Storage Access Framework (SAF) Sandboxing
- Backups and CSV exports are written exclusively via user-selected URIs using `Intent.ACTION_CREATE_DOCUMENT`.
- No broad `READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE` permissions requested.
- Exported JSON backups are password-protected and signed with AES-GCM.
