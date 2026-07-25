package com.expensetracker.app.features.reports

import com.expensetracker.app.core.database.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportsExporter {

    fun generateCsvReport(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append("ID,Date,Merchant,Category,Type,Amount (INR),Note\n")

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        for (t in transactions) {
            val dateStr = dateFormat.format(Date(t.timestamp))
            val merchantEscaped = "\"${t.merchant.replace("\"", "\"\"")}\""
            val noteEscaped = "\"${t.note.replace("\"", "\"\"")}\""
            sb.append("${t.id},$dateStr,$merchantEscaped,${t.category},${t.type},${t.amount},$noteEscaped\n")
        }

        return sb.toString()
    }

    fun generateSummaryTextReport(
        transactions: List<TransactionEntity>,
        totalIncome: Double,
        totalExpenses: Double
    ): String {
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("EXPENSE TRACKER - MONTHLY REPORT\n")
        sb.append("Generated on: ").append(SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())).append("\n")
        sb.append("=========================================\n\n")

        sb.append("Total Income:   ₹").append(String.format("%.2f", totalIncome)).append("\n")
        sb.append("Total Expenses: ₹").append(String.format("%.2f", totalExpenses)).append("\n")
        sb.append("Net Balance:    ₹").append(String.format("%.2f", totalIncome - totalExpenses)).append("\n\n")

        sb.append("-----------------------------------------\n")
        sb.append("TRANSACTION HISTORY (").append(transactions.size).append(" Records)\n")
        sb.append("-----------------------------------------\n")

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        for (t in transactions) {
            val dateStr = dateFormat.format(Date(t.timestamp))
            val sign = if (t.type == "CREDIT") "+" else "-"
            sb.append("[$dateStr] ").append(t.merchant).append(" (").append(t.category).append("): ")
                .append(sign).append("₹").append(String.format("%.2f", t.amount)).append("\n")
        }

        return sb.toString()
    }
}
