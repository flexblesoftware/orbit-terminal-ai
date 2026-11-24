package com.orbitterminal.app.ui.github

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orbitterminal.app.ui.update.UpdateState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubScreen(viewModel: GitHubViewModel = hiltViewModel()) {
    val workflowRun by viewModel.latestRun.collectAsState()
    val isTriggering by viewModel.isTriggering.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("GitHub CI & Updates") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GitHub Actions Build", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    workflowRun?.let { run ->
                        Text("Status: ${run.status} (${run.conclusion ?: "in progress"})")
                        Text("Created at: ${run.createdAt}")
                    } ?: Text("No recent workflow runs found.")

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.triggerBuild() },
                        enabled = !isTriggering,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTriggering) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trigger Release Build")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("In-App Updates", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    when (val s = updateState) {
                        is UpdateState.Checking -> {
                            CircularProgressIndicator()
                            Text("Checking for updates...")
                        }
                        is UpdateState.UpdateAvailable -> {
                            Text("New Version: ${s.release.tagName}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.downloadUpdate(s.apkUrl, s.release.tagName) }) {
                                Text("Download Update")
                            }
                        }
                        is UpdateState.ReadyToInstall -> {
                            Text("Update downloaded successfully!")
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.installUpdate(s.apkFile) }) {
                                Text("Install APK Now")
                            }
                        }
                        else -> {
                            Text("App is up to date.")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { viewModel.checkForUpdates() }) {
                                Text("Check for Updates")
                            }
                        }
                    }
                }
            }
        }
    }
}
