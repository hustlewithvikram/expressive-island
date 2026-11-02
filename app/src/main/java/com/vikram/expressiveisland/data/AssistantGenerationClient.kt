package com.vikram.expressiveisland.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AssistantChatTurn(val role: String, val content: String)

/**
 * Small provider adapter for the APIs exposed by the Integrations screen.
 * OpenAI-compatible vendors share /chat/completions; Gemini and Anthropic use their native APIs.
 */
class AssistantGenerationClient(private val preferences: AssistantIntegrationPreferences) {
    suspend fun generate(prompt: String, history: List<AssistantChatTurn>): String =
        withContext(Dispatchers.IO) {
            val provider = preferences.selectedProviderId
            val key = preferences.getApiKey(provider).trim()
            require(key.isNotEmpty()) { "Add an API key for the selected provider in Integrations." }
            val configuredModel = preferences.model.trim()
            // Gemini 2.5 Flash is no longer available to newly onboarded API users.
            // Migrate the stale saved value and use the current model recommended by Google's error.
            val model = when {
                provider == "gemini" && (configuredModel.isBlank() || configuredModel == "gemini-2.5-flash" || configuredModel == "models/gemini-2.5-flash") ->
                    "gemini-3.8-flash"
                configuredModel.isBlank() -> "gpt-4o-mini"
                else -> configuredModel.removePrefix("models/")
            }
            if (provider == "gemini" && model != configuredModel) preferences.model = model
            val endpoint = preferences.endpoint.trim().trimEnd('/')
            require(endpoint.isNotBlank()) { "The API endpoint is empty. Check the selected provider settings." }

            val connection = when (provider) {
                "gemini" -> openGemini(endpoint, model, key)
                "anthropic" -> openAnthropic(endpoint, key)
                else -> openOpenAiCompatible(endpoint, key)
            }
            connection.connectTimeout = preferences.requestTimeoutSeconds * 1000
            connection.readTimeout = preferences.requestTimeoutSeconds * 1000
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            val body = when (provider) {
                "gemini" -> geminiBody(model, prompt, history)
                "anthropic" -> anthropicBody(model, prompt, history)
                else -> openAiBody(model, prompt, history)
            }
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            connection.disconnect()
            if (status !in 200..299) {
                val detail = runCatching { JSONObject(response).optJSONObject("error")?.optString("message") }.getOrNull()
                throw IllegalStateException(detail?.takeIf { !it.isNullOrBlank() } ?: "Provider request failed (HTTP $status). Check the API key, model and endpoint.")
            }
            parseResponse(provider, response)
        }

    private fun openOpenAiCompatible(endpoint: String, key: String): HttpURLConnection {
        val url = URL(if (endpoint.endsWith("/chat/completions")) endpoint else "$endpoint/chat/completions")
        return (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("Authorization", "Bearer $key")
        }
    }

    private fun openAnthropic(endpoint: String, key: String): HttpURLConnection {
        val base = endpoint.ifBlank { "https://api.anthropic.com/v1" }
        val url = URL(if (base.endsWith("/messages")) base else "$base/messages")
        return (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("x-api-key", key)
            setRequestProperty("anthropic-version", "2023-06-01")
        }
    }

    private fun openGemini(endpoint: String, model: String, key: String): HttpURLConnection {
        val base = endpoint.ifBlank { "https://generativelanguage.googleapis.com/v1beta" }
        val url = URL("$base/models/$model:generateContent?key=$key")
        return url.openConnection() as HttpURLConnection
    }

    private fun openAiBody(model: String, prompt: String, history: List<AssistantChatTurn>) =
        JSONObject().put("model", model)
            .put("messages", JSONArray().apply {
                if (preferences.includeConversationContext) history.forEach {
                    put(JSONObject().put("role", it.role).put("content", it.content))
                }
                put(JSONObject().put("role", "user").put("content", prompt))
            })
            .put("max_tokens", preferences.maxOutputTokens)
            .put("temperature", preferences.temperature)

    private fun anthropicBody(model: String, prompt: String, history: List<AssistantChatTurn>) =
        JSONObject().put("model", model)
            .put("max_tokens", preferences.maxOutputTokens)
            .put("temperature", preferences.temperature)
            .put("messages", JSONArray().apply {
                if (preferences.includeConversationContext) history.forEach {
                    put(JSONObject().put("role", it.role).put("content", it.content))
                }
                put(JSONObject().put("role", "user").put("content", prompt))
            })

    private fun geminiBody(model: String, prompt: String, history: List<AssistantChatTurn>) =
        JSONObject().put("generationConfig", JSONObject()
            .put("maxOutputTokens", preferences.maxOutputTokens)
            .put("temperature", preferences.temperature))
            .put("contents", JSONArray().apply {
                if (preferences.includeConversationContext) history.forEach {
                    put(JSONObject().put("role", if (it.role == "assistant") "model" else "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", it.content))))
                }
                put(JSONObject().put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", prompt))))
            })

    private fun parseResponse(provider: String, raw: String): String {
        val json = JSONObject(raw)
        val text = when (provider) {
            "gemini" -> json.optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts")
                ?.let { parts -> (0 until parts.length()).mapNotNull { parts.optJSONObject(it)?.optString("text") }.joinToString("") }
            "anthropic" -> json.optJSONArray("content")
                ?.let { parts -> (0 until parts.length()).mapNotNull { parts.optJSONObject(it)?.optString("text") }.joinToString("") }
            else -> json.optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.optString("content")
        }
        return text?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("The provider returned an empty response.")
    }
}
