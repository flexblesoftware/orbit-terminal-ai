package com.orbitterminal.app.data.provider

import com.orbitterminal.app.data.provider.base.AiProvider
import com.orbitterminal.app.data.provider.base.ProviderException
import com.orbitterminal.app.data.provider.gemini.GeminiProvider
import com.orbitterminal.app.data.provider.openai.OpenAiCompatProvider
import com.orbitterminal.app.data.security.EncryptedKeyStorage
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderFactory @Inject constructor(
    private val keyStorage: EncryptedKeyStorage
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun getProvider(providerId: String): AiProvider {
        return when (providerId) {
            "gemini" -> {
                val key = keyStorage.getKey(EncryptedKeyStorage.KEY_GEMINI_API)
                    ?: throw ProviderException("Gemini API key not set. Please set it in Settings.", isAuthError = true)
                GeminiProvider(key, client, json)
            }
            "groq" -> {
                val key = keyStorage.getKey(EncryptedKeyStorage.KEY_GROQ_API)
                    ?: throw ProviderException("Groq API key not set. Please set it in Settings.", isAuthError = true)
                OpenAiCompatProvider("groq", key, "https://api.groq.com/openai/v1", client, json)
            }
            "openrouter" -> {
                val key = keyStorage.getKey(EncryptedKeyStorage.KEY_OPENROUTER_API)
                    ?: throw ProviderException("OpenRouter API key not set. Please set it in Settings.", isAuthError = true)
                OpenAiCompatProvider(
                    "openrouter", key,
                    "https://openrouter.ai/api/v1", client, json,
                    extraHeaders = mapOf(
                        "HTTP-Referer" to "https://github.com/flexblesoftware/orbit-terminal-ai",
                        "X-Title" to "Orbit Terminal AI"
                    )
                )
            }
            "openai" -> {
                val key = keyStorage.getKey(EncryptedKeyStorage.KEY_OPENAI_API)
                    ?: throw ProviderException("OpenAI API key not set. Please set it in Settings.", isAuthError = true)
                OpenAiCompatProvider("openai", key, "https://api.openai.com/v1", client, json)
            }
            else -> throw ProviderException("Unknown provider: $providerId")
        }
    }
}
