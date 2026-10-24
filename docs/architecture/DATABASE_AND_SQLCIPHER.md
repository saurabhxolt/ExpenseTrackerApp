# 🗄️ Database & SQLCipher Architecture

> **Rule**: Keep under 80 lines. Details in [ExpenseDatabase.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/core/database/ExpenseDatabase.kt).

## 1. Room Database & Schema Entities
- **Entities**:
  - `TransactionEntity`: Primary transaction ledger (amount, merchant, category, type, timestamp, account).
  - `CategoryEntity`: Master categories with emoji avatars and theme color hexes.
  - `BudgetEntity`: Monthly spending caps per category name.
  - `SubscriptionEntity`: Recurring mandate tracker with cycle frequency and next due date.
  - `MerchantCategoryMappingEntity`: Learned user preferences for merchant categorization.
- Reactive Access: DAOs return Kotlin Coroutines `Flow<List<T>>` for real-time Compose updates.

## 2. SQLCipher AES-256 Encryption
- Database file is encrypted on disk using `net.zetetic:android-database-sqlcipher`.
- Passphrase generation:
  - 256-bit cryptographic random key generated on first launch.
  - Key sealed and retrieved using Android hardware-backed KeyStore (`AndroidKeyStore`).
  - Database passphrase never written to plaintext disk, shared preferences, or logs.

## 3. Schema Migrations & Integrity
- Room `exportSchema = true` enabled in `app/build.gradle.kts`.
- Schema json files tracked in version control (`app/schemas/`).
- Automated unit test suite verifies non-destructive migrations (`MigrationTest`).
- Pre-populated default categories seeded via Room `RoomDatabase.Callback`.
