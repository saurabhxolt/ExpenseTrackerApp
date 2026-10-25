package com.expensetracker.app.ingestion.parser

import com.expensetracker.app.core.database.dao.MerchantCategoryMappingDao
import com.expensetracker.app.core.database.entity.MerchantCategoryMappingEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryClassifier @Inject constructor(
    private val merchantCategoryMappingDao: MerchantCategoryMappingDao
) {

    companion object {
        fun normalizeMerchantKey(merchant: String): String {
            return merchant.lowercase()
                .replace(Regex("[^a-z0-9]"), "")
                .trim()
        }

        fun predictCategoryStatic(merchant: String, text: String, type: String): String {
            val lowerMerchant = merchant.lowercase()
            val lowerText = text.lowercase()

            if (type == "CREDIT") return "Income"
            if (type == "INVESTMENT") return "Investments"
            if (type == "TRANSFER") {
                if (lowerMerchant.contains("cred") || lowerMerchant.contains("cheq") || lowerMerchant.contains("card bill")) {
                    return "Bills & Utilities"
                }
                return "Transfer"
            }

            return when {
                // Food & Dining
                lowerMerchant.contains("swiggy") || lowerMerchant.contains("zomato") ||
                lowerMerchant.contains("domino") || lowerMerchant.contains("mcdonald") ||
                lowerMerchant.contains("kfc") || lowerMerchant.contains("starbucks") ||
                lowerMerchant.contains("food") || lowerMerchant.contains("restaurant") ||
                lowerMerchant.contains("cafe") || lowerMerchant.contains("atithi") ||
                lowerMerchant.contains("hotel") || lowerMerchant.contains("dining") ||
                lowerText.contains("swiggy") || lowerText.contains("zomato") -> "Food & Dining"

                // Groceries & Vegetables
                lowerMerchant.contains("blinkit") || lowerMerchant.contains("zepto") ||
                lowerMerchant.contains("bigbasket") || lowerMerchant.contains("instamart") ||
                lowerMerchant.contains("grocery") || lowerMerchant.contains("supermarket") ||
                lowerMerchant.contains("vegetable") || lowerMerchant.contains("fruit") -> "Groceries & Vegetables"

                // Fuel & Transportation
                lowerMerchant.contains("fuel") || lowerMerchant.contains("petrol") ||
                lowerMerchant.contains("hpcl") || lowerMerchant.contains("bpcl") ||
                lowerMerchant.contains("iocl") || lowerMerchant.contains("shell") -> "Fuel"

                lowerMerchant.contains("uber") || lowerMerchant.contains("ola") ||
                lowerMerchant.contains("rapido") || lowerMerchant.contains("metro") ||
                lowerMerchant.contains("irctc") || lowerMerchant.contains("indigo") ||
                lowerMerchant.contains("airindia") || lowerMerchant.contains("redbus") ||
                lowerMerchant.contains("transport") -> "Transportation"

                // Bills & Utilities
                lowerMerchant.contains("airtel") || lowerMerchant.contains("jio") ||
                lowerMerchant.contains("vi ") || lowerMerchant.contains("vodafone") ||
                lowerMerchant.contains("bescom") || lowerMerchant.contains("tata power") ||
                lowerMerchant.contains("act fibernet") || lowerMerchant.contains("broadband") ||
                lowerMerchant.contains("electricity") || lowerMerchant.contains("billdesk") ||
                lowerText.contains("recharge") || lowerText.contains("utility") -> "Bills & Utilities"

                // Subscriptions
                lowerMerchant.contains("netflix") || lowerMerchant.contains("spotify") ||
                lowerMerchant.contains("prime") || lowerMerchant.contains("youtube") ||
                lowerMerchant.contains("hotstar") || lowerMerchant.contains("apple") -> "Subscriptions"

                // Shopping
                lowerMerchant.contains("amazon") || lowerMerchant.contains("flipkart") ||
                lowerMerchant.contains("myntra") || lowerMerchant.contains("nykaa") ||
                lowerMerchant.contains("ajio") || lowerMerchant.contains("tata cliq") ||
                lowerMerchant.contains("uniqlo") || lowerMerchant.contains("zara") ||
                lowerMerchant.contains("mall") || lowerMerchant.contains("retail") -> "Shopping"

                // Health & Medical
                lowerMerchant.contains("pharmacy") || lowerMerchant.contains("apollo") ||
                lowerMerchant.contains("1mg") || lowerMerchant.contains("pharmeasy") ||
                lowerMerchant.contains("hospital") || lowerMerchant.contains("clinic") ||
                lowerMerchant.contains("doctor") || lowerMerchant.contains("lab") -> "Health & Medical"

                // Insurance & Fees
                lowerMerchant.contains("acko") || lowerMerchant.contains("policybazaar") ||
                lowerMerchant.contains("lic") || lowerMerchant.contains("insurance") -> "Insurance"

                lowerMerchant.contains("morth") || lowerMerchant.contains("challan") ||
                lowerMerchant.contains("tax") || lowerMerchant.contains("fee") -> "Government & Fees"

                // Investments
                lowerMerchant.contains("zerodha") || lowerMerchant.contains("groww") ||
                lowerMerchant.contains("indmoney") || lowerMerchant.contains("upstox") ||
                lowerMerchant.contains("ksec") || lowerMerchant.contains("trading") ||
                lowerMerchant.contains("mutual fund") || lowerMerchant.contains("sip") -> "Investments"

                // Rent & EMI
                lowerMerchant.contains("rent") || lowerMerchant.contains("housing") -> "Rent"
                lowerMerchant.contains("emi") || lowerMerchant.contains("loan") -> "EMI"

                else -> "General Expense"
            }
        }
    }

    suspend fun classify(merchant: String, rawText: String, type: String): String {
        // 1. Check user preference learning memory FIRST
        val normalizedKey = normalizeMerchantKey(merchant)
        if (normalizedKey.isNotBlank()) {
            val mapping = merchantCategoryMappingDao.getMappingForMerchant(normalizedKey)
            if (mapping != null && mapping.assignedCategory.isNotBlank()) {
                return mapping.assignedCategory
            }
        }

        // 2. Fall back to static rules
        return predictCategoryStatic(merchant, rawText, type)
    }

    suspend fun saveUserMerchantPreference(merchant: String, assignedCategory: String) {
        val normalizedKey = normalizeMerchantKey(merchant)
        if (normalizedKey.isNotBlank() && assignedCategory.isNotBlank()) {
            merchantCategoryMappingDao.insertMapping(
                MerchantCategoryMappingEntity(
                    merchantKey = normalizedKey,
                    assignedCategory = assignedCategory,
                    lastUpdatedMs = System.currentTimeMillis()
                )
            )
        }
    }
}
