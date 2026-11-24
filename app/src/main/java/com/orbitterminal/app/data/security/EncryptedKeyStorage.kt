package com.orbitterminal.app.data.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted key-value storage backed by Android Keystore.
 * API keys, GitHub tokens, and agent tokens are stored here.
 * Keys are NEVER logged or returned as plain text outside this class.
 */
@Singleton
class EncryptedKeyStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "orbit_encrypted_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun storeKey(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getKey(key: String): String? = prefs.getString(key, null)

    fun removeKey(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun hasKey(key: String): Boolean = prefs.contains(key)

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
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
