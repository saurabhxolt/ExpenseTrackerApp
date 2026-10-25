package com.expensetracker.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.expensetracker.app.core.database.dao.BudgetDao
import com.expensetracker.app.core.database.dao.CategoryDao
import com.expensetracker.app.core.database.dao.SplitDao
import com.expensetracker.app.core.database.dao.SubscriptionDao
import com.expensetracker.app.core.database.dao.TransactionDao
import com.expensetracker.app.core.database.entity.BudgetEntity
import com.expensetracker.app.core.database.entity.CategoryEntity
import com.expensetracker.app.core.database.entity.ExpenseSplitShareEntity
import com.expensetracker.app.core.database.entity.GroupEntity
import com.expensetracker.app.core.database.entity.GroupMemberEntity
import com.expensetracker.app.core.database.entity.GroupSettlementEntity
import com.expensetracker.app.core.database.entity.SplitExpenseEntity
import com.expensetracker.app.core.database.entity.SubscriptionEntity
import com.expensetracker.app.core.database.entity.TransactionEntity
import net.sqlcipher.database.SupportFactory

import com.expensetracker.app.core.database.dao.MerchantCategoryMappingDao
import com.expensetracker.app.core.database.dao.SavingsGoalDao
import com.expensetracker.app.core.database.dao.CreditCardDao
import com.expensetracker.app.core.database.dao.RecurringIncomeDao
import com.expensetracker.app.core.database.dao.InsurancePolicyDao
import com.expensetracker.app.core.database.entity.MerchantCategoryMappingEntity
import com.expensetracker.app.core.database.entity.SavingsGoalEntity
import com.expensetracker.app.core.database.entity.CreditCardEntity
import com.expensetracker.app.core.database.entity.RecurringIncomeEntity
import com.expensetracker.app.core.database.entity.InsurancePolicyEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        SplitExpenseEntity::class,
        ExpenseSplitShareEntity::class,
        GroupSettlementEntity::class,
        SubscriptionEntity::class,
        MerchantCategoryMappingEntity::class,
        SavingsGoalEntity::class,
        CreditCardEntity::class,
        RecurringIncomeEntity::class,
        InsurancePolicyEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun splitDao(): SplitDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun merchantCategoryMappingDao(): MerchantCategoryMappingDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun recurringIncomeDao(): RecurringIncomeDao
    abstract fun insurancePolicyDao(): InsurancePolicyDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_timestamp` ON `transactions` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type_timestamp` ON `transactions` (`type`, `timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_category` ON `transactions` (`category`)")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `taxDeductibleSection` TEXT DEFAULT NULL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetAmount` REAL NOT NULL,
                        `currentAmount` REAL NOT NULL,
                        `targetDate` INTEGER NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `emoji` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `credit_cards` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cardName` TEXT NOT NULL,
                        `last4Digits` TEXT NOT NULL,
                        `totalLimit` REAL NOT NULL,
                        `statementDayOfMonth` INTEGER NOT NULL,
                        `dueDayOfMonth` INTEGER NOT NULL,
                        `cardColorHex` INTEGER NOT NULL,
                        `bankName` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recurring_incomes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `expectedDayOfMonth` INTEGER NOT NULL,
                        `nextDueDateMs` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `settlementMode` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `isSalary` INTEGER NOT NULL,
                        `lastReceivedDateMs` INTEGER,
                        `matchedTransactionId` INTEGER,
                        `payerPpiOrBank` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `insurance_policies` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `policyName` TEXT NOT NULL,
                        `policyType` TEXT NOT NULL,
                        `policyNumber` TEXT NOT NULL,
                        `insurerName` TEXT NOT NULL,
                        `premiumAmount` REAL NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `renewalDueDateMs` INTEGER NOT NULL,
                        `taxSection` TEXT NOT NULL,
                        `linkedSavingsGoalId` INTEGER,
                        `isActive` INTEGER NOT NULL,
                        `lastPaidDateMs` INTEGER,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context, passphrase: ByteArray): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_tracker_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
