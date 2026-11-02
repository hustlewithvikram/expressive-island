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

    /** Stable provider identifier used by the Integrations provider selector. */
    var selectedProviderId: String
        get() = prefs.getString("provider_id", null) ?: when (provider.lowercase()) {
            "openai-compatible", "openai", "chatgpt" -> "openai"
            else -> "custom"
        }
        set(value) { prefs.edit().putString("provider_id", value).apply() }

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
        get() = prefs.getInt("request_timeout_seconds", 90).coerceIn(30, 180)
        set(value) { prefs.edit().putInt("request_timeout_seconds", value.coerceIn(30, 180)).apply() }

    var streamResponses: Boolean
        get() = prefs.getBoolean("stream_responses", true)
        set(value) { prefs.edit().putBoolean("stream_responses", value).apply() }

    var includeConversationContext: Boolean
        get() = prefs.getBoolean("include_context", true)
        set(value) { prefs.edit().putBoolean("include_context", value).apply() }

    var apiKey: String
        get() = getApiKey(selectedProviderId)
        set(value) = setApiKey(selectedProviderId, value)

    /** Each provider keeps its own encrypted credential instead of overwriting another provider's key. */
    fun getApiKey(providerId: String): String = runCatching {
        val preferenceKey = apiKeyPreferenceKey(providerId)
        val encoded = prefs.getString(preferenceKey, null)
            ?: (if (providerId == "openai") prefs.getString("api_key", null) else null)
            ?: return@runCatching ""
        val packed = Base64.decode(encoded, Base64.NO_WRAP)
        val ivSize = packed[0].toInt()
        val iv = packed.copyOfRange(1, 1 + ivSize)
        val encrypted = packed.copyOfRange(1 + ivSize, packed.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }.getOrDefault("")

    fun setApiKey(providerId: String, value: String) {
        val preferenceKey = apiKeyPreferenceKey(providerId)
        if (value.isBlank()) {
            prefs.edit().remove(preferenceKey).apply()
            if (providerId == "openai") prefs.edit().remove("api_key").apply()
            return
        }
        runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val packed = byteArrayOf(cipher.iv.size.toByte()) + cipher.iv + encrypted
            prefs.edit().putString(preferenceKey, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
            if (providerId == "openai") prefs.edit().remove("api_key").apply()
        }
    }

    fun hasApiKey(): Boolean =
        prefs.contains(apiKeyPreferenceKey(selectedProviderId)) ||
            (selectedProviderId == "openai" && prefs.contains("api_key"))

    private fun apiKeyPreferenceKey(providerId: String): String =
        "api_key_${providerId.lowercase().replace(Regex("[^a-z0-9_-]"), "_")}"

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
