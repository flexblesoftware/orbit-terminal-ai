package com.orbitterminal.app.ui.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.orbitterminal.app.BuildConfig
import com.orbitterminal.app.data.github.GitHubClient
import com.orbitterminal.app.data.github.GitHubRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object NoUpdate : UpdateState()
    data class UpdateAvailable(val release: GitHubRelease, val apkUrl: String) : UpdateState()
    data class Downloading(val downloadId: Long, val progress: Int) : UpdateState()
    data class ReadyToInstall(val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHubClient: GitHubClient
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var currentDownloadId: Long = -1L

    suspend fun checkForUpdates(owner: String = BuildConfig.GITHUB_REPO_OWNER, repo: String = BuildConfig.GITHUB_REPO_NAME) {
        _updateState.value = UpdateState.Checking
        val release = gitHubClient.getLatestRelease(owner, repo)
        if (release == null) {
            _updateState.value = UpdateState.NoUpdate
            return
        }

        val currentVersion = "v${BuildConfig.VERSION_NAME}"
        if (isNewerVersion(release.tagName, currentVersion)) {
            val apkAsset = release.assets.find { it.name.endsWith(".apk") }
            if (apkAsset != null) {
                _updateState.value = UpdateState.UpdateAvailable(release, apkAsset.downloadUrl)
            } else {
                _updateState.value = UpdateState.NoUpdate
            }
        } else {
            _updateState.value = UpdateState.NoUpdate
        }
    }

    fun startDownload(apkUrl: String, version: String): Long {
        val fileName = "orbit-terminal-ai-$version.apk"
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Orbit Terminal AI $version")
            .setDescription("Downloading update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = dm.enqueue(request)
        currentDownloadId = id
        _updateState.value = UpdateState.Downloading(id, 0)
        return id
    }

    fun onDownloadComplete(downloadId: Long) {
        if (downloadId != currentDownloadId) return
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = dm.query(query)
        if (cursor != null && cursor.moveToFirst()) {
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val uriString = cursor.getString(uriIndex)
                val file = File(Uri.parse(uriString).path ?: "")
                _updateState.value = UpdateState.ReadyToInstall(file)
            } else {
                _updateState.value = UpdateState.Error("Download failed.")
            }
            cursor.close()
        }
    }

    fun installApk(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(installIntent)
    }

    private fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
        val remoteClean = remoteTag.removePrefix("v").trim()
        val localClean = localVersion.removePrefix("v").trim()
        return remoteClean != localClean
    }
}
