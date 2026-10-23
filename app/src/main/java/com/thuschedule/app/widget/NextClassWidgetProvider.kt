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
import com.thuschedule.app.data.db.AppDatabase
import com.thuschedule.app.model.Course
import com.thuschedule.app.ui.ThemePreferences
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * 「下节课」小组件（2x1）：今天下一节未开始的课，含时间与教室。
 * 刷新时机与今日课程小组件一致（数据变化 / 跨天 / 半点重算）。
 */
class NextClassWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repo = CourseRepository(context, AppDatabase.getDatabase(context).courseDao())
        widgetScope.launch {
            val courses = repo.observeAllOnce()
            appWidgetIds.forEach { id -> appWidgetManager.updateAppWidget(id, buildViews(context, courses)) }
        }
    }

    companion object {
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, error -> Log.e("NextClassWidget", "Widget refresh failed", error) })

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NextClassWidgetProvider::class.java))
            if (ids.isEmpty()) return
            NextClassWidgetProvider().onUpdate(context, manager, ids)
        }

        fun buildViews(context: Context, courses: List<Course>, date: LocalDate = LocalDate.now(), now: LocalTime = LocalTime.now()): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_next)
            applyTheme(context, views)
            val dayNames = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
            views.setTextViewText(R.id.widget_next_header,
                context.getString(R.string.widget_next_title) + " · " + dayNames[date.dayOfWeek.value])
            val entries = TodayPlan.entriesFor(courses, date)
            val next = TodayPlan.nextEntry(entries, now)
            when {
                entries.isEmpty() -> {
                    views.setTextViewText(R.id.widget_next_body, "今天没有课程")
                    views.setTextViewText(R.id.widget_next_sub, "好好休息 🎉")
                }
                next == null -> {
                    views.setTextViewText(R.id.widget_next_body, "今天的课已结束")
                    views.setTextViewText(R.id.widget_next_sub, "共 ${entries.size} 节")
                }
                else -> {
                    views.setTextViewText(R.id.widget_next_body, next.course.name)
                    val room = next.course.room.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                    views.setTextViewText(R.id.widget_next_sub,
                        "${next.startTime}–${next.endTime}$room")
                }
            }
            return views
        }

        private fun applyTheme(context: Context, views: RemoteViews) {
            val themed = ThemePreferences.widgetContext(context)
            val dark = themed.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            views.setInt(R.id.widget_next_root, "setBackgroundResource", if (dark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light)
            views.setTextColor(R.id.widget_next_header, themed.getColor(R.color.widget_header))
            views.setTextColor(R.id.widget_next_body, themed.getColor(R.color.widget_text))
            views.setTextColor(R.id.widget_next_sub, themed.getColor(R.color.widget_text))
        }
    }
}
