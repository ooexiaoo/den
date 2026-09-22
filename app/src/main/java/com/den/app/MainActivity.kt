package com.den.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.den.app.settings.Settings
import com.den.app.ui.nav.AppNav
import com.den.app.ui.screens.OnboardingScreen
import com.den.app.ui.screens.PasscodeScreen
import com.den.app.ui.theme.DenTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val container = AppGraph.container ?: error("AppContainer not initialized")

        val initialTaskId = intent.getLongExtra("task_id", -1L).takeIf { it != -1L }
        intent.removeExtra("task_id")

        setContent {
            var settings by remember { mutableStateOf(Settings()) }
            LaunchedEffect(container) {
                container.settings.settings.collect { settings = it }
            }
            DenTheme(settings) {
                MainGate(
                    container = container,
                    settings = settings,
                    initialTaskId = initialTaskId,
                    onInitialTaskHandled = { },
                )
            }
        }
    }
}

@Composable
private fun MainGate(
    container: AppContainer,
    settings: Settings,
    initialTaskId: Long?,
    onInitialTaskHandled: () -> Unit,
) {
    val passcodeEnabled = settings.passcodeEnabled && !settings.passcodeHash.isNullOrEmpty()
    var unlocked by remember { mutableStateOf(!passcodeEnabled) }
    var handledDeepLink by remember { mutableStateOf(initialTaskId == null) }

    LaunchedEffect(passcodeEnabled) {
        if (!passcodeEnabled) unlocked = true
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(passcodeEnabled, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && passcodeEnabled) {
                unlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when {
        !settings.onboardingDone -> OnboardingScreen(container) { }
        passcodeEnabled && !unlocked -> PasscodeScreen(container, settings.biometricEnabled) { unlocked = true }
        else -> AppNav(
            container = container,
            initialTaskId = if (handledDeepLink) null else initialTaskId,
            onInitialTaskHandled = {
                handledDeepLink = true
                onInitialTaskHandled()
            },
        )
    }

    val context = LocalContext.current
    var crashReport by rememberSaveable { mutableStateOf<String?>(CrashReporter.consumeLast(context)) }
    crashReport?.let { text ->
        AlertDialog(
            onDismissRequest = { crashReport = null },
            title = { Text("Den crashed last time") },
            text = {
                SelectionContainer {
                    Text(
                        text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        copyTextToClipboard(context, text)
                        crashReport = null
                    }
                ) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { crashReport = null }) {
                    Text("Dismiss")
                }
            },
        )
    }
}

private fun copyTextToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Den crash log", text))
    Toast.makeText(context, "Crash log copied", Toast.LENGTH_SHORT).show()
}