package com.den.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.den.app.data.model.Label
import com.den.app.data.model.Task
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Swipeable task row: swipe right to complete/reopen, swipe left to open the
 * context menu (reschedule etc). Slow drags reveal tinted action panels.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableTaskRow(
    task: Task,
    labels: List<Label>,
    subtasksDone: Int,
    subtasksTotal: Int,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    onOpenActions: () -> Unit,
) {
    val actionWidth = 76.dp
    val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    val currentOnComplete by rememberUpdatedState(onComplete)
    val currentOnOpenActions by rememberUpdatedState(onOpenActions)

    val settle = {
        val v = offset.value
        scope.launch {
            when {
                v > actionWidthPx * 0.45f -> {
                    offset.animateTo(actionWidthPx, tween(160))
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnComplete()
                    offset.animateTo(0f, tween(220))
                }
                v < -actionWidthPx * 0.45f -> {
                    offset.animateTo(-actionWidthPx, tween(160))
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnOpenActions()
                    offset.animateTo(0f, tween(220))
                }
                else -> offset.animateTo(0f, tween(220))
            }
        }
    }

    val completeColor = MaterialTheme.colorScheme.primary
    val completeLabel = if (task.completed) "Reopen" else "Complete"

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterStart) {
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    .background(completeColor),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = completeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Schedule",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiary,
                    )
                }
            }
        }
        TaskRowContent(
            task = task,
            labels = labels,
            subtasksDone = subtasksDone,
            subtasksTotal = subtasksTotal,
            onCheck = onComplete,
            modifier = Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.background)
                .combinedClickable(onClick = onClick, onLongClick = onOpenActions)
                .pointerInput(task.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offset.snapTo((offset.value + dragAmount).coerceIn(-actionWidthPx * 2f, actionWidthPx * 2f))
                            }
                        },
                        onDragEnd = { settle() },
                        onDragCancel = { scope.launch { offset.animateTo(0f, tween(220)) } },
                    )
                },
        )
    }
}