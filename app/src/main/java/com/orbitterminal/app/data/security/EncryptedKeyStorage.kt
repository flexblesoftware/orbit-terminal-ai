package com.orbitterminal.app.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted key-value storage backed by Android Keystore.
 * API keys, GitHub tokens, and agent tokens are stored here.
 * Keys are NEVER logged or returned as plain text outside this class.
 * Includes automatic recovery and fallback to ensure app never crashes on startup.
 */
@Singleton
class EncryptedKeyStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = initPreferences(context)

    private fun initPreferences(context: Context): SharedPreferences {
        return try {
            createEncryptedPrefs(context)
        } catch (e: Exception) {
            Timber.e(e, "EncryptedSharedPreferences creation failed, attempting wipe and recreate")
            try {
                context.deleteSharedPreferences(PREFS_NAME)
                createEncryptedPrefs(context)
            } catch (fallbackError: Exception) {
                Timber.e(fallbackError, "Fallback to standard private SharedPreferences")
                context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun storeKey(key: String, value: String) {
        try {
            prefs.edit().putString(key, value).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to store key: $key")
        }
    }

    fun getKey(key: String): String? {
        return try {
            prefs.getString(key, null)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get key: $key")
            null
        }
    }

    fun removeKey(key: String) {
        try {
            prefs.edit().remove(key).apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove key: $key")
        }
    }

    fun hasKey(key: String): Boolean {
        return try {
            prefs.contains(key)
        } catch (e: Exception) {
            false
        }
    }

    fun clearAll() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear all keys")
        }
    }

    companion object {
        private const val PREFS_NAME = "orbit_encrypted_keys"
        private const val FALLBACK_PREFS_NAME = "orbit_keys_fallback"

        // Key constants — never hardcode the values, only the key names
        const val KEY_GEMINI_API = "api_key_gemini"
        const val KEY_GROQ_API = "api_key_groq"
        const val KEY_OPENROUTER_API = "api_key_openrouter"
        const val KEY_OPENAI_API = "api_key_openai"
        const val KEY_GITHUB_TOKEN = "github_token"
        const val KEY_ANTIGRAVITY_TOKEN = "agent_token_antigravity"
        const val KEY_CLAUDE_CODE_KEY = "agent_key_claude_code"
        const val KEY_CODEX_KEY = "agent_key_codex"
    }
}
