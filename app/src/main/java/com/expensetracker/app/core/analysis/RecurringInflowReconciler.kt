package com.expensetracker.app.core.analysis

import com.expensetracker.app.core.database.entity.RecurringIncomeEntity
import com.expensetracker.app.core.database.entity.TransactionEntity
import java.util.Calendar
import java.util.regex.Pattern
import kotlin.math.abs

data class ReconciledIncomeMatch(
    val recurringIncome: RecurringIncomeEntity,
    val matchedTransaction: TransactionEntity,
    val confidence: Float,
    val isSalary: Boolean,
    val matchReason: String
)

object RecurringInflowReconciler {

    private val SALARY_KEYWORDS = Pattern.compile(
        """(?i)\b(salary|sal\s+cr|sal\s+credited|payroll|stipend|remuneration)\b"""
    )

    fun reconcile(
        transaction: TransactionEntity,
        activeIncomes: List<RecurringIncomeEntity>
    ): ReconciledIncomeMatch? {
        if (!transaction.type.equals("CREDIT", ignoreCase = true)) {
            return null
        }

        val enabledIncomes = activeIncomes.filter { it.isActive }
        if (enabledIncomes.isEmpty()) return null

        val merchantLower = transaction.merchant.lowercase()
        val rawLower = transaction.rawText.lowercase()
        val noteLower = transaction.note.lowercase()
        val combinedText = "$merchantLower $rawLower $noteLower"

        // 1. First priority: Check primary Salary income if transaction has salary signals
        val salaryIncome = enabledIncomes.firstOrNull { it.isSalary }
        if (salaryIncome != null) {
            val hasSalaryKeyword = SALARY_KEYWORDS.matcher(combinedText).find()
            val hasPayerMatch = salaryIncome.payerPpiOrBank.isNotBlank() &&
                    combinedText.contains(salaryIncome.payerPpiOrBank.lowercase())
            val amountRatio = if (salaryIncome.amount > 0) abs(transaction.amount - salaryIncome.amount) / salaryIncome.amount else 1.0

            if (hasSalaryKeyword || hasPayerMatch) {
                // If it explicitly says "salary", match with high confidence
                return ReconciledIncomeMatch(
                    recurringIncome = salaryIncome,
                    matchedTransaction = transaction,
                    confidence = if (amountRatio < 0.15) 0.95f else 0.85f,
                    isSalary = true,
                    matchReason = if (hasSalaryKeyword) "Salary keyword detected in bank alert" else "Employer identifier matched"
                )
            } else if (amountRatio < 0.05 && isNearExpectedDay(transaction.timestamp, salaryIncome.expectedDayOfMonth)) {
                // High amount match near salary day of month
                return ReconciledIncomeMatch(
                    recurringIncome = salaryIncome,
                    matchedTransaction = transaction,
                    confidence = 0.80f,
                    isSalary = true,
                    matchReason = "Amount and date aligned with expected monthly salary"
                )
            }
        }

        // 2. Second priority: Check other recurring incomes (Rent, Dividends, Retainers, Annuity)
        for (income in enabledIncomes.filter { !it.isSalary }) {
            val nameWords = income.name.lowercase().split("\\s+".toRegex()).filter { it.length > 2 }
            val hasNameMatch = nameWords.any { combinedText.contains(it) }
            val hasPayerMatch = income.payerPpiOrBank.isNotBlank() &&
                    combinedText.contains(income.payerPpiOrBank.lowercase())

            val amountDiff = abs(transaction.amount - income.amount)
            val isAmountExact = amountDiff < 1.0
            val isAmountClose = income.amount > 0 && (amountDiff / income.amount) < 0.05
            val isDateAligned = isNearExpectedDay(transaction.timestamp, income.expectedDayOfMonth)

            if (hasPayerMatch && (isAmountExact || isAmountClose)) {
                return ReconciledIncomeMatch(
                    recurringIncome = income,
                    matchedTransaction = transaction,
                    confidence = 0.92f,
                    isSalary = false,
                    matchReason = "Payer name and credit amount verified"
                )
            }

            if (hasNameMatch && (isAmountExact || isAmountClose)) {
                return ReconciledIncomeMatch(
                    recurringIncome = income,
                    matchedTransaction = transaction,
                    confidence = 0.88f,
                    isSalary = false,
                    matchReason = "Income source name matched with transaction"
                )
            }

            if (isAmountExact && isDateAligned) {
                return ReconciledIncomeMatch(
                    recurringIncome = income,
                    matchedTransaction = transaction,
                    confidence = 0.75f,
                    isSalary = false,
                    matchReason = "Exact amount credited on expected schedule date"
                )
            }
        }

        return null
    }

    private fun isNearExpectedDay(txTimestampMs: Long, expectedDay: Int): Boolean {
        if (expectedDay !in 1..31) return true
        val cal = Calendar.getInstance().apply { timeInMillis = txTimestampMs }
        val actualDay = cal.get(Calendar.DAY_OF_MONTH)
        val diff = abs(actualDay - expectedDay)
        return diff <= 5 || diff >= 26 // Handles month rollover (e.g. 31st vs 1st)
    }
}
