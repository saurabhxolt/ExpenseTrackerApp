package com.expensetracker.app.core.promotions

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromotionManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("promotions_cache", Context.MODE_PRIVATE)

    // GitHub Raw hosted configuration URL
    private val DEFAULT_REMOTE_JSON_URL = "https://raw.githubusercontent.com/saurabhxolt/ExpenseTrackerApp/Dev/promotions.json"

    suspend fun getActivePromotions(): List<Promotion> = withContext(Dispatchers.IO) {
        val config = fetchOrReadConfig()
        filterActivePromotions(config.promotions)
    }

    suspend fun getActiveAnnouncements(): List<Announcement> = withContext(Dispatchers.IO) {
        val config = fetchOrReadConfig()
        config.announcements.filter { it.enabled }
    }

    private suspend fun fetchOrReadConfig(): PromotionConfig = withContext(Dispatchers.IO) {
        var freshJson: String? = null
        try {
            val url = URL(DEFAULT_REMOTE_JSON_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            connection.useCaches = false // Disable HTTP cache for live updates

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                if (jsonString.isNotBlank()) {
                    prefs.edit()
                        .putString("cached_promotions_json", jsonString)
                        .putLong("last_refresh_time", System.currentTimeMillis())
                        .commit()
                    freshJson = jsonString
                }
            }
        } catch (e: Exception) {
            // Offline fallback
        }

        val jsonToParse = freshJson ?: prefs.getString("cached_promotions_json", null)
        if (jsonToParse != null) {
            parseConfig(jsonToParse)
        } else {
            getDefaultFallbackConfig()
        }
    }

    private fun filterActivePromotions(promotions: List<Promotion>): List<Promotion> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentAppVersion = getAppVersionCode()

        return promotions.filter { promo ->
            promo.enabled &&
                    promo.minimumAppVersion <= currentAppVersion &&
                    (promo.startDate.isBlank() || promo.startDate <= todayStr || promo.startDate.startsWith("202")) &&
                    (promo.endDate.isBlank() || promo.endDate >= todayStr || promo.endDate.startsWith("202"))
        }.sortedBy { it.priority }
    }

    private fun parseConfig(jsonStr: String): PromotionConfig {
        val promoList = mutableListOf<Promotion>()
        val announcementList = mutableListOf<Announcement>()
        try {
            val root = JSONObject(jsonStr)
            
            val promoArray = root.optJSONArray("promotions")
            if (promoArray != null) {
                for (i in 0 until promoArray.length()) {
                    val obj = promoArray.getJSONObject(i)
                    promoList.add(
                        Promotion(
                            id = obj.optString("id", ""),
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            imageUrl = obj.optString("imageUrl", ""),
                            actionUrl = obj.optString("actionUrl", ""),
                            priority = obj.optInt("priority", 1),
                            startDate = obj.optString("startDate", ""),
                            endDate = obj.optString("endDate", ""),
                            minimumAppVersion = obj.optInt("minimumAppVersion", 1),
                            enabled = obj.optBoolean("enabled", true)
                        )
                    )
                }
            }

            val annArray = root.optJSONArray("announcements")
            if (annArray != null) {
                for (i in 0 until annArray.length()) {
                    val obj = annArray.getJSONObject(i)
                    announcementList.add(
                        Announcement(
                            id = obj.optString("id", ""),
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            actionUrl = obj.optString("actionUrl", ""),
                            enabled = obj.optBoolean("enabled", true)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return getDefaultFallbackConfig()
        }
        return PromotionConfig(promotions = promoList, announcements = announcementList)
    }

    private fun getAppVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    private fun getDefaultFallbackConfig(): PromotionConfig {
        return PromotionConfig(
            promotions = listOf(
                Promotion(
                    id = "clinic_mngt_system",
                    title = "Clinic Management Platform 🏥",
                    description = "Complete healthcare & patient record management system for clinics.",
                    actionUrl = "https://saurabhxolt.github.io/ClinicMngt/",
                    priority = 1,
                    enabled = true
                ),
                Promotion(
                    id = "arivtech_solutions",
                    title = "Arivtech Software Solutions 🚀",
                    description = "Custom mobile, web & AI software engineering services.",
                    actionUrl = "https://saurabhxolt.github.io/arivtech-website/",
                    priority = 2,
                    enabled = true
                )
            ),
            announcements = listOf(
                Announcement(
                    id = "v1_release_note",
                    title = "Expense Tracker v1.0 Live 🎉",
                    description = "SQLCipher hardware encryption, 1-tap SMS auto-tracking, and Canvas visual analytics are now active.",
                    actionUrl = "https://github.com/saurabhxolt/ExpenseTrackerApp",
                    enabled = true
                )
            )
        )
    }
}
