package com.expensetracker.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.expensetracker.app.core.ui.theme.ExpenseTrackerTheme
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import com.expensetracker.app.core.ui.theme.TextPrimary
import com.expensetracker.app.core.ui.theme.TextSecondary
import com.expensetracker.app.features.main.MainScreen
import com.expensetracker.app.features.security.BiometricLockManager
import com.expensetracker.app.ingestion.worker.BillReminderWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private var isUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAppLockState()

        // Schedule periodic 24-hour Bill & EMI Due Date Reminder Worker
        BillReminderWorker.schedulePeriodicReminders(this)

        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isUnlocked) {
                        MainScreen()
                    } else {
                        LockedScreen(
                            onUnlockClick = { triggerBiometricUnlock() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (BiometricLockManager.isBiometricLockEnabled(this) && !isUnlocked) {
            triggerBiometricUnlock()
        }
    }

    private fun checkAppLockState() {
        if (!BiometricLockManager.isBiometricLockEnabled(this)) {
            isUnlocked = true
        } else {
            isUnlocked = false
            triggerBiometricUnlock()
        }
    }

    private fun triggerBiometricUnlock() {
        if (BiometricLockManager.canAuthenticate(this)) {
            BiometricLockManager.promptBiometric(
                activity = this,
                onSuccess = {
                    isUnlocked = true
                },
                onError = {
                    isUnlocked = false
                }
            )
        } else {
            // Fallback: If device has no biometrics set up, unlock
            isUnlocked = true
        }
    }
}

@Composable
fun LockedScreen(onUnlockClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "App Locked",
            tint = PrimaryBlue,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Expense Tracker Locked",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Biometric authentication is required to access your financial data.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onUnlockClick,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = CircleShape,
            modifier = Modifier.height(48.dp)
        ) {
            Text("Unlock App 🔒", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
