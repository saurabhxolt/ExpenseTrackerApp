package com.expensetracker.app.core.sync

import android.graphics.Bitmap
import android.graphics.Color
import com.expensetracker.app.core.database.entity.ExpenseSplitShareEntity
import com.expensetracker.app.core.database.entity.GroupEntity
import com.expensetracker.app.core.database.entity.GroupMemberEntity
import com.expensetracker.app.core.database.entity.GroupSettlementEntity
import com.expensetracker.app.core.database.entity.SplitExpenseEntity
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class GroupSyncPayload(
    val version: Int = 1,
    val senderName: String,
    val group: GroupSyncData
)

data class GroupSyncData(
    val name: String,
    val description: String = "",
    val emoji: String = "✈️",
    val createdAt: Long = System.currentTimeMillis(),
    val members: List<String> = emptyList(),
    val expenses: List<ExpenseSyncData> = emptyList(),
    val settlements: List<SettlementSyncData> = emptyList()
)

data class ExpenseSyncData(
    val title: String,
    val totalAmount: Double,
    val paidByMember: String,
    val category: String = "General Expense",
    val timestamp: Long = System.currentTimeMillis(),
    val shares: List<ShareSyncData> = emptyList()
)

data class ShareSyncData(
    val memberName: String,
    val shareAmount: Double
)

