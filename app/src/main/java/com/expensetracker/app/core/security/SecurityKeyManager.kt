package com.expensetracker.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecurityKeyManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "vault_expense_sqlcipher_master_key"
    private const val PREFS_NAME = "expense_security_vault_prefs"
    private const val ENCRYPTED_PASSPHRASE_KEY = "encrypted_db_passphrase"
    private const val PASSPHRASE_IV_KEY = "db_passphrase_iv"

    @Volatile
    private var cachedPassphrase: ByteArray? = null

    @Synchronized
    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        cachedPassphrase?.let { return it }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(ENCRYPTED_PASSPHRASE_KEY, null)
        val ivBase64 = prefs.getString(PASSPHRASE_IV_KEY, null)

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }

        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

        if (encryptedBase64 != null && ivBase64 != null) {
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                val decrypted = cipher.doFinal(encrypted)
                cachedPassphrase = decrypted
                return decrypted
            } catch (e: Exception) {
                // If Keystore was corrupted or invalidated, fall through to regenerate
            }
        }

        // Seamless migration: If DB file existed prior to Keystore integration, seal existing key
        val dbFile = context.getDatabasePath("expense_tracker_encrypted.db")
        val rawPassphrase = if (dbFile.exists() && encryptedBase64 == null) {
            "expense_tracker_secret_passphrase_key".toByteArray()
        } else {
            // Generate 32 bytes (256 bits) secure random passphrase
            val randomBytes = ByteArray(32)
            SecureRandom().nextBytes(randomBytes)
            randomBytes
        }

        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encrypted = cipher.doFinal(rawPassphrase)
            val iv = cipher.iv

            prefs.edit()
                .putString(ENCRYPTED_PASSPHRASE_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(PASSPHRASE_IV_KEY, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedPassphrase = rawPassphrase
        return rawPassphrase
    }
}
