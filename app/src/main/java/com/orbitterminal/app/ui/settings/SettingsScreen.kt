package com.orbitterminal.app.ui.settings

import android.app.AlertDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orbitterminal.app.BuildConfig
import com.orbitterminal.app.data.security.EncryptedKeyStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val themeMode by viewModel.themeMode.collectAsState(initial = "dark")
    val dynamicColor by viewModel.dynamicColor.collectAsState(initial = true)
    val terminalFontSize by viewModel.terminalFontSize.collectAsState(initial = 14f)
    val chatFontSize by viewModel.chatFontSize.collectAsState(initial = 16f)

    var geminiKey by remember { mutableStateOf(viewModel.getKey(EncryptedKeyStorage.KEY_GEMINI_API) ?: "") }
    var groqKey by remember { mutableStateOf(viewModel.getKey(EncryptedKeyStorage.KEY_GROQ_API) ?: "") }
    var openRouterKey by remember { mutableStateOf(viewModel.getKey(EncryptedKeyStorage.KEY_OPENROUTER_API) ?: "") }
    var openAiKey by remember { mutableStateOf(viewModel.getKey(EncryptedKeyStorage.KEY_OPENAI_API) ?: "") }
    var githubToken by remember { mutableStateOf(viewModel.getKey(EncryptedKeyStorage.KEY_GITHUB_TOKEN) ?: "") }

    var showResetDialog by remember { mutableStateOf(false) }
    var savedSnackbar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("API Keys (Stored Encrypted)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            SecretKeyInput("Google Gemini API Key", geminiKey) { geminiKey = it; viewModel.saveKey(EncryptedKeyStorage.KEY_GEMINI_API, it) }
            Spacer(modifier = Modifier.height(8.dp))
            SecretKeyInput("Groq API Key", groqKey) { groqKey = it; viewModel.saveKey(EncryptedKeyStorage.KEY_GROQ_API, it) }
            Spacer(modifier = Modifier.height(8.dp))
            SecretKeyInput("OpenRouter API Key", openRouterKey) { openRouterKey = it; viewModel.saveKey(EncryptedKeyStorage.KEY_OPENROUTER_API, it) }
            Spacer(modifier = Modifier.height(8.dp))
            SecretKeyInput("OpenAI API Key", openAiKey) { openAiKey = it; viewModel.saveKey(EncryptedKeyStorage.KEY_OPENAI_API, it) }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("GitHub Integration", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            SecretKeyInput("GitHub Personal Access Token (repo, workflow)", githubToken) {
                githubToken = it
                viewModel.saveKey(EncryptedKeyStorage.KEY_GITHUB_TOKEN, it)
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Appearance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Dynamic Color", modifier = Modifier.weight(1f))
                Switch(checked = dynamicColor, onCheckedChange = { viewModel.setDynamicColor(it) })
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Terminal Font Size: ${terminalFontSize.toInt()} sp")
            Slider(
                value = terminalFontSize,
                onValueChange = { viewModel.setTerminalFontSize(it) },
                valueRange = 10f..24f,
                steps = 14
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Danger Zone", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showResetDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Linux Environment")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("About", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Orbit Terminal AI v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
            Text("Architecture: arm64-v8a only")
            Text("License: GNU General Public License v3.0 (GPL-3.0)")
            Text("GitHub: flexblesoftware/orbit-terminal-ai")
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Linux Environment?") },
            text = { Text("This will delete the entire Debian rootfs, all installed packages, and home directory. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetLinuxEnvironment()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SecretKeyInput(label: String, value: String, onValueChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide" else "Show"
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
