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

    suspend fun getActivePromotions(): List<Promotion> = withContext(Dispatchers.IO) {
        val cachedJson = prefs.getString("cached_promotions_json", null)
        val promotions = if (cachedJson != null) {
            parsePromotions(cachedJson)
        } else {
            getDefaultFallbackPromotions()
        }

        // Filter active promotions
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentAppVersion = getAppVersionCode()

        promotions.filter { promo ->
            promo.enabled &&
                    promo.minimumAppVersion <= currentAppVersion &&
                    (promo.startDate.isBlank() || promo.startDate <= todayStr) &&
                    (promo.endDate.isBlank() || promo.endDate >= todayStr)
        }.sortedBy { it.priority }
    }

    suspend fun refreshPromotions(jsonUrl: String) = withContext(Dispatchers.IO) {
        try {
            val url = URL(jsonUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                if (jsonString.isNotBlank()) {
                    prefs.edit()
                        .putString("cached_promotions_json", jsonString)
                        .putLong("last_refresh_time", System.currentTimeMillis())
                        .apply()
                }
            }
        } catch (e: Exception) {
            // Gracefully ignore fetch errors offline
        }
    }

    private fun parsePromotions(jsonStr: String): List<Promotion> {
        val list = mutableListOf<Promotion>()
        try {
            val root = JSONObject(jsonStr)
            val array = root.optJSONArray("promotions") ?: return emptyList()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
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
        } catch (e: Exception) {
            return getDefaultFallbackPromotions()
        }
        return list
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

    private fun getDefaultFallbackPromotions(): List<Promotion> {
        return listOf(
            Promotion(
                id = "ai_assistant_plugin",
                title = "AI Assistant Plugin 🧠",
                description = "Coming Soon: On-device LLM inference (Gemma & Phi) for privacy-first financial insights.",
                priority = 1,
                enabled = true
            )
        )
    }
}
