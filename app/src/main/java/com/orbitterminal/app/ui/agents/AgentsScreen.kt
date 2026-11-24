package com.orbitterminal.app.ui.agents

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orbitterminal.app.data.agent.AgentId
import com.orbitterminal.app.data.agent.AgentStatus
import com.orbitterminal.app.data.agent.AntigravityLoginState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentsScreen(viewModel: AgentsViewModel = hiltViewModel()) {
    val statuses by viewModel.agentStatuses.collectAsState()
    val antigravityState by viewModel.antigravityLoginState.collectAsState()
    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshStatuses()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CLI Agents") },
                actions = {
                    IconButton(onClick = { viewModel.refreshStatuses() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(AgentId.values()) { agentId ->
                val status = statuses[agentId] ?: AgentStatus.NotInstalled
                AgentCard(
                    agentId = agentId,
                    status = status,
                    onInstall = { viewModel.installAgent(agentId) },
                    onLaunch = { viewModel.launchAgent(agentId) },
                    onGoogleLogin = {
                        viewModel.startAntigravityLogin()
                        showTokenDialog = true
                    }
                )
            }
        }
    }

    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text("Paste Antigravity Auth Token") },
            text = {
                Column {
                    Text("Copy the auth token displayed in your Google login browser tab and paste it below:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Auth Token") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.primaryClip?.getItemAt(0)?.text?.let {
                                tokenInput = it.toString()
                            }
                        }
                    ) {
                        Text("Paste from Clipboard")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitAntigravityToken(tokenInput)
                        showTokenDialog = false
                    },
                    enabled = tokenInput.isNotBlank()
                ) {
                    Text("Verify & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AgentCard(
    agentId: AgentId,
    status: AgentStatus,
    onInstall: () -> Unit,
    onLaunch: () -> Unit,
    onGoogleLogin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(agentId.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(agentId.description, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            when (status) {
                is AgentStatus.NotInstalled -> {
                    Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Install ${agentId.displayName}")
                    }
                }
                is AgentStatus.Installing -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(status.progress, style = MaterialTheme.typography.bodySmall)
                    }
                }
                is AgentStatus.Installed -> {
                    Column {
                        Text("Version: ${status.version}", style = MaterialTheme.typography.labelSmall)
                        if (agentId == AgentId.ANTIGRAVITY) {
                            if (status.isLoggedIn) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Logged in as ${status.userEmail ?: "Google Account"}", style = MaterialTheme.typography.bodySmall)
                                }
                            } else {
                                OutlinedButton(onClick = onGoogleLogin, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    Text("Login with Google")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onLaunch, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Terminal, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch in Terminal")
                        }
                    }
                }
                is AgentStatus.Error -> {
                    Text("Error: ${status.message}", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
