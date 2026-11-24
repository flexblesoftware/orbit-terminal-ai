package com.orbitterminal.app.data.agent

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.orbitterminal.app.data.datastore.AppSettings
import com.orbitterminal.app.data.security.EncryptedKeyStorage
import com.orbitterminal.app.data.terminal.TerminalController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

sealed class AntigravityLoginState {
    object Idle : AntigravityLoginState()
    object StartingLogin : AntigravityLoginState()
    data class WaitingForUser(val authUrl: String) : AntigravityLoginState()
    object VerifyingToken : AntigravityLoginState()
    data class Success(val email: String) : AntigravityLoginState()
    data class Error(val error: String) : AntigravityLoginState()
}

@Singleton
class AntigravityLoginHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val terminalController: TerminalController,
    private val keyStorage: EncryptedKeyStorage,
    private val appSettings: AppSettings
) {
    private val _loginState = MutableStateFlow<AntigravityLoginState>(AntigravityLoginState.Idle)
    val loginState: StateFlow<AntigravityLoginState> = _loginState.asStateFlow()

    suspend fun startLoginFlow(): String? = withContext(Dispatchers.IO) {
        _loginState.value = AntigravityLoginState.StartingLogin
        try {
            // Run agy auth / login command
            val output = terminalController.runCommandAndCapture("agy auth login --no-browser 2>&1 || agy login 2>&1")
            
            // Extract URL from command output
            val urlPattern = Pattern.compile("https://[\\w\\-\\.]+\\.[a-z]{2,}(?:/[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]*)?")
            val matcher = urlPattern.matcher(output)
            val authUrl = if (matcher.find()) matcher.group(0) else null

            if (authUrl != null) {
                _loginState.value = AntigravityLoginState.WaitingForUser(authUrl)
                launchCustomTab(authUrl)
                return@withContext authUrl
            } else {
                _loginState.value = AntigravityLoginState.Error("Could not extract Google login URL. Output: $output")
                null
            }
        } catch (e: Exception) {
            _loginState.value = AntigravityLoginState.Error(e.localizedMessage ?: "Login failed")
            null
        }
    }

    private fun launchCustomTab(url: String) {
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch Custom Tab")
        }
    }

    suspend fun submitAuthToken(token: String): Boolean = withContext(Dispatchers.IO) {
        _loginState.value = AntigravityLoginState.VerifyingToken
        try {
            keyStorage.storeKey(EncryptedKeyStorage.KEY_ANTIGRAVITY_TOKEN, token)

            // Submit token to agy CLI non-interactively
            val submitOutput = terminalController.runCommandAndCapture("echo \"$token\" | agy auth paste-token 2>&1 || echo ok")

            // Check agy auth status
            val statusOutput = terminalController.runCommandAndCapture("agy auth status 2>&1 || agy whoami 2>&1")
            
            // Parse email from output: e.g. "Logged in as user@example.com"
            val emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
            val matcher = emailPattern.matcher(statusOutput)
            val email = if (matcher.find()) matcher.group(0) else "User"

            if (statusOutput.contains("Logged in", ignoreCase = true) || matcher.find()) {
                appSettings.setAntigravityEmail(email)
                _loginState.value = AntigravityLoginState.Success(email)
                true
            } else {
                _loginState.value = AntigravityLoginState.Error("Invalid or expired token. Please retry.")
                false
            }
        } catch (e: Exception) {
            _loginState.value = AntigravityLoginState.Error(e.localizedMessage ?: "Token verification failed")
            false
        }
    }

    suspend fun checkStatus(): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            val output = terminalController.runCommandAndCapture("agy auth status 2>&1 || agy whoami 2>&1")
            val emailPattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
            val matcher = emailPattern.matcher(output)
            if (matcher.find()) {
                val email = matcher.group(0)
                appSettings.setAntigravityEmail(email)
                Pair(true, email)
            } else {
                Pair(false, null)
            }
        } catch (_: Exception) {
            Pair(false, null)
        }
    }

    fun resetState() {
        _loginState.value = AntigravityLoginState.Idle
    }
}
