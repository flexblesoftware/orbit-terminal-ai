package com.orbitterminal.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbitterminal.app.data.db.dao.ChatDao
import com.orbitterminal.app.data.db.dao.MessageDao
import com.orbitterminal.app.data.db.entity.ChatEntity
import com.orbitterminal.app.data.db.entity.MessageEntity
import com.orbitterminal.app.data.provider.ProviderFactory
import com.orbitterminal.app.data.provider.ToolBridge
import com.orbitterminal.app.data.provider.base.StreamEvent
import com.orbitterminal.app.data.provider.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val providerFactory: ProviderFactory,
    private val toolBridge: ToolBridge
) : ViewModel() {

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()

    val chats: Flow<List<ChatEntity>> = chatDao.getAllChats()

    val currentChat: Flow<ChatEntity?> = _currentChatId.flatMapLatest { id ->
        if (id == null) flowOf(null) else flow { emit(chatDao.getChatById(id)) }
    }

    val messages: Flow<List<MessageEntity>> = _currentChatId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else messageDao.getMessagesForChat(id)
    }

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _activeToolRunning = MutableStateFlow<String?>(null)
    val activeToolRunning: StateFlow<String?> = _activeToolRunning.asStateFlow()

    private var streamingJob: Job? = null

    fun selectChat(id: String) {
        _currentChatId.value = id
    }

    fun startNewChat(providerId: String = "gemini", modelId: String = "gemini-2.0-flash") {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val newChat = ChatEntity(
                id = id,
                title = "New Chat",
                providerId = providerId,
                modelId = modelId
            )
            chatDao.insertChat(newChat)
            _currentChatId.value = id
        }
    }

    fun switchModel(providerId: String, modelId: String) {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            chatDao.updateChatModel(chatId, providerId, modelId)
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val chatId = _currentChatId.value ?: run {
            startNewChat()
            return
        }

        viewModelScope.launch {
            val userMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                role = "user",
                content = content
            )
            messageDao.insertMessage(userMsg)

            // Auto-rename chat if first user message
            val chat = chatDao.getChatById(chatId)
            if (chat != null && chat.title == "New Chat") {
                val title = if (content.length > 30) content.take(30) + "…" else content
                chatDao.renameChat(chatId, title)
            }

            executeAssistantReply(chatId, chat?.providerId ?: "gemini", chat?.modelId ?: "gemini-2.0-flash")
        }
    }

    private fun executeAssistantReply(chatId: String, providerId: String, modelId: String) {
        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            _isStreaming.value = true
            _streamingText.value = ""

            try {
                val dbMessages = messageDao.getRecentMessages(chatId, 20).reversed()
                val chatMessages = dbMessages.map {
                    ChatMessage(role = it.role, content = it.content)
                }

                val provider = providerFactory.getProvider(providerId)
                val fullResponse = StringBuilder()

                provider.streamChat(
                    messages = chatMessages,
                    modelId = modelId,
                    tools = toolBridge.availableTools
                ).collect { event ->
                    when (event) {
                        is StreamEvent.TextChunk -> {
                            fullResponse.append(event.text)
                            _streamingText.value = fullResponse.toString()
                        }
                        is StreamEvent.ToolCall -> {
                            _activeToolRunning.value = "Running command: ${event.name}..."
                            val toolResult = toolBridge.executeTool(event.name, event.arguments)
                            _activeToolRunning.value = null

                            fullResponse.append("\n\n*Tool Output (${event.name})*:\n```\n$toolResult\n```\n")
                            _streamingText.value = fullResponse.toString()
                        }
                        is StreamEvent.Done -> {}
                    }
                }

                messageDao.insertMessage(
                    MessageEntity(
                        id = UUID.randomUUID().toString(),
                        chatId = chatId,
                        role = "assistant",
                        content = fullResponse.toString(),
                        providerId = providerId,
                        modelId = modelId
                    )
                )
            } catch (e: Exception) {
                messageDao.insertMessage(
                    MessageEntity(
                        id = UUID.randomUUID().toString(),
                        chatId = chatId,
                        role = "assistant",
                        content = "Error: ${e.message}",
                        isError = true
                    )
                )
            } finally {
                _isStreaming.value = false
                _streamingText.value = ""
                _activeToolRunning.value = null
            }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        _isStreaming.value = false
        val current = _streamingText.value
        val chatId = _currentChatId.value
        if (current.isNotBlank() && chatId != null) {
            viewModelScope.launch {
                messageDao.insertMessage(
                    MessageEntity(
                        id = UUID.randomUUID().toString(),
                        chatId = chatId,
                        role = "assistant",
                        content = "$current\n\n*(Stopped)*"
                    )
                )
                _streamingText.value = ""
            }
        }
    }

    fun deleteChat(id: String) {
        viewModelScope.launch {
            chatDao.deleteChatById(id)
            if (_currentChatId.value == id) {
                _currentChatId.value = null
            }
        }
    }

    fun togglePin(id: String, currentPin: Boolean) {
        viewModelScope.launch {
            chatDao.setPinned(id, !currentPin)
        }
    }
}
