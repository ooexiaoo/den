package com.den.app.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.den.app.AppContainer
import kotlinx.coroutines.launch

@Composable
fun PasscodeScreen(
    container: AppContainer,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var biometricAttempted by remember { mutableStateOf(false) }

    val activity = context as? FragmentActivity
    val canBiometric = activity != null &&
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS
    val showBiometric = canBiometric && biometricEnabled

    val prompt = remember(context, activity) {
        activity?.let { act ->
            val executor = ContextCompat.getMainExecutor(act)
            BiometricPrompt(
                act,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        scope.launch {
                            container.passcode.unlock()
                            onUnlocked()
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                            errorCode != BiometricPrompt.ERROR_USER_CANCELED
                        ) {
                            Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
            )
        }
    }

    LaunchedEffect(showBiometric, biometricAttempted) {
        if (showBiometric && !biometricAttempted) {
            biometricAttempted = true
            prompt?.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Den")
                    .setSubtitle("Use your fingerprint to unlock")
                    .setNegativeButtonText("Use passcode")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build()
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.align(Alignment.Center).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Den is locked",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Enter your passcode to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            if (showBiometric) {
                OutlinedButton(
                    onClick = {
                        prompt?.authenticate(
                            BiometricPrompt.PromptInfo.Builder()
                                .setTitle("Unlock Den")
                                .setSubtitle("Use your fingerprint to unlock")
                                .setNegativeButtonText("Use passcode")
                                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                                .build()
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock with fingerprint")
                }
                Spacer(Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = code,
                onValueChange = { input ->
                    code = input.filter(Char::isDigit).take(8)
                    error = false
                },
                label = { Text("Passcode") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            if (error) {
                Spacer(Modifier.height(8.dp))
                Text("Wrong passcode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        if (container.passcode.verify(code)) {
                            container.passcode.unlock()
                            onUnlocked()
                        } else {
                            error = true
                            code = ""
                        }
                    }
                },
            ) {
                Text("Unlock")
            }
        }
    }
}