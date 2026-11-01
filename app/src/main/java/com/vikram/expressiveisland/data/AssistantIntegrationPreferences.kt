package com.vikram.expressiveisland.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Local configuration for the Assistant API integration.
 * API credentials are encrypted with an Android Keystore AES-GCM key before persistence.
 */
class AssistantIntegrationPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("assistant_integration", Context.MODE_PRIVATE)

    var provider: String
        get() = prefs.getString("provider", "OpenAI-compatible") ?: "OpenAI-compatible"
        set(value) { prefs.edit().putString("provider", value).apply() }

    var endpoint: String
        get() = prefs.getString("endpoint", "https://api.openai.com/v1") ?: "https://api.openai.com/v1"
        set(value) { prefs.edit().putString("endpoint", value.trim()).apply() }

    var model: String
        get() = prefs.getString("model", "gpt-4o-mini") ?: "gpt-4o-mini"
        set(value) { prefs.edit().putString("model", value.trim()).apply() }

    var maxOutputTokens: Int
        get() = prefs.getInt("max_output_tokens", 800).coerceIn(128, 4096)
        set(value) { prefs.edit().putInt("max_output_tokens", value.coerceIn(128, 4096)).apply() }

    var temperature: Float
        get() = prefs.getFloat("temperature", 0.7f).coerceIn(0f, 2f)
        set(value) { prefs.edit().putFloat("temperature", value.coerceIn(0f, 2f)).apply() }

    var requestTimeoutSeconds: Int
        get() = prefs.getInt("request_timeout_seconds", 30).coerceIn(10, 120)
        set(value) { prefs.edit().putInt("request_timeout_seconds", value.coerceIn(10, 120)).apply() }

    var streamResponses: Boolean
        get() = prefs.getBoolean("stream_responses", true)
        set(value) { prefs.edit().putBoolean("stream_responses", value).apply() }

    var includeConversationContext: Boolean
        get() = prefs.getBoolean("include_context", true)
        set(value) { prefs.edit().putBoolean("include_context", value).apply() }

    var apiKey: String
        get() = runCatching {
            val packed = Base64.decode(prefs.getString("api_key", null), Base64.NO_WRAP)
            val ivSize = packed[0].toInt()
            val iv = packed.copyOfRange(1, 1 + ivSize)
            val encrypted = packed.copyOfRange(1 + ivSize, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrDefault("")
        set(value) {
            if (value.isBlank()) {
                prefs.edit().remove("api_key").apply()
            } else {
                runCatching {
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
                    val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
                    val packed = byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + encrypted
                    prefs.edit().putString("api_key", Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
                }
            }
        }

    fun hasApiKey(): Boolean = prefs.contains("api_key")

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "expressive_island_assistant_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
