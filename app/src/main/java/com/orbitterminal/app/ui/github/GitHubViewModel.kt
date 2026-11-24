package com.orbitterminal.app.ui.github

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbitterminal.app.BuildConfig
import com.orbitterminal.app.data.github.GitHubClient
import com.orbitterminal.app.data.github.WorkflowRun
import com.orbitterminal.app.ui.update.UpdateManager
import com.orbitterminal.app.ui.update.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class GitHubViewModel @Inject constructor(
    private val gitHubClient: GitHubClient,
    private val updateManager: UpdateManager
) : ViewModel() {

    private val _latestRun = MutableStateFlow<WorkflowRun?>(null)
    val latestRun: StateFlow<WorkflowRun?> = _latestRun.asStateFlow()

    private val _isTriggering = MutableStateFlow(false)
    val isTriggering: StateFlow<Boolean> = _isTriggering.asStateFlow()

    val updateState: StateFlow<UpdateState> = updateManager.updateState

    init {
        loadLatestRun()
    }

    fun loadLatestRun() {
        viewModelScope.launch {
            val run = gitHubClient.getLatestWorkflowRun(
                BuildConfig.GITHUB_REPO_OWNER,
                BuildConfig.GITHUB_REPO_NAME,
                "build-apk.yml"
            )
            _latestRun.value = run
        }
    }

    fun triggerBuild() {
        viewModelScope.launch {
            _isTriggering.value = true
            gitHubClient.triggerWorkflow(
                BuildConfig.GITHUB_REPO_OWNER,
                BuildConfig.GITHUB_REPO_NAME,
                "build-apk.yml"
            )
            _isTriggering.value = false
            loadLatestRun()
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates()
        }
    }

    fun downloadUpdate(apkUrl: String, version: String) {
        updateManager.startDownload(apkUrl, version)
    }

    fun installUpdate(file: File) {
        updateManager.installApk(file)
    }
}