data class SettlementSyncData(
    val fromMember: String,
    val toMember: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class GroupQrSyncEngine @Inject constructor() {

    companion object {
        private const val PREFIX_UNCOMPRESSED = "ET_GRP:1:"
        private const val PREFIX_GZIPPED = "ET_GRP_Z:1:"
    }

    /**
     * Serializes group data into an air-gapped QR sync payload string.
     * Uses GZIP + Base64 compression when the payload exceeds threshold.
     */
    fun encodeGroupPayload(payload: GroupSyncPayload): String {
        val root = JSONObject()
        root.put("v", payload.version)
        root.put("sender", payload.senderName)

        val grpObj = JSONObject()
        grpObj.put("name", payload.group.name)
        grpObj.put("desc", payload.group.description)
        grpObj.put("emoji", payload.group.emoji)
        grpObj.put("time", payload.group.createdAt)

        val memArray = JSONArray()
        payload.group.members.forEach { memArray.put(it) }
        grpObj.put("members", memArray)

        val expArray = JSONArray()
        for (exp in payload.group.expenses) {
            val expObj = JSONObject()
            expObj.put("title", exp.title)
            expObj.put("amt", exp.totalAmount)
            expObj.put("paidBy", exp.paidByMember)
            expObj.put("cat", exp.category)
            expObj.put("time", exp.timestamp)

            val sharesArray = JSONArray()
            for (share in exp.shares) {
                val sObj = JSONObject()
                sObj.put("m", share.memberName)
                sObj.put("a", share.shareAmount)
                sharesArray.put(sObj)
            }
            expObj.put("shares", sharesArray)
            expArray.put(expObj)
        }
        grpObj.put("expenses", expArray)

        val settArray = JSONArray()
        for (s in payload.group.settlements) {
            val sObj = JSONObject()
            sObj.put("from", s.fromMember)
            sObj.put("to", s.toMember)
            sObj.put("amt", s.amount)
            sObj.put("time", s.timestamp)
            settArray.put(sObj)
        }
        grpObj.put("settlements", settArray)

        root.put("group", grpObj)
        val jsonStr = root.toString()

        return if (jsonStr.length > 250) {
            try {
                val bos = ByteArrayOutputStream()
                GZIPOutputStream(bos).use { gzip ->
                    gzip.write(jsonStr.toByteArray(StandardCharsets.UTF_8))
                }
                val compressedBytes = bos.toByteArray()
                val base64 = Base64.getUrlEncoder().withoutPadding().encodeToString(compressedBytes)
                "$PREFIX_GZIPPED$base64"
            } catch (_: Exception) {
                "$PREFIX_UNCOMPRESSED$jsonStr"
            }
        } else {
            "$PREFIX_UNCOMPRESSED$jsonStr"
        }
    }

    /**
     * Parses an encoded QR sync payload string back into a GroupSyncPayload.
     */
    fun decodeGroupPayload(rawPayload: String): GroupSyncPayload? {
        val trimmed = rawPayload.trim()
        val jsonStr: String = when {
            trimmed.startsWith(PREFIX_GZIPPED) -> {
                try {
                    val base64 = trimmed.substring(PREFIX_GZIPPED.length)
                    val compressedBytes = Base64.getUrlDecoder().decode(base64)
                    val bis = ByteArrayInputStream(compressedBytes)
                    GZIPInputStream(bis).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                } catch (_: Exception) {
                    return null
                }
            }
            trimmed.startsWith(PREFIX_UNCOMPRESSED) -> {
                trimmed.substring(PREFIX_UNCOMPRESSED.length)
            }
            trimmed.startsWith("{") -> {
                trimmed
            }
            else -> return null
        }

        return try {
            val root = JSONObject(jsonStr)
            val version = root.optInt("v", 1)
            val sender = root.optString("sender", "Friend")

            val grpObj = root.getJSONObject("group")
            val name = grpObj.getString("name")
            val desc = grpObj.optString("desc", "")
            val emoji = grpObj.optString("emoji", "✈️")
            val createdAt = grpObj.optLong("time", System.currentTimeMillis())

            val membersList = mutableListOf<String>()
            val memArray = grpObj.optJSONArray("members")
            if (memArray != null) {
                for (i in 0 until memArray.length()) {
                    membersList.add(memArray.getString(i))
                }
            }

            val expensesList = mutableListOf<ExpenseSyncData>()
            val expArray = grpObj.optJSONArray("expenses")
            if (expArray != null) {
                for (i in 0 until expArray.length()) {
                    val eo = expArray.getJSONObject(i)
                    val title = eo.getString("title")
                    val amt = eo.getDouble("amt")
                    val paidBy = eo.getString("paidBy")
                    val cat = eo.optString("cat", "General Expense")
                    val time = eo.optLong("time", System.currentTimeMillis())

                    val sharesList = mutableListOf<ShareSyncData>()
                    val soArray = eo.optJSONArray("shares")
                    if (soArray != null) {
                        for (j in 0 until soArray.length()) {
                            val so = soArray.getJSONObject(j)
                            sharesList.add(ShareSyncData(so.getString("m"), so.getDouble("a")))
                        }
                    }

                    expensesList.add(
                        ExpenseSyncData(
                            title = title,
                            totalAmount = amt,
                            paidByMember = paidBy,
                            category = cat,
                            timestamp = time,
                            shares = sharesList
                        )
                    )
                }
            }

            val settlementsList = mutableListOf<SettlementSyncData>()
            val settArray = grpObj.optJSONArray("settlements")
            if (settArray != null) {
                for (i in 0 until settArray.length()) {
                    val so = settArray.getJSONObject(i)
                    settlementsList.add(
                        SettlementSyncData(
                            fromMember = so.getString("from"),
                            toMember = so.getString("to"),
                            amount = so.getDouble("amt"),
                            timestamp = so.optLong("time", System.currentTimeMillis())
                        )
                    )
                }
            }

            GroupSyncPayload(
                version = version,
                senderName = sender,
                group = GroupSyncData(
                    name = name,
                    description = desc,
                    emoji = emoji,
                    createdAt = createdAt,
                    members = membersList,
                    expenses = expensesList,
                    settlements = settlementsList
                )
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolves member identity perspective between sender and recipient:
     * - The sender's "You" becomes the sender's actual name (e.g. "Alice").
     * - The recipient's chosen identity in the group (e.g. "Bob") becomes "You" on this device.
     */
    fun mapIdentity(payload: GroupSyncPayload, myIdentityInGroup: String): GroupSyncData {
        val senderActualName = payload.senderName.trim().ifBlank { "Sender" }
        val rawGroup = payload.group

        // Mapping function for any member string
        val mapMember: (String) -> String = { name ->
            when {
                name.equals("You", ignoreCase = true) -> senderActualName
                name.equals(myIdentityInGroup, ignoreCase = true) -> "You"
                else -> name
            }
        }

        val mappedMembers = rawGroup.members.map(mapMember).distinct()

        val mappedExpenses = rawGroup.expenses.map { exp ->
            exp.copy(
                paidByMember = mapMember(exp.paidByMember),
                shares = exp.shares.map { share ->
                    share.copy(memberName = mapMember(share.memberName))
                }
            )
        }

        val mappedSettlements = rawGroup.settlements.map { sett ->
            sett.copy(
                fromMember = mapMember(sett.fromMember),
                toMember = mapMember(sett.toMember)
            )
        }

        return rawGroup.copy(
            members = mappedMembers,
            expenses = mappedExpenses,
            settlements = mappedSettlements
        )
    }

    /**
     * Generates an unblurred, high-contrast QR Code Bitmap using ZXing.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val hints = HashMap<EncodeHintType, Any>()
        hints[EncodeHintType.MARGIN] = 1
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"

        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Decodes a QR code directly from a Bitmap.
     */
    fun decodeQrBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            result.text
        } catch (_: Exception) {
            null
        }
    }
}
