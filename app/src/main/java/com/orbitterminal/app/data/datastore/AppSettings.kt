package com.orbitterminal.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "orbit_settings")

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_PROVIDER = stringPreferencesKey("default_provider")
        val DEFAULT_MODEL = stringPreferencesKey("default_model")
        val CHAT_FONT_SIZE = floatPreferencesKey("chat_font_size")
        val TERMINAL_FONT_SIZE = floatPreferencesKey("terminal_font_size")
        val ROOTFS_DOWNLOADED = booleanPreferencesKey("rootfs_downloaded")
        val ROOTFS_VERSION = stringPreferencesKey("rootfs_version")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val GITHUB_OWNER = stringPreferencesKey("github_owner")
        val GITHUB_REPO = stringPreferencesKey("github_repo")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val ANTIGRAVITY_LOGGED_IN_EMAIL = stringPreferencesKey("antigravity_email")
        val CLAUDE_CODE_INSTALLED = booleanPreferencesKey("claude_code_installed")
        val CODEX_INSTALLED = booleanPreferencesKey("codex_installed")
        val ANTIGRAVITY_INSTALLED = booleanPreferencesKey("antigravity_installed")
    }

    val onboardingComplete: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[ONBOARDING_COMPLETE] ?: false }

    val themeMode: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[THEME_MODE] ?: "system" }

    val dynamicColor: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[DYNAMIC_COLOR] ?: true }

    val defaultProvider: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[DEFAULT_PROVIDER] ?: "gemini" }

    val defaultModel: Flow<String> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[DEFAULT_MODEL] ?: "gemini-2.0-flash" }

    val rootfsDownloaded: Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[ROOTFS_DOWNLOADED] ?: false }

    val terminalFontSize: Flow<Float> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[TERMINAL_FONT_SIZE] ?: 14f }

    val chatFontSize: Flow<Float> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[CHAT_FONT_SIZE] ?: 16f }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setDefaultProvider(providerId: String) {
        dataStore.edit { it[DEFAULT_PROVIDER] = providerId }
    }

    suspend fun setDefaultModel(modelId: String) {
        dataStore.edit { it[DEFAULT_MODEL] = modelId }
    }

    suspend fun setRootfsDownloaded(downloaded: Boolean) {
        dataStore.edit { it[ROOTFS_DOWNLOADED] = downloaded }
    }

    suspend fun setTerminalFontSize(size: Float) {
        dataStore.edit { it[TERMINAL_FONT_SIZE] = size }
    }

    suspend fun setRootfsVersion(version: String) {
        dataStore.edit { it[ROOTFS_VERSION] = version }
    }

    suspend fun setAntigravityEmail(email: String?) {
        dataStore.edit {
            if (email != null) it[ANTIGRAVITY_LOGGED_IN_EMAIL] = email
            else it.remove(ANTIGRAVITY_LOGGED_IN_EMAIL)
        }
    }

    suspend fun setAgentInstalled(agentId: String, installed: Boolean) {
        dataStore.edit {
            when (agentId) {
                "antigravity" -> it[ANTIGRAVITY_INSTALLED] = installed
                "claude_code" -> it[CLAUDE_CODE_INSTALLED] = installed
                "codex" -> it[CODEX_INSTALLED] = installed
            }
        }
    }
}
