package com.den.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.den.app.AppContainer
import com.den.app.ui.components.ChipItem
import com.den.app.ui.components.DenTopBar
import com.den.app.ui.components.FilterChipRow
import kotlinx.coroutines.delay

private enum class FocusMode(val label: String, val millis: Long) {
    FOCUS_25("Pomodoro", 25L * 60_000),
    DEEP_45("Deep", 45L * 60_000),
    QUICK_15("Quick", 15L * 60_000),
    STOPWATCH("Stopwatch", 0L),
}

private fun formatMillis(millis: Long): String {
    val totalSecs = (millis / 1000).coerceAtLeast(0L)
    val m = totalSecs / 60
    val s = totalSecs % 60
    return "%02d:%02d".format(m, s)
}

/** Minimal distraction-free focus session: timers + a stopwatch, tied to a task. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    container: AppContainer,
    taskId: Long,
    onBack: () -> Unit,
    onComplete: (Long) -> Unit,
) {
    val task by container.taskRepo.observeById(taskId).collectAsState(initial = null)

    var mode by remember { mutableStateOf(FocusMode.FOCUS_25) }
    var running by remember { mutableStateOf(false) }
    var endAt by remember { mutableLongStateOf(0L) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val countingDown = mode != FocusMode.STOPWATCH

    LaunchedEffect(running) {
        while (running) {
            now = System.currentTimeMillis()
            if (countingDown && now >= endAt) running = false
            delay(200)
        }
    }

    val finished = countingDown && endAt != 0L && now >= endAt
    val remaining = when {
        finished -> 0L
        countingDown && endAt == 0L -> mode.millis
        countingDown -> (endAt - now).coerceAtLeast(0L)
        else -> 0L
    }
    val elapsed = if (!countingDown && startedAt != 0L) (now - startedAt).coerceAtLeast(0L) else 0L
    val fresh = if (countingDown) endAt == 0L else startedAt == 0L
    val progress = when {
        !countingDown -> 0f
        endAt == 0L -> 0f
        else -> (1f - remaining.toFloat() / mode.millis).coerceIn(0f, 1f)
    }

    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    fun reset() {
        running = false
        endAt = 0L
        startedAt = 0L
        now = System.currentTimeMillis()
    }

    fun start() {
        if (countingDown) {
            if (endAt == 0L) endAt = System.currentTimeMillis() + mode.millis
        } else {
            if (startedAt == 0L) startedAt = System.currentTimeMillis()
        }
        running = true
    }

    Scaffold(
        topBar = {
            DenTopBar(
                title = "Focus",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            task?.let {
                Text(
                    text = it.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = mode.label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp,
            )

            Spacer(Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center) {
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(250),
                    label = "focusProgress",
                )
                val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                val progressColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.size(300.dp)) {
                    val stroke = 12.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (countingDown) formatMillis(remaining) else formatMillis(elapsed),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Light,
                        ),
                        fontSize = 72.sp,
                    )
                    Text(
                        text = when {
                            finished -> "Time's up"
                            running -> if (countingDown) "in flow" else "counting up"
                            else -> if (countingDown) "ready" else "press start"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            when {
                finished -> {
                    Button(
                        onClick = { onComplete(taskId) },
                        shape = CircleShape,
                        modifier = Modifier.size(88.dp),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Wrap up", modifier = Modifier.size(36.dp))
                    }
                    TextButton(onClick = { reset(); start() }) {
                        Text("Start again")
                    }
                }
                running -> {
                    Button(
                        onClick = { running = false },
                        shape = CircleShape,
                        modifier = Modifier.size(88.dp),
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = "Pause", modifier = Modifier.size(36.dp))
                    }
                    TextButton(onClick = { reset() }) {
                        Text("Reset")
                    }
                }
                else -> {
                    Button(
                        onClick = { start() },
                        shape = CircleShape,
                        modifier = Modifier.size(88.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Start", modifier = Modifier.size(36.dp))
                    }
                    TextButton(onClick = { reset() }, enabled = !fresh) {
                        Text("Reset")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            FilterChipRow(
                items = FocusMode.entries.map { m ->
                    ChipItem(
                        label = m.label,
                        selected = m == mode,
                        onClick = {
                            if (m != mode) {
                                reset()
                                mode = m
                            }
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}