package com.thuschedule.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.widget.RemoteViews
import com.thuschedule.app.R
import com.thuschedule.app.data.CourseRepository
import com.thuschedule.app.data.TodayPlan
import com.thuschedule.app.data.TermCalendar
import com.thuschedule.app.data.db.AppDatabase
import com.thuschedule.app.model.Course
import com.thuschedule.app.ui.ThemePreferences
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 周列表小组件：按天列出本周课程（只显示有课的日子，超出可视行数截断）。
 * 刷新时机与今日课程小组件一致。
 */
class WeekListWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repo = CourseRepository(context, AppDatabase.getDatabase(context).courseDao())
        widgetScope.launch {
            val courses = repo.observeAllOnce()
            appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, buildViews(context, courses)) }
        }
    }

    companion object {
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, error -> Log.e("WeekListWidget", "Widget refresh failed", error) })

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeekListWidgetProvider::class.java))
            if (ids.isEmpty()) return
            WeekListWidgetProvider().onUpdate(context, manager, ids)
        }

        fun buildViews(context: Context, courses: List<Course>, date: LocalDate = LocalDate.now()): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_week)
            applyTheme(context, views)
            val week = TermCalendar.currentWeek(date)
            if (week !in 1..TermCalendar.TOTAL_WEEKS) {
                views.setTextViewText(R.id.widget_week_header, "假期中")
                views.setTextViewText(R.id.widget_week_body, "当前不在教学周内")
                return views
            }
            val dates = TermCalendar.weekDates(week)
            val dayNames = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
            val maxLines = 9
            val lines = dates.mapIndexedNotNull { index, day ->
                val entries = TodayPlan.entriesFor(courses, day)
                if (entries.isEmpty()) return@mapIndexedNotNull null
                "%s %02d/%02d  %s".format(dayNames[index + 1], day.monthValue, day.dayOfMonth,
                    entries.joinToString("、") { it.course.name })
            }
            val rendered = when {
                lines.isEmpty() -> "本周没有课程"
                lines.size <= maxLines -> lines.joinToString("\n")
                else -> lines.take(maxLines).joinToString("\n") + "\n…还有 ${lines.size - maxLines} 天有课"
            }
            views.setTextViewText(R.id.widget_week_header, TodayPlan.header(date))
            views.setTextViewText(R.id.widget_week_body, rendered)
            return views
        }

        private fun applyTheme(context: Context, views: RemoteViews) {
            val themed = ThemePreferences.widgetContext(context)
            val dark = themed.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            views.setInt(R.id.widget_week_root, "setBackgroundResource", if (dark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light)
            views.setTextColor(R.id.widget_week_header, themed.getColor(R.color.widget_header))
            views.setTextColor(R.id.widget_week_body, themed.getColor(R.color.widget_text))
        }
    }
}
