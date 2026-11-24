package com.orbitterminal.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbitterminal.app.data.datastore.AppSettings
import com.orbitterminal.app.data.security.EncryptedKeyStorage
import com.orbitterminal.app.data.terminal.LinuxEnvironmentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val keyStorage: EncryptedKeyStorage,
    private val appSettings: AppSettings,
    private val envManager: LinuxEnvironmentManager
) : ViewModel() {

    val themeMode: Flow<String> = appSettings.themeMode
    val dynamicColor: Flow<Boolean> = appSettings.dynamicColor
    val terminalFontSize: Flow<Float> = appSettings.terminalFontSize
    val chatFontSize: Flow<Float> = appSettings.chatFontSize

    fun getKey(key: String): String? = keyStorage.getKey(key)

    fun saveKey(key: String, value: String) {
        if (value.isBlank()) keyStorage.removeKey(key)
        else keyStorage.storeKey(key, value.trim())
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { appSettings.setDynamicColor(enabled) }
    }

    fun setTerminalFontSize(size: Float) {
        viewModelScope.launch { appSettings.setTerminalFontSize(size) }
    }

    fun resetLinuxEnvironment() {
        viewModelScope.launch { envManager.resetEnvironment() }
    }
}
