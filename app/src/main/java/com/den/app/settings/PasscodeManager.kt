package com.den.app.settings

import com.den.app.crypto.AppCrypto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class PasscodeManager(
    private val settings: SettingsStore,
    private val crypto: AppCrypto,
) {
    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked

    suspend fun isEnabled(): Boolean {
        val s = settings.settings.first()
        return s.passcodeEnabled && !s.passcodeHash.isNullOrEmpty()
    }

    suspend fun verify(passcode: String): Boolean {
        val s = settings.settings.first()
        val hash = s.passcodeHash ?: return false
        return crypto.hashPasscode(passcode) == hash
    }

    suspend fun enable(passcode: String) {
        settings.setPasscodeHash(crypto.hashPasscode(passcode))
        settings.setPasscodeEnabled(true)
    }

    suspend fun disable() {
        settings.setPasscodeEnabled(false)
        settings.setPasscodeHash(null)
        _locked.value = false
    }

    fun unlock() {
        _locked.value = false
    }

    fun lockNow() {
        _locked.value = true
    }
}