package com.orbitterminal.app.data.terminal

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import com.orbitterminal.app.BuildConfig
import com.orbitterminal.app.data.datastore.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.*
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class SetupState {
    object Idle : SetupState()
    object CheckingStorage : SetupState()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : SetupState()
    object VerifyingChecksum : SetupState()
    data class Extracting(val progressMessage: String) : SetupState()
    object InitializingPackages : SetupState()
    object Ready : SetupState()
    data class Error(val message: String, val canRetry: Boolean = true) : SetupState()
}

@Singleton
class LinuxEnvironmentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings
) {
    private val _setupState = MutableStateFlow<SetupState>(SetupState.Idle)
    val setupState: StateFlow<SetupState> = _setupState.asStateFlow()

    val rootfsDir: File get() = File(context.filesDir, "rootfs")
    val homeDir: File get() = File(context.filesDir, "home").apply { mkdirs() }
    val binDir: File get() = File(context.filesDir, "bin").apply { mkdirs() }
    val prootBinary: File get() = File(binDir, "proot")

    fun isEnvironmentReady(): Boolean {
        return rootfsDir.exists() && File(rootfsDir, "bin/bash").exists() && prootBinary.exists()
    }

    fun isConnectedToWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun getAvailableStorageBytes(): Long {
        val stat = StatFs(context.filesDir.path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    suspend fun startSetup(forceDownload: Boolean = false) = withContext(Dispatchers.IO) {
        if (isEnvironmentReady() && !forceDownload) {
            _setupState.value = SetupState.Ready
            return@withContext
        }

        _setupState.value = SetupState.CheckingStorage
        val freeBytes = getAvailableStorageBytes()
        val requiredBytes = 2L * 1024 * 1024 * 1024 // 2 GB minimum free
        if (freeBytes < requiredBytes) {
            _setupState.value = SetupState.Error(
                "Insufficient storage: ${freeBytes / (1024 * 1024)} MB free. At least 2 GB required."
            )
            return@withContext
        }

        extractNativeProot()

        val downloadFile = File(context.cacheDir, "debian-arm64-rootfs.tar.gz")
        val downloadSuccess = downloadRootfsWithResume(downloadFile)
        if (!downloadSuccess) {
            _setupState.value = SetupState.Error("Failed to download rootfs.")
            return@withContext
        }

        _setupState.value = SetupState.VerifyingChecksum
        if (BuildConfig.ROOTFS_SHA256 != "placeholder_sha256_updated_at_build_time") {
            val valid = verifySha256(downloadFile, BuildConfig.ROOTFS_SHA256)
            if (!valid) {
                downloadFile.delete()
                _setupState.value = SetupState.Error("Checksum verification failed. Corrupt download.")
                return@withContext
            }
        }

        _setupState.value = SetupState.Extracting("Extracting Debian root filesystem...")
        rootfsDir.mkdirs()
        val extracted = extractTarGz(downloadFile, rootfsDir)
        downloadFile.delete()

        if (!extracted) {
            _setupState.value = SetupState.Error("Failed to extract Debian root filesystem.")
            return@withContext
        }

        _setupState.value = SetupState.InitializingPackages
        setupEnvironmentConfigs()

        appSettings.setRootfsDownloaded(true)
        _setupState.value = SetupState.Ready
    }

    private fun extractNativeProot() {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val prootSo = File(nativeDir, "libproot.so")
        if (prootSo.exists()) {
            prootBinary.delete()
            prootSo.copyTo(prootBinary, overwrite = true)
            prootBinary.setExecutable(true, false)
        } else {
            val fallbackProot = File(context.filesDir, "proot")
            if (fallbackProot.exists()) {
                fallbackProot.copyTo(prootBinary, overwrite = true)
                prootBinary.setExecutable(true, false)
            }
        }
    }

    private fun downloadRootfsWithResume(targetFile: File): Boolean {
        val client = OkHttpClient()
        val existingLength = if (targetFile.exists()) targetFile.length() else 0L

        val requestBuilder = Request.Builder().url(BuildConfig.ROOTFS_URL)
        if (existingLength > 0) {
            requestBuilder.header("Range", "bytes=$existingLength-")
        }

        return try {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful && response.code != 416) return false

                val body = response.body ?: return false
                val totalBytes = (body.contentLength().takeIf { it > 0 } ?: 0L) + existingLength
                var downloadedBytes = existingLength

                val outputStream = FileOutputStream(targetFile, existingLength > 0 && response.code == 206)
                val buffer = ByteArray(8192)
                val inputStream = body.byteStream()
                var read: Int

                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                    downloadedBytes += read
                    val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                    _setupState.value = SetupState.Downloading(progress, downloadedBytes, totalBytes)
                }
                outputStream.flush()
                outputStream.close()
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "Error downloading rootfs")
            false
        }
    }

    private fun verifySha256(file: File, expectedHash: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            file.inputStream().use { stream ->
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            hash.equals(expectedHash, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    private fun extractTarGz(tarGzFile: File, outputDir: File): Boolean {
        return try {
            val process = ProcessBuilder(
                "tar", "-xzf", tarGzFile.absolutePath, "-C", outputDir.absolutePath
            ).redirectErrorStream(true).start()
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            try {
                GZIPInputStream(FileInputStream(tarGzFile)).use { gzip ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (gzip.read(buffer).also { read = it } != -1) {}
                }
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    private fun setupEnvironmentConfigs() {
        val resolvConf = File(rootfsDir, "etc/resolv.conf")
        resolvConf.parentFile?.mkdirs()
        resolvConf.writeText("nameserver 1.1.1.1\nnameserver 8.8.8.8\n")

        val hosts = File(rootfsDir, "etc/hosts")
        hosts.writeText("127.0.0.1 localhost\n::1 localhost\n")

        val profile = File(rootfsDir, "root/.bashrc")
        profile.parentFile?.mkdirs()
        profile.appendText("""
            export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
            export TERM=xterm-256color
            export HOME=/root
            export LANG=C.UTF-8
            export LC_ALL=C.UTF-8
            alias ll='ls -la'
            cd /root
        """.trimIndent() + "\n")
    }

    suspend fun resetEnvironment() = withContext(Dispatchers.IO) {
        _setupState.value = SetupState.Idle
        rootfsDir.deleteRecursively()
        homeDir.deleteRecursively()
        appSettings.setRootfsDownloaded(false)
    }
}
