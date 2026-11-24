package com.orbitterminal.app.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orbitterminal.app.data.db.entity.MessageEntity
import com.orbitterminal.app.data.provider.model.PROVIDERS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val currentChatId by viewModel.currentChatId.collectAsState()
    val currentChat by viewModel.currentChat.collectAsState(initial = null)
    val messages by viewModel.messages.collectAsState(initial = emptyList())
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingText by viewModel.streamingText.collectAsState()
    val activeTool by viewModel.activeToolRunning.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showModelSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            listState.animateScrollToItem((messages.size).coerceAtLeast(0))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentChat?.title ?: "Orbit Terminal AI",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                        currentChat?.let {
                            Text(
                                text = "${it.providerId} / ${it.modelId}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    AssistChip(
                        onClick = { showModelSheet = true },
                        label = { Text("Model") },
                        leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, Modifier.size(16.dp)) }
                    )
                    IconButton(onClick = { viewModel.startNewChat() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(msg)
                }
                if (isStreaming && streamingText.isNotEmpty()) {
                    item {
                        AssistantStreamingBubble(text = streamingText, toolRunning = activeTool)
                    }
                }
            }

            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                isStreaming = isStreaming,
                onStop = { viewModel.stopStreaming() }
            )
        }
    }

    if (showModelSheet) {
        ModalBottomSheet(onDismissRequest = { showModelSheet = false }) {
            ModelSelectionSheet(
                currentProvider = currentChat?.providerId ?: "gemini",
                currentModel = currentChat?.modelId ?: "gemini-2.0-flash",
                onSelect = { p, m ->
                    viewModel.switchModel(p, m)
                    showModelSheet = false
                }
            )
        }
    }
}

@Composable
fun MessageBubble(message: MessageEntity) {
    val isUser = message.role == "user"
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
                if (!isUser) {
                    Spacer(modifier = Modifier.height(4.dp))
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AI message", message.content))
                        },
                        modifier = Modifier.size(24.dp).align(Alignment.End)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AssistantStreamingBubble(text: String, toolRunning: String?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .widthIn(max = 320.dp)
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            toolRunning?.let {
                Text(
                    text = "⚙ $it",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = "$text ▋",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isStreaming: Boolean,
    onStop: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Message Orbit AI…") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isStreaming) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.error)
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ModelSelectionSheet(
    currentProvider: String,
    currentModel: String,
    onSelect: (providerId: String, modelId: String) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Select Provider & Model", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        PROVIDERS.forEach { provider ->
            Text(provider.displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            provider.models.forEach { model ->
                ListItem(
                    headlineContent = { Text(model.displayName) },
                    supportingContent = { Text(model.id) },
                    trailingContent = {
                        if (currentProvider == provider.id && currentModel == model.id) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(provider.id, model.id) },
                    colors = ListItemDefaults.colors()
                )
            }
            HorizontalDivider()
        }
    }
}
