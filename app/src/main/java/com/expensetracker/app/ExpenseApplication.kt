package com.expensetracker.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.zetetic.database.sqlcipher.SQLiteDatabase

@HiltAndroidApp
class ExpenseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize SQLCipher library binaries
        SQLiteDatabase.loadLibs(this)
    }
}
