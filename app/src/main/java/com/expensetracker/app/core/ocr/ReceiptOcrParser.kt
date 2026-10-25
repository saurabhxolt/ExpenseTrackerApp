package com.expensetracker.app.core.ocr

import com.expensetracker.app.ingestion.parser.CategoryClassifier
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedReceipt(
    val merchantName: String,
    val amount: Double,
    val dateMillis: Long,
    val suggestedCategory: String,
    val rawText: String,
    val allDetectedAmounts: List<Double> = emptyList()
)

@Singleton
class ReceiptOcrParser @Inject constructor() {

    // Regex for amounts with optional currency symbols or decimals
    private val amountPattern = Pattern.compile(
        """(?:(?:rs\.?|inr|₹)\s*)?([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2}))""",
        Pattern.CASE_INSENSITIVE
    )

    // Lines to strictly reject as monetary totals (e.g. quantity or item count lines)
    private val countNoiseKeywords = listOf(
        "total item", "total qty", "total quantity", "total count",
        "total pcs", "total piece", "item count", "items count",
        "qty:", "qty.", "no of items", "no. of items"
    )

    // Keywords indicating final total payable
    private val totalKeywords = listOf(
        "grand total", "net payable", "net amount", "total payable",
        "total amount", "amount payable", "final total", "bill total",
        "invoice total", "total due", "balance due", "sub total", "subtotal", "total"
    )

    // Lines to skip when identifying merchant name
    private val headerNoisePatterns = listOf(
        "tax invoice", "retail invoice", "cash receipt", "bill of supply",
        "welcome", "thank you", "visit again", "original for recipient",
        "duplicate copy", "customer copy",
        "gstin", "gst no", "fssai", "cin:", "pan:", "tel:", "phone:", "ph:", "order no",
        "table no", "token no", "date:", "time:", "cashier:", "pos id"
    )

    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val (amount, candidates) = extractAmountAndCandidates(lines)
        val dateMillis = extractDate(lines)
        val merchant = extractMerchant(lines)
        val category = CategoryClassifier.predictCategoryStatic(merchant, rawText, "DEBIT")

        return ParsedReceipt(
            merchantName = merchant,
            amount = amount,
            dateMillis = dateMillis,
            suggestedCategory = category,
            rawText = rawText,
            allDetectedAmounts = candidates
        )
    }

    private fun extractAmountAndCandidates(lines: List<String>): Pair<Double, List<Double>> {
        val allCandidates = mutableListOf<Double>()

        for (line in lines) {
            val amountsInLine = parseAllAmountsFromLine(line)
            allCandidates.addAll(amountsInLine)
        }

        // Priority 1: Check lines containing total keywords (bottom up)
        for (keyword in totalKeywords) {
            for (i in lines.indices.reversed()) {
                val line = lines[i]
                val lower = line.lowercase(Locale.ROOT)

                // Skip if this line is just an item count e.g. "Total Items: 3"
                if (countNoiseKeywords.any { lower.contains(it) }) {
                    continue
                }

                if (lower.contains(keyword)) {
                    // 1a. Try to parse amount from the exact line
                    val candidate = parseAmountFromLine(line)
                    if (candidate > 0.0) {
                        val sortedCandidates = allCandidates.distinct().sortedDescending()
                        return Pair(candidate, sortedCandidates)
                    }

                    // 1b. Proximity search: Check the immediately next line (e.g. line 1: "TOTAL", line 2: "₹1,450.00")
                    if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1]
                        val nextCandidate = parseAmountFromLine(nextLine)
                        if (nextCandidate > 0.0) {
                            val sortedCandidates = allCandidates.distinct().sortedDescending()
                            return Pair(nextCandidate, sortedCandidates)
                        }
                    }

                    // 1c. Proximity search 2 lines ahead if line i+1 was a symbol like "₹" or "INR"
                    if (i + 2 < lines.size) {
                        val secondLine = lines[i + 2]
                        val secondCandidate = parseAmountFromLine(secondLine)
                        if (secondCandidate > 0.0) {
                            val sortedCandidates = allCandidates.distinct().sortedDescending()
                            return Pair(secondCandidate, sortedCandidates)
                        }
                    }
                }
            }
        }

        // Priority 2: Scan bottom-up for highest realistic monetary amount
        val bottomCandidates = mutableListOf<Double>()
        for (line in lines.reversed().take(15)) {
            val lower = line.lowercase(Locale.ROOT)
            if (countNoiseKeywords.any { lower.contains(it) }) continue
            bottomCandidates.addAll(parseAllAmountsFromLine(line))
        }

        val primary = bottomCandidates.maxOrNull() ?: allCandidates.maxOrNull() ?: 0.0
        val sortedCandidates = allCandidates.distinct().sortedDescending()
        return Pair(primary, sortedCandidates)
    }

    private fun parseAmountFromLine(line: String): Double {
        val amounts = parseAllAmountsFromLine(line)
        return amounts.lastOrNull() ?: 0.0
    }

    private fun parseAllAmountsFromLine(line: String): List<Double> {
        val result = mutableListOf<Double>()
        val matcher = amountPattern.matcher(line)
        while (matcher.find()) {
            val strVal = matcher.group(1)?.replace(",", "") ?: continue
            val candidate = strVal.toDoubleOrNull() ?: continue

            // Filter out noise numbers:
            // 1. Years (2020..2030)
            // 2. Huge integers (> 500,000)
            // 3. Tiny fractions (< 0.50)
            // 4. Standalone 6-digit pin codes
            val isYear = candidate in 2020.0..2030.0 && !strVal.contains(".")
            val isPinCode = candidate in 100000.0..999999.0 && !strVal.contains(".")
            val isInvalidRange = candidate < 0.50 || candidate > 500000.0

            if (!isYear && !isPinCode && !isInvalidRange) {
                // Round to 2 decimal places
                val rounded = kotlin.math.round(candidate * 100.0) / 100.0
                result.add(rounded)
            }
        }
        return result
    }

    private fun extractDate(lines: List<String>): Long {
        val dateRegexes = listOf(
            Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b"""),
            Regex("""\b(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{2,4})\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(\d{4}[/-]\d{1,2}[/-]\d{1,2})\b""")
        )

        val dateFormats = listOf(
            "dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy", "dd-MM-yy",
            "d/M/yyyy", "d-M-yyyy",
            "dd MMM yyyy", "d MMM yyyy",
            "yyyy-MM-dd", "yyyy/MM/dd"
        )

        for (line in lines) {
            for (regex in dateRegexes) {
                val match = regex.find(line)
                if (match != null) {
                    val dateStr = match.groupValues[1]
                    for (fmt in dateFormats) {
                        try {
                            val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                            sdf.isLenient = false
                            val parsed = sdf.parse(dateStr)
                            if (parsed != null && parsed.time in 946684800000L..System.currentTimeMillis() + 86400000L) {
                                return parsed.time
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }

        return System.currentTimeMillis()
    }

    private fun extractMerchant(lines: List<String>): String {
        for (line in lines.take(8)) {
            val lower = line.lowercase(Locale.ROOT)
            val isNoise = headerNoisePatterns.any { lower.contains(it) }
            val hasDigitsOnly = line.all { it.isDigit() || it.isWhitespace() || it == '-' || it == '/' || it == ':' }
            val isUrlOrEmail = lower.contains("www.") || lower.contains(".com") || lower.contains(".in") || lower.contains("@")

            if (!isNoise && !hasDigitsOnly && !isUrlOrEmail && line.length >= 3) {
                // Clean up any stray punctuation
                return line.trim(',', ':', ';', '-', '*', '#')
            }
        }

        return "Retail Store"
    }
}
