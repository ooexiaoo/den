package com.den.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.weight
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.runtime.Composable
import com.den.app.AppGraph
import com.den.app.MainActivity
import com.den.app.data.model.Task
import com.den.app.util.Dates
import androidx.glance.unit.dp
import androidx.glance.unit.sp

private val WidgetBg = ColorProvider(0xFF1B1B1F)
private val WidgetFg = ColorProvider(0xFFF4F4F5)
private val WidgetMuted = ColorProvider(0xFFB0B0B6)
private val WidgetAccent = ColorProvider(0xFF8AB4F8)
private val WidgetDone = ColorProvider(0xFFEDEDED)

class TasksWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasks = loadTodayTasks()
        provideContent {
            WidgetContent(tasks = tasks, onOpenApp = actionStartActivity<MainActivity>())
        }
    }

    private suspend fun loadTodayTasks(): List<Task> {
        val container = AppGraph.container ?: return emptyList()
        val now = Dates.startOfDay(Dates.now())
        return container.taskRepo.allTasks()
            .asSequence()
            .filter { !it.completed && !it.archived }
            .filter { it.dueAt == null || Dates.startOfDay(it.dueAt) <= now }
            .sortedWith(
                compareByDescending<Task> { it.priority }
                    .thenBy { it.dueAt ?: Long.MAX_VALUE }
                    .thenBy { it.createdAt }
            )
            .take(8)
            .toList()
    }

    companion object {
        val TaskIdKey = ActionParameters.Key<Long>("den_task_id")
    }
}

class TasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TasksWidget()
}

@Composable
private fun WidgetContent(tasks: List<Task>, onOpenApp: () -> Unit) {
    val today = Dates.startOfDay(Dates.now())
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(WidgetBg)
            .padding(16.dp),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth().clickable(onOpenApp)) {
            Column(modifier = GlanceModifier.weight(1f)) {
                Text(
                    text = "Today",
                    style = TextStyle(color = WidgetFg, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "${tasks.size} open",
                    style = TextStyle(color = WidgetMuted, fontSize = 12.sp),
                )
            }
            Text(
                text = Dates.formatDateShort(today).take(6),
                style = TextStyle(color = WidgetAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold),
            )
        }
        if (tasks.isEmpty()) {
            Spacer(GlanceModifier.height(16.dp))
            Text(
                text = "Nothing due. Tap to add a task.",
                style = TextStyle(color = WidgetMuted, fontSize = 13.sp),
                modifier = GlanceModifier.fillMaxWidth().clickable(onOpenApp),
            )
        } else {
            Spacer(GlanceModifier.height(10.dp))
            tasks.forEach { task ->
                TaskRow(task, onOpenApp)
            }
        }
        Spacer(GlanceModifier.height(10.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth().clickable(onOpenApp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Open Den",
                style = TextStyle(color = WidgetAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.weight(1f))
            Text(
                text = "›",
                style = TextStyle(color = WidgetAccent, fontSize = 16.sp),
            )
        }
    }
}

@Composable
private fun TaskRow(task: Task, onOpenApp: () -> Unit) {
    val dueLabel = task.dueAt?.let { Dates.humanDay(it, hasTime = false) } ?: ""
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.clickable(
                actionRunCallback<ToggleTaskAction>(
                    actionParametersOf(TasksWidget.TaskIdKey to task.id),
                )
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (task.completed) "◉" else "○",
                style = TextStyle(
                    color = if (task.completed) WidgetDone else WidgetAccent,
                    fontSize = 16.sp,
                ),
            )
            Spacer(GlanceModifier.width(8.dp))
        }
        Column(modifier = GlanceModifier.weight(1f).clickable(onOpenApp)) {
            Text(
                text = task.title,
                maxLines = 1,
                style = TextStyle(
                    color = WidgetFg,
                    fontSize = 13.sp,
                    decoration = if (task.completed) TextDecoration.LineThrough else null,
                ),
            )
            if (dueLabel.isNotEmpty()) {
                Text(
                    text = dueLabel,
                    maxLines = 1,
                    style = TextStyle(color = WidgetMuted, fontSize = 11.sp),
                )
            }
        }
    }
}

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val container = AppGraph.container ?: return
        val taskId = parameters[TasksWidget.TaskIdKey] ?: return
        val task = container.taskRepo.getTask(taskId) ?: return
        if (!task.completed) {
            container.taskRepo.toggleCompletion(task, rating = null, reflection = null)
        } else {
            container.taskRepo.uncomplete(task)
        }
        TasksWidget().updateAll(context)
    }
}