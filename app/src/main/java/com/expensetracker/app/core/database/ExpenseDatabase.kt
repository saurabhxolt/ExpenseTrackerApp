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
import com.expensetracker.app.core.database.entity.MerchantCategoryMappingEntity

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
        MerchantCategoryMappingEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun splitDao(): SplitDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun merchantCategoryMappingDao(): MerchantCategoryMappingDao

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

        fun getInstance(context: Context, passphrase: ByteArray): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactory(passphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    "expense_tracker_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
