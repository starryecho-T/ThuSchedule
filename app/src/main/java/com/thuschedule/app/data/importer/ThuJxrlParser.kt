package com.thuschedule.app.data.importer

import com.thuschedule.app.data.ThuTimeTable
import com.thuschedule.app.model.Course
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 清华「个人教学日历」接口（info.tsinghua.edu.cn/jxmh_out.do?m=bks_jxrl_all）的
 * 记录解析：接口对每节课返回一条 JSON 记录。
 *
 * 字段规格（与社区开源实现一致）：nq=日期、nr=课程名、dd=地点、
 * fl=分类、kssj/jssj=起止时间（可能含全角冒号）。接口不含教师字段，
 * 教师留空。日期用于反推周次与星期，时间反查作息表节次；
 * 同名同地同时段的跨周记录合并为一条课程。
 *
 * 「个人日历」分类的条目不是课程安排，跳过；其余任一记录非法即整批拒绝。
 */
object ThuJxrlParser {

    data class Result(val courses: List<Course>, val issues: List<String>)

    fun parse(records: List<*>, termStart: LocalDate, totalWeeks: Int): Result {
        val issues = mutableListOf<String>()

        data class Slot(
            val name: String,
            val room: String,
            val day: Int,
            val startPeriod: Int,
            val endPeriod: Int,
            val order: Int,
            val weeks: MutableSet<Int>,
        )

        val slots = linkedMapOf<String, Slot>()
        records.forEachIndexed { index, raw ->
            fun reject(reason: String) { issues.add("第${index + 1}条记录：$reason") }
            val row = raw as? Map<*, *> ?: run { reject("不是对象"); return@forEachIndexed }
            fun text(key: String): String = (row[key] as? String)?.trim().orEmpty()
            if (text("fl").contains("个人")) return@forEachIndexed
            val name = text("nr")
            if (name.isEmpty()) { reject("缺少课程名"); return@forEachIndexed }
            val date = runCatching { LocalDate.parse(text("nq")) }.getOrElse {
                reject("日期「${text("nq")}」无法识别"); return@forEachIndexed
            }
            val week = Math.floorDiv(ChronoUnit.DAYS.between(termStart, date), 7L).toInt() + 1
            if (week !in 1..totalWeeks) {
                reject("日期 $date 不在本学期（第1-${totalWeeks}周）内"); return@forEachIndexed
            }
            val start = ThuTimeTable.PERIODS.firstOrNull { it.start == normalizeTime(text("kssj")) }
            val end = ThuTimeTable.PERIODS.firstOrNull { it.end == normalizeTime(text("jssj")) }
            if (start == null || end == null || start.index > end.index) {
                reject("上课时间 ${text("kssj")}-${text("jssj")} 与作息表不匹配")
                return@forEachIndexed
            }
            val day = date.dayOfWeek.value
            val room = text("dd")
            val key = listOf(name, room, day, start.index, end.index).joinToString("\u0000")
            val slot = slots.getOrPut(key) {
                Slot(name, room, day, start.index, end.index, slots.size, mutableSetOf())
            }
            slot.weeks.add(week)
        }
        if (issues.isNotEmpty()) return Result(emptyList(), issues)
        if (slots.isEmpty()) {
            issues.add("教学日历没有返回课程安排，请确认已登录 INFO 门户且学期正确")
            return Result(emptyList(), issues)
        }
        val courses = slots.values.sortedBy { it.order }.map { slot ->
            Course(
                name = slot.name, teacher = "", room = slot.room,
                dayOfWeek = slot.day, startPeriod = slot.startPeriod, endPeriod = slot.endPeriod,
                weeks = slot.weeks.toSortedSet(), colorIndex = slot.order,
            )
        }
        return Result(courses, emptyList())
    }

    private fun normalizeTime(value: String): String = value.replace("：", ":").trim()
}
