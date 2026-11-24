package com.orbitterminal.app.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orbitterminal.app.data.terminal.SetupState
import com.orbitterminal.app.ui.theme.TerminalBackground
import com.orbitterminal.app.ui.theme.TerminalForeground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(viewModel: TerminalViewModel = hiltViewModel()) {
    val setupState by viewModel.setupState.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val activeSessionId by viewModel.activeSessionId.collectAsState()
    val activeSession = sessions.find { it.id == activeSessionId }
    val output by activeSession?.outputHistory?.collectAsState() ?: remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf("") }

    if (setupState !is SetupState.Ready && setupState !is SetupState.Idle) {
        TerminalSetupView(setupState = setupState, onRetry = { viewModel.startSetup() })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ScrollableTabRow(
                        selectedTabIndex = sessions.indexOfFirst { it.id == activeSessionId }.coerceAtLeast(0),
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 0.dp
                    ) {
                        sessions.forEach { s ->
                            Tab(
                                selected = s.id == activeSessionId,
                                onClick = { viewModel.selectSession(s.id) },
                                text = { Text(s.title, maxLines = 1) }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createSession() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Tab")
                    }
                    if (activeSession != null) {
                        IconButton(onClick = { viewModel.closeActiveSession() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Tab")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(TerminalBackground)
        ) {
            val scrollState = rememberScrollState()
            LaunchedEffect(output.length) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(8.dp)
            ) {
                Text(
                    text = if (output.isEmpty()) "Orbit Linux (Debian arm64)\nType a command or ask the AI to run one...\n" else output,
                    color = TerminalForeground,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }

            SpecialKeysBar(
                onKey = { key -> viewModel.sendSpecialKey(key) },
                onCtrlC = { viewModel.sendCtrlC() }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "root# ",
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = TerminalForeground,
                        unfocusedTextColor = TerminalForeground,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = { Text("Command...", color = Color.Gray) }
                )
                IconButton(
                    onClick = {
                        viewModel.sendCommand(inputText)
                        inputText = ""
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun SpecialKeysBar(onKey: (String) -> Unit, onCtrlC: () -> Unit) {
    val keys = listOf("TAB", "ESC", "CTRL", "ALT", "↑", "↓", "←", "→", "|", "~", "/", "-")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF14141E))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Button(
            onClick = onCtrlC,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.height(32.dp).padding(horizontal = 2.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text("Ctrl+C", fontSize = 11.sp)
        }
        keys.forEach { k ->
            OutlinedButton(
                onClick = { onKey(k) },
                modifier = Modifier.height(32.dp).padding(horizontal = 2.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(k, fontSize = 11.sp, color = TerminalForeground)
            }
        }
    }
}

@Composable
fun TerminalSetupView(setupState: SetupState, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Setting Up Linux Environment", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                when (setupState) {
                    is SetupState.Downloading -> {
                        LinearProgressIndicator(
                            progress = setupState.progress,
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(setupState.progress * 100).toInt()}% (${setupState.downloadedBytes / (1024 * 1024)} MB)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    is SetupState.Extracting -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(setupState.progressMessage, style = MaterialTheme.typography.bodySmall)
                    }
                    is SetupState.Error -> {
                        Text(setupState.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                    else -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Preparing root filesystem...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
