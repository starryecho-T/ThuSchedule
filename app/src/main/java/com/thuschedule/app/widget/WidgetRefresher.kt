package com.thuschedule.app.widget

import android.content.Context

/** 小组件矩阵的统一刷新入口：课程变化、跨天、主题切换时全部重绘。 */
object WidgetRefresher {
    fun refreshAll(context: Context) {
        val app = context.applicationContext
        TodayCourseWidgetProvider.refreshAll(app)
        NextClassWidgetProvider.refreshAll(app)
        WeekListWidgetProvider.refreshAll(app)
    }
}
