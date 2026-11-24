package com.orbitterminal.app.data.provider.base

import com.orbitterminal.app.data.provider.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface AiProvider {
    val providerId: String

    fun streamChat(
        messages: List<ChatMessage>,
        modelId: String,
        systemPrompt: String? = null,
        tools: List<ToolDefinition>? = null
    ): Flow<StreamEvent>
}

sealed class StreamEvent {
    data class TextChunk(val text: String) : StreamEvent()
    data class ToolCall(val id: String, val name: String, val arguments: String) : StreamEvent()
    object Done : StreamEvent()
}

data class ToolDefinition(
    val name: String,
    val description: String,
    val parametersJson: String
)

class ProviderException(
    message: String,
    val code: Int = -1,
    val isRateLimit: Boolean = false,
    val isAuthError: Boolean = false,
    val isNetworkError: Boolean = false
) : Exception(message)
