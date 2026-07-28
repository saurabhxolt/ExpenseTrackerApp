# Proguard rules for VaultExpense
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class androidx.room.** { *; }
-keep class com.expensetracker.app.core.database.entity.** { *; }
-keep class com.expensetracker.app.core.promotions.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**
-keep class dagger.hilt.** { *; }
