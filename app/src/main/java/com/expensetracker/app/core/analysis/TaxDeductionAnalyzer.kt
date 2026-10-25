package com.expensetracker.app.core.analysis

import com.expensetracker.app.core.database.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TaxDeductionAnalyzer {

    const val SECTION_80C = "80C"
    const val SECTION_80D = "80D"
    const val SECTION_10_13A = "10(13A)"
    const val SECTION_80CCD = "80CCD(1B)"
    const val SECTION_80E = "80E"
    const val SECTION_80G = "80G"

    val AVAILABLE_SECTIONS = listOf(
        SECTION_80C,
        SECTION_80D,
        SECTION_10_13A,
        SECTION_80CCD,
        SECTION_80E,
        SECTION_80G
    )

    fun getSectionDisplayName(section: String): String {
        return when (section) {
            SECTION_80C -> "Section 80C (PPF, ELSS, LIC, Tuition)"
            SECTION_80D -> "Section 80D (Health Insurance / Mediclaim)"
            SECTION_10_13A -> "Section 10(13A) (HRA / House Rent)"
            SECTION_80CCD -> "Section 80CCD (NPS Pension)"
            SECTION_80E -> "Section 80E (Education Loan Interest)"
            SECTION_80G -> "Section 80G (Charitable Donations)"
            else -> "Section $section"
        }
    }

    fun getSectionLimit(section: String): Double? {
        return when (section) {
            SECTION_80C -> 150000.0
            SECTION_80D -> 25000.0
            SECTION_80CCD -> 50000.0
            else -> null // No ceiling or based on actual expense
        }
    }

    /**
     * Determines whether a transaction qualifies under an Indian Income Tax exemption section.
     * Respects user-defined taxDeductibleSection first; otherwise infers via merchant & category heuristics.
     */
    fun inferSection(merchant: String, category: String, rawText: String): String? {
        val lowerText = "$merchant $category $rawText".lowercase(Locale.ROOT)

        // 1. Section 80D: Health Insurance / Mediclaim
        if (lowerText.contains("health insurance") || lowerText.contains("mediclaim") ||
            lowerText.contains("star health") || lowerText.contains("care health") ||
            lowerText.contains("niva bupa") || lowerText.contains("hdfc ergo health") ||
            lowerText.contains("max bupa") || lowerText.contains("icici lombard health") ||
            (lowerText.contains("insurance") && lowerText.contains("health"))
        ) {
            return SECTION_80D
        }

        // 2. Section 10(13A): House Rent Allowance (HRA)
        if (category.equals("Rent", ignoreCase = true) ||
            category.equals("House Rent", ignoreCase = true) ||
            lowerText.contains("house rent") || lowerText.contains("flat rent") ||
            lowerText.contains("society maintenance") || lowerText.contains("rent for ") ||
            lowerText.contains("landlord")
        ) {
            return SECTION_10_13A
        }

        // 3. Section 80CCD(1B): National Pension Scheme (NPS)
        if (lowerText.contains("nps") || lowerText.contains("national pension") ||
            lowerText.contains("cra nsdl") || lowerText.contains("e-nps")
        ) {
            return SECTION_80CCD
        }

        // 4. Section 80E: Higher Education Loan Interest
        if ((lowerText.contains("education loan") || lowerText.contains("student loan")) &&
            (lowerText.contains("interest") || lowerText.contains("emi"))
        ) {
            return SECTION_80E
        }

        // 5. Section 80G: Donations & Relief Funds
        if (lowerText.contains("donation") || lowerText.contains("pm cares") ||
            lowerText.contains("cry child") || lowerText.contains("unicef") ||
            lowerText.contains("relief fund") || lowerText.contains("akshaya patra") ||
            lowerText.contains("charity")
        ) {
            return SECTION_80G
        }

        // 6. Section 80C: PPF, ELSS, Life Insurance, EPF, Tuition
        if (lowerText.contains("elss") || lowerText.contains("ppf") ||
            lowerText.contains("public provident") || lowerText.contains("lic of india") ||
            lowerText.contains("life insurance") || lowerText.contains("term insurance") ||
            lowerText.contains("sukanya samriddhi") || lowerText.contains("tuition fee") ||
            lowerText.contains("school fee") || lowerText.contains("tax saver") ||
            lowerText.contains("tax saving") || lowerText.contains("max life") ||
            lowerText.contains("hdfc life") || lowerText.contains("sbi life") ||
            lowerText.contains("icici pru")
        ) {
            return SECTION_80C
        }

        return null
    }

    /**
     * Calculates Financial Year boundaries in India (April 1 to March 31).
     * e.g., for October 2026, returns April 1, 2026 00:00:00 to March 31, 2027 23:59:59.
     */
    fun getFinancialYearBoundaries(timestamp: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) // 0-based: 0 is Jan, 3 is April

        val startYear = if (month >= Calendar.APRIL) year else year - 1
        val endYear = startYear + 1

        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, startYear)
            set(Calendar.MONTH, Calendar.APRIL)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, endYear)
            set(Calendar.MONTH, Calendar.MARCH)
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    fun getFinancialYearLabel(timestamp: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val startYear = if (month >= Calendar.APRIL) year else year - 1
        return "FY ${startYear}-${(startYear + 1).toString().takeLast(2)}"
    }

    data class TaxSummary(
        val financialYearLabel: String,
        val sectionTotals: Map<String, Double>,
        val sectionTransactions: Map<String, List<TransactionEntity>>,
        val totalClaimedAmount: Double,
        val estimatedTaxSavings: Double,
        val totalTransactionsCount: Int
    )

    fun computeSummary(
        transactions: List<TransactionEntity>,
        forCurrentFyOnly: Boolean = true
    ): TaxSummary {
        val (fyStart, fyEnd) = getFinancialYearBoundaries()
        val fyLabel = getFinancialYearLabel()

        val filtered = transactions.filter { t ->
            val matchesType = t.type.equals("DEBIT", ignoreCase = true) || t.type.equals("TRANSFER", ignoreCase = true)
            val matchesDate = if (forCurrentFyOnly) t.timestamp in fyStart..fyEnd else true
            matchesType && matchesDate && !t.isDuplicate
        }

        val grouped = mutableMapOf<String, MutableList<TransactionEntity>>()

        for (t in filtered) {
            val section = t.taxDeductibleSection ?: inferSection(t.merchant, t.category, t.rawText)
            if (section != null) {
                grouped.getOrPut(section) { mutableListOf() }.add(t)
            }
        }

        val totals = mutableMapOf<String, Double>()
        var totalClaimed = 0.0

        for ((section, list) in grouped) {
            val sum = list.sumOf { it.amount }
            val limit = getSectionLimit(section)
            val effectiveClaimed = if (limit != null) minOf(sum, limit) else sum
            totals[section] = sum
            totalClaimed += effectiveClaimed
        }

        // Estimate tax savings based on conservative 20% slab (approx)
        val estimatedSavings = totalClaimed * 0.20

        return TaxSummary(
            financialYearLabel = fyLabel,
            sectionTotals = totals,
            sectionTransactions = grouped,
            totalClaimedAmount = totalClaimed,
            estimatedTaxSavings = estimatedSavings,
            totalTransactionsCount = grouped.values.sumOf { it.size }
        )
    }

    fun generateFormattedTaxReport(summary: TaxSummary): String {
        val sb = StringBuilder()
        sb.append("🏛️ Indian Income Tax Exemption Summary (${summary.financialYearLabel})\n")
        sb.append("Generated by VaultExpense (100% Offline Vault)\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append(String.format(Locale.getDefault(), "💰 Total Deductions Tracked: ₹%,.2f\n", summary.totalClaimedAmount))
        sb.append(String.format(Locale.getDefault(), "🛡️ Estimated Tax Saved (20%% Slab): ₹%,.2f\n", summary.estimatedTaxSavings))
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

        for (section in AVAILABLE_SECTIONS) {
            val total = summary.sectionTotals[section] ?: 0.0
            if (total > 0) {
                val name = getSectionDisplayName(section)
                val limit = getSectionLimit(section)
                val limitStr = if (limit != null) String.format(Locale.getDefault(), " / ₹%,.0f Cap", limit) else ""
                sb.append(String.format(Locale.getDefault(), "• %s: ₹%,.2f%s\n", name, total, limitStr))

                val txs = summary.sectionTransactions[section] ?: emptyList()
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                txs.take(5).forEach { tx ->
                    sb.append(String.format(Locale.getDefault(), "   - %s: ₹%,.2f (%s)\n", tx.merchant, tx.amount, dateFormat.format(Date(tx.timestamp))))
                }
                if (txs.size > 5) {
                    sb.append("   - ... and ${txs.size - 5} more transactions\n")
                }
                sb.append("\n")
            }
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Note: Verify receipts with your Chartered Accountant (CA) before filing ITR.")
        return sb.toString()
    }
}
