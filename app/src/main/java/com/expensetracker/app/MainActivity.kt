package com.expensetracker.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.expensetracker.app.core.ui.theme.ExpenseTrackerTheme
import com.expensetracker.app.core.ui.theme.PrimaryBlue
import com.expensetracker.app.features.main.MainScreen
import com.expensetracker.app.core.security.PrivacyModeManager
import com.expensetracker.app.core.security.ShakeDetector
import com.expensetracker.app.features.security.BiometricLockManager
import com.expensetracker.app.features.splash.SplashScreen
import com.expensetracker.app.ingestion.worker.BillReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.widget.Toast

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var privacyModeManager: PrivacyModeManager

    private var isUnlocked by mutableStateOf(false)
    private var isAuthenticating = false
    private var lastBackgroundTimestamp: Long = 0L
    private val lockGracePeriodMs: Long = 30_000L // 30 seconds
    private var shakeDetector: ShakeDetector? = null

    private var pendingQuickCashLog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (intent?.action == com.expensetracker.app.features.quicklog.CashQuickLogTileService.ACTION_QUICK_LOG_CASH) {
            pendingQuickCashLog = true
        }

        // Enable Fullscreen Layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        checkAppLockState()

        shakeDetector = ShakeDetector(this) {
            if (privacyModeManager.isShakeToHideEnabled() && isUnlocked) {
                window.decorView.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                privacyModeManager.toggle()
                val status = if (privacyModeManager.isPrivacyMode.value) "Peek Privacy: Active 🛡️" else "Peek Privacy: Revealed 👁️"
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
            }
        }

        // Schedule periodic 24-hour Bill & EMI Due Date Reminder Worker
        BillReminderWorker.schedulePeriodicReminders(this)

        // Schedule daily end-of-day expense review reminder worker (default 9:00 PM)
        com.expensetracker.app.ingestion.worker.DailyReviewWorker.scheduleConfiguredReview(this)

        // Schedule periodic automated local rolling backup worker (weekly)
        com.expensetracker.app.ingestion.worker.ScheduledBackupWorker.scheduleConfiguredBackup(this)

        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen(
                            onSplashFinished = {
                                showSplash = false
                            }
                        )
                    } else {
                        if (isUnlocked) {
                            MainScreen(
                                initialQuickCashLog = pendingQuickCashLog,
                                onQuickCashConsumed = { pendingQuickCashLog = false }
                            )
                        } else {
                            LockedScreen(
                                onUnlockClick = { triggerBiometricUnlock() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent?.action == com.expensetracker.app.features.quicklog.CashQuickLogTileService.ACTION_QUICK_LOG_CASH) {
            pendingQuickCashLog = true
        }
    }

    override fun onStop() {
        super.onStop()
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        shakeDetector?.start()
        if (BiometricLockManager.isBiometricLockEnabled(this)) {
            val now = System.currentTimeMillis()
            if (lastBackgroundTimestamp > 0 && (now - lastBackgroundTimestamp) > lockGracePeriodMs) {
                isUnlocked = false
            }
            if (!isUnlocked && !isAuthenticating) {
                triggerBiometricUnlock()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        shakeDetector?.stop()
    }

    private fun checkAppLockState() {
        isUnlocked = !BiometricLockManager.isBiometricLockEnabled(this)
    }

    private fun triggerBiometricUnlock() {
        if (isAuthenticating) return
        if (BiometricLockManager.canAuthenticate(this)) {
            isAuthenticating = true
            BiometricLockManager.promptBiometric(
                activity = this,
                onSuccess = {
                    isAuthenticating = false
                    isUnlocked = true
                },
                onError = {
                    isAuthenticating = false
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
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF111E38))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_vault_logo),
                contentDescription = "VaultExpense Logo",
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "VaultExpense Locked 🔒",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Private. Smart. Offline Expense Vault.",
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryBlue,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Biometric authentication is required to access your financial data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
