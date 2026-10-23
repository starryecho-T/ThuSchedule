package com.thuschedule.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 校历工具：由学期开学日（第 1 周的周一）推算周次与具体日期。
 *
 * 开学日期如与实际校历不符，修改 [TERM_START] 即可，
 * 周次编号与表头日期会全部自动重算。
 */
object TermCalendar {

    /** 2026-2027 学年秋季学期开学日（第 1 教学周周一，据清华大学官方校历）。 */
    val TERM_START: LocalDate = LocalDate.of(2026, 9, 14)

    /** 学期总周数，与作息表一致。 */
    const val TOTAL_WEEKS: Int = ThuTimeTable.TOTAL_WEEKS

    /** 今天是第几周（学期外可能返回 <=0 或 > 总周数，调用方自行 clamp）。 */
    fun currentWeek(today: LocalDate = LocalDate.now()): Int =
        Math.floorDiv(ChronoUnit.DAYS.between(TERM_START, today), 7L).toInt() + 1

    /** 第 [week] 周的 7 个日期（下标 0=周一 ... 6=周日）。 */
    fun weekDates(week: Int): List<LocalDate> {
        val monday = TERM_START.plusWeeks((week - 1).toLong())
        return (0L..6L).map(monday::plusDays)
    }

    /** [week] 是否为本周。 */
    fun isCurrentWeek(week: Int): Boolean = currentWeek() == week
}
