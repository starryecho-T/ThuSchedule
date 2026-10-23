package com.thuschedule.app.data.importer

import com.thuschedule.app.data.ThuTimeTable
import com.thuschedule.app.model.Course
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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

    /** 作息表的节次时间预解析；接口时间格式可能是 8:00 / 08:00 / 08:00:00。 */
    private val periodStarts = ThuTimeTable.PERIODS.map { it.index to parseTime(it.start) }
    private val periodEnds = ThuTimeTable.PERIODS.map { it.index to parseTime(it.end) }

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
            val date = parseDate(text("nq")) ?: run {
                reject("日期「${text("nq")}」无法识别"); return@forEachIndexed
            }
            val week = Math.floorDiv(ChronoUnit.DAYS.between(termStart, date), 7L).toInt() + 1
            if (week !in 1..totalWeeks) {
                reject("日期 $date 不在本学期（第1-${totalWeeks}周）内"); return@forEachIndexed
            }
            val startTime = parseTime(text("kssj")) ?: run {
                reject("上课时间「${text("kssj")}」无法识别"); return@forEachIndexed
            }
            val endTime = parseTime(text("jssj")) ?: run {
                reject("下课时间「${text("jssj")}」无法识别"); return@forEachIndexed
            }
            val start = periodStarts.firstOrNull { it.second == startTime }?.first
            val end = periodEnds.firstOrNull { it.second == endTime }?.first
            if (start == null || end == null || start > end) {
                reject("上课时间 ${text("kssj")}-${text("jssj")} 与作息表不匹配")
                return@forEachIndexed
            }
            val day = date.dayOfWeek.value
            val room = text("dd")
            val key = listOf(name, room, day, start, end).joinToString("\u0000")
            val slot = slots.getOrPut(key) {
                Slot(name, room, day, start, end, slots.size, mutableSetOf())
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

    /** 兼容 2026-09-14、2026/9/14、2026.9.14、20260914 等写法（thu-info 用 dayjs 宽容解析）。 */
    internal fun parseDate(raw: String): LocalDate? {
        val parts = raw.trim().replace('.', '-').replace('/', '-').split('-')
        if (parts.size == 3) {
            val numbers = parts.map { it.toIntOrNull() ?: return null }
            val (year, month, day) = numbers
            if (year !in 2000..2100) return null
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }
        val compact = parts.singleOrNull() ?: return null
        if (compact.length != 8 || compact.any { !it.isDigit() }) return null
        return runCatching {
            LocalDate.of(compact.substring(0, 4).toInt(), compact.substring(4, 6).toInt(), compact.substring(6, 8).toInt())
        }.getOrNull()
    }

    /** 兼容全角冒号与可省略的前导零（8:00 与 08:00 等价）。 */
    internal fun parseTime(raw: String): LocalTime? {
        val value = raw.replace("：", ":").trim()
        return runCatching { LocalTime.parse(value) }.getOrElse {
            runCatching { LocalTime.parse(value, DateTimeFormatter.ofPattern("H:mm")) }.getOrNull()
        }
    }
}
