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
    val rawText: String
)

@Singleton
class ReceiptOcrParser @Inject constructor() {

    // Regex for amounts with currency symbols or standalone decimals
    private val amountPattern = Pattern.compile(
        "(?:(?:rs\\.?|inr|₹)\\s*)?([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // Keywords indicating final total payable
    private val totalKeywords = listOf(
        "grand total", "net payable", "net amount", "total payable",
        "total amount", "amount payable", "final total", "bill total",
        "invoice total", "total", "sub total", "subtotal", "balance due"
    )

    // Lines to skip when identifying merchant name
    private val headerNoisePatterns = listOf(
        "tax invoice", "retail invoice", "cash receipt", "bill of supply",
        "welcome", "thank you", "visit again", "original for recipient",
        "gstin", "fssai", "cin:", "pan:", "tel:", "phone:", "ph:", "order no",
        "table no", "token no", "date:", "time:"
    )

    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val amount = extractAmount(lines)
        val dateMillis = extractDate(lines)
        val merchant = extractMerchant(lines)
        val category = CategoryClassifier.predictCategoryStatic(merchant, rawText, "DEBIT")

        return ParsedReceipt(
            merchantName = merchant,
            amount = amount,
            dateMillis = dateMillis,
            suggestedCategory = category,
            rawText = rawText
        )
    }

    private fun extractAmount(lines: List<String>): Double {
        // Priority 1: Check lines explicitly containing total keywords
        for (keyword in totalKeywords) {
            for (line in lines.reversed()) {
                val lower = line.lowercase(Locale.ROOT)
                if (lower.contains(keyword)) {
                    val candidate = parseAmountFromLine(line)
                    if (candidate > 0.0) {
                        return candidate
                    }
                }
            }
        }

        // Priority 2: Scan from bottom-up for the first valid monetary amount
        val candidates = mutableListOf<Double>()
        for (line in lines.reversed().take(12)) {
            val candidate = parseAmountFromLine(line)
            if (candidate > 0.0) {
                candidates.add(candidate)
            }
        }

        return candidates.maxOrNull() ?: 0.0
    }

    private fun parseAmountFromLine(line: String): Double {
        val matcher = amountPattern.matcher(line)
        var lastFound = 0.0
        while (matcher.find()) {
            val strVal = matcher.group(1)?.replace(",", "") ?: continue
            val candidate = strVal.toDoubleOrNull() ?: continue
            // Ignore phone numbers, pin codes, or years formatted as integers (> 500,000 or 1900..2100)
            if (candidate in 1.0..500000.0 && candidate != 2024.0 && candidate != 2025.0 && candidate != 2026.0) {
                lastFound = candidate
            }
        }
        return lastFound
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
        for (line in lines.take(6)) {
            val lower = line.lowercase(Locale.ROOT)
            val isNoise = headerNoisePatterns.any { lower.contains(it) }
            val hasDigitsOnly = line.all { it.isDigit() || it.isWhitespace() || it == '-' || it == '/' }

            if (!isNoise && !hasDigitsOnly && line.length >= 3) {
                // Clean up any stray punctuation
                return line.trim(',', ':', ';', '-', '*', '#')
            }
        }

        return "Retail Store"
    }
}
