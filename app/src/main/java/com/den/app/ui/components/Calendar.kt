package com.den.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.den.app.util.Dates
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthCalendar(
    month: LocalDate,
    selected: LocalDate,
    today: LocalDate,
    taskDays: Set<Long>,
    onSelect: (LocalDate) -> Unit,
    onShiftMonth: (Long) -> Unit,
) {
    val grid = remember(month) { Dates.monthGrid(month.year, month.monthValue - 1) }
    val weeks = grid.chunked(7)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onShiftMonth(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
            }
            Text(
                text = Dates.monthLabel(month.year, month.monthValue - 1),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { onShiftMonth(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            (DayOfWeek.MONDAY.value..DayOfWeek.MONDAY.value + 6).forEach { v ->
                val dow = DayOfWeek.of(((v - 1) % 7) + 1)
                Text(
                    text = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()).take(1),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        inMonth = Dates.isSameMonth(date, month.year, month.monthValue - 1),
                        isToday = date == today,
                        isSelected = date == selected,
                        hasTasks = Dates.dayMillis(date) in taskDays,
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    hasTasks: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                CircleShape,
                            )
                        } else Modifier
                    )
                    .then(
                        if (isToday && !isSelected) {
                            Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = when {
                            !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .size(if (hasTasks) 5.dp else 0.dp)
                    .background(
                        if (inMonth) MaterialTheme.colorScheme.primary else Color.Transparent,
                        CircleShape,
                    ),
            )
        }
    }
}