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
import org.json.JSONObject

/**
 * Local configuration for the Assistant API integration.
 * API credentials are encrypted with an Android Keystore AES-GCM key before persistence.
 */
class AssistantIntegrationPreferences(context: Context) : JsonSerializable {
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

    private fun defaultModel(providerId: String): String = when (providerId) {
        "gemini" -> "gemini-3.8-flash"
        "openai" -> "gpt-4.1-mini"
        "anthropic" -> "claude-3-5-haiku-latest"
        "deepseek" -> "deepseek-chat"
        "groq" -> "llama-3.3-70b-versatile"
        "openrouter" -> "google/gemini-3.8-flash"
        "mistral" -> "mistral-small-latest"
        "xai" -> "grok-3-mini"
        else -> ""
    }

    var model: String
        get() = prefs.getString("model_$selectedProviderId", null)
            ?: if (selectedProviderId == "openai") prefs.getString("model", defaultModel(selectedProviderId)) ?: defaultModel(selectedProviderId)
            else defaultModel(selectedProviderId)
        set(value) { prefs.edit().putString("model_$selectedProviderId", value.trim()).apply() }

    var maxOutputTokens: Int
        get() = prefs.getInt("max_output_tokens", 800).coerceIn(128, 4096)
        set(value) { prefs.edit().putInt("max_output_tokens", value.coerceIn(128, 4096)).apply() }

    var temperature: Float
        get() = prefs.getFloat("temperature", 0.7f).coerceIn(0f, 2f)
        set(value) { prefs.edit().putFloat("temperature", value.coerceIn(0f, 2f)).apply() }

    var requestTimeoutSeconds: Int
        get() = prefs.getInt("request_timeout_seconds", 90).coerceIn(60, 180)
        set(value) { prefs.edit().putInt("request_timeout_seconds", value.coerceIn(60, 180)).apply() }

    var streamResponses: Boolean
        get() = prefs.getBoolean("stream_responses", true)
        set(value) { prefs.edit().putBoolean("stream_responses", value).apply() }

    var includeConversationContext: Boolean
        get() = prefs.getBoolean("include_context", true)
        set(value) { prefs.edit().putBoolean("include_context", value).apply() }


    /**
     * Exports provider configuration only. API keys are deliberately excluded because they are
     * encrypted with a device-bound Android Keystore key and must never be placed in a portable file.
     */
    override suspend fun toJson(): String = JSONObject().apply {
        put("provider", provider)
        put("selectedProviderId", selectedProviderId)
        put("endpoint", endpoint)
        put("model", model)
        put("maxOutputTokens", maxOutputTokens)
        put("temperature", temperature.toDouble())
        put("requestTimeoutSeconds", requestTimeoutSeconds)
        put("streamResponses", streamResponses)
        put("includeConversationContext", includeConversationContext)
    }.toString()

    /**
     * Restores non-secret provider configuration. Existing encrypted API keys are intentionally
     * left untouched; restoring to another device requires the user to enter its API key again.
     */
    override suspend fun fromJson(json: String) {
        val obj = JSONObject(json)
        if (obj.has("selectedProviderId")) selectedProviderId = obj.optString("selectedProviderId")
        if (obj.has("provider")) provider = obj.optString("provider")
        if (obj.has("endpoint")) endpoint = obj.optString("endpoint")
        if (obj.has("model")) model = obj.optString("model")
        if (obj.has("maxOutputTokens")) maxOutputTokens = obj.optInt("maxOutputTokens", maxOutputTokens)
        if (obj.has("temperature")) temperature = obj.optDouble("temperature", temperature.toDouble()).toFloat()
        if (obj.has("requestTimeoutSeconds")) requestTimeoutSeconds = obj.optInt("requestTimeoutSeconds", requestTimeoutSeconds)
        if (obj.has("streamResponses")) streamResponses = obj.optBoolean("streamResponses", streamResponses)
        if (obj.has("includeConversationContext")) {
            includeConversationContext = obj.optBoolean("includeConversationContext", includeConversationContext)
        }
    }

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
