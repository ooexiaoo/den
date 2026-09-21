package com.den.app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
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
}