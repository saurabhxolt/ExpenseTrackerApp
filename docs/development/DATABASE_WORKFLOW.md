# 🗄️ Database Workflow & Schema Migration Guide

This document governs database schema evolution, Room migrations, and SQLCipher encrypted backups.

---

## 🏗️ 1. Room Schema Rules

1. **Schema Versioning**: Any modification to an entity class (`@Entity`), field, or index requires incrementing `version` in [ExpenseDatabase.kt](file:///e:/ExpenseTrackerApp/app/src/main/java/com/expensetracker/app/core/database/ExpenseDatabase.kt).
2. **Schema Export**: Room schemas are automatically exported to `app/schemas/com.expensetracker.app.core.database.ExpenseDatabase/<version>.json`.
3. **Commit Integrity**: Always commit the newly generated Room JSON schema file alongside your entity changes.

---

## 🔄 2. Migration Standards

### Automated Migrations
For additive changes (adding a nullable column or a new table):
```kotlin
@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, BudgetEntity::class, SubscriptionEntity::class, MerchantCategoryMappingEntity::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 2, to = 3)
    ],
    exportSchema = true
)
```

### Manual Migrations
For complex alterations (column renaming, type changes, or data transformations):
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN referenceId TEXT DEFAULT NULL")
    }
}
```

### Testing Migrations
Always add a migration test case in `app/src/androidTest/java/.../MigrationTest.kt` using `MigrationTestHelper`.

---

## 🔐 3. SQLCipher Passphrase Management

1. **Hardware Key Sealing**: Master key is stored in AndroidKeyStore under alias `vault_expense_db_key`.
2. **SupportFactory**: Database is initialized using `SupportFactory(passphrase)` from `net.zetetic.database.sqlcipher`.
3. **Zero Plaintext Logs**: Never log the raw passphrase or pass it to standard string interpolations.

---

## 💾 4. Backup & Restore Schema Specification

### JSON Backup Envelope
Exported backups are encrypted JSON archives structured as:
```json
{
  "version": 1,
  "exportedAt": 1792882800000,
  "appVersion": "1.2.0",
  "data": {
    "transactions": [...],
    "categories": [...],
    "budgets": [...],
    "subscriptions": [...],
    "merchantMappings": [...]
  }
}
```

### CSV Spreadsheet Format
CSV exports use standard comma-delimited columns:
`Date,Type,Category,Merchant,Amount,Account,Reference`
