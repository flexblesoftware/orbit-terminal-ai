package com.orbitterminal.app.data.provider.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ChatMessage(
    val role: String,
    val content: String,
    val toolCallId: String? = null,
    val toolName: String? = null
)

data class Provider(
    val id: String,
    val displayName: String,
    val models: List<AiModel>
)

data class AiModel(
    val id: String,
    val displayName: String,
    val contextWindow: Int = 128000,
    val supportsTools: Boolean = true,
    val supportsVision: Boolean = false
)

val PROVIDERS = listOf(
    Provider(
        id = "gemini",
        displayName = "Google Gemini",
        models = listOf(
            AiModel("gemini-2.0-flash", "Gemini 2.0 Flash", 1048576, true, true),
            AiModel("gemini-2.0-flash-thinking-exp", "Gemini 2.0 Flash Thinking", 32768, false, false),
            AiModel("gemini-1.5-pro", "Gemini 1.5 Pro", 2097152, true, true),
            AiModel("gemini-1.5-flash", "Gemini 1.5 Flash", 1048576, true, true)
        )
    ),
    Provider(
        id = "groq",
        displayName = "Groq",
        models = listOf(
            AiModel("llama-3.3-70b-versatile", "LLaMA 3.3 70B", 128000, true, false),
            AiModel("llama-3.1-8b-instant", "LLaMA 3.1 8B Instant", 128000, true, false),
            AiModel("mixtral-8x7b-32768", "Mixtral 8x7B", 32768, false, false),
            AiModel("gemma2-9b-it", "Gemma 2 9B", 8192, false, false)
        )
    ),
    Provider(
        id = "openrouter",
        displayName = "OpenRouter",
        models = listOf(
            AiModel("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", 200000, true, true),
            AiModel("anthropic/claude-3-haiku", "Claude 3 Haiku", 200000, true, false),
            AiModel("openai/gpt-4o", "GPT-4o", 128000, true, true),
            AiModel("openai/gpt-4o-mini", "GPT-4o Mini", 128000, true, true),
            AiModel("google/gemini-2.0-flash-001", "Gemini 2.0 Flash (OR)", 1048576, true, true),
            AiModel("meta-llama/llama-3.3-70b-instruct", "LLaMA 3.3 70B (OR)", 128000, true, false)
        )
    ),
    Provider(
        id = "openai",
        displayName = "OpenAI",
        models = listOf(
            AiModel("gpt-4o", "GPT-4o", 128000, true, true),
            AiModel("gpt-4o-mini", "GPT-4o Mini", 128000, true, true),
            AiModel("gpt-4-turbo", "GPT-4 Turbo", 128000, true, true),
            AiModel("o1-mini", "o1-mini", 128000, false, false)
        )
    )
)

fun getProvider(id: String) = PROVIDERS.find { it.id == id }
fun getModel(providerId: String, modelId: String) =
    getProvider(providerId)?.models?.find { it.id == modelId }
