package com.thuschedule.app.data.importer

import com.thuschedule.app.model.Course
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 教学日历记录解析测试。学期起点 2026-09-14（第 1 教学周周一）、共 18 周。
 * 字段规格与 INFO jxmh_out.do?m=bks_jxrl_all 一致：nq/nr/dd/fl/kssj/jssj。
 */
class ThuJxrlParserTest {

    private val termStart = LocalDate.of(2026, 9, 14)
    private val totalWeeks = 18

    private fun record(
        nq: String, nr: String, dd: String = "六教-200",
        kssj: String = "08:00", jssj: String = "09:35", fl: String = "课程",
    ): Map<String, String> = mapOf("nq" to nq, "nr" to nr, "dd" to dd, "kssj" to kssj, "jssj" to jssj, "fl" to fl)

    @Test fun mergesSameSlotAcrossWeeksIntoOneCourse() {
        val result = ThuJxrlParser.parse(listOf(
            record("2026-09-14", "微积分A(1)"),
            record("2026-09-21", "微积分A(1)"),
            record("2026-10-19", "微积分A(1)"),
        ), termStart, totalWeeks)
        val course = result.courses.single()
        assertEquals("微积分A(1)", course.name)
        assertEquals(1, course.dayOfWeek)
        assertEquals(1, course.startPeriod)
        assertEquals(2, course.endPeriod)
        assertEquals(setOf(1, 2, 6), course.weeks)
        assertEquals("六教-200", course.room)
    }

    @Test fun mapsTimeSpanToPeriodRangeAndDerivesDay() {
        // 2026-09-15 是周二；09:50-12:15 覆盖第 3-5 小节
        val result = ThuJxrlParser.parse(listOf(
            record("2026-09-15", "大学物理B(1)", dd = "三教-1200", kssj = "09:50", jssj = "12:15"),
        ), termStart, totalWeeks)
        val course = result.courses.single()
        assertEquals(2, course.dayOfWeek)
        assertEquals(3, course.startPeriod)
        assertEquals(5, course.endPeriod)
        assertEquals(setOf(1), course.weeks)
    }

    @Test fun normalizesFullWidthColonAndAcceptsEmptyRoom() {
        val result = ThuJxrlParser.parse(listOf(
            record("2026-09-14", "体育(1)", dd = "", kssj = "13：30", jssj = "15：05"),
        ), termStart, totalWeeks)
        val course = result.courses.single()
        assertEquals(6, course.startPeriod)
        assertEquals(7, course.endPeriod)
        assertEquals("", course.room)
    }

    @Test fun skipsPersonalCalendarEntries() {
        val result = ThuJxrlParser.parse(listOf(
            record("2026-09-14", "微积分A(1)"),
            record("2026-09-14", "体检", dd = "校医院", kssj = "08:00", jssj = "09:35", fl = "个人日历"),
        ), termStart, totalWeeks)
        assertEquals(listOf("微积分A(1)"), result.courses.map { it.name })
    }

    @Test fun assignsStableColorIndexesInFirstSeenOrder() {
        val result = ThuJxrlParser.parse(listOf(
            record("2026-09-14", "微积分A(1)"),
            record("2026-09-15", "大学物理B(1)", kssj = "09:50", jssj = "10:35"),
            record("2026-09-16", "微积分A(1)"),
        ), termStart, totalWeeks)
        assertEquals(0, result.courses.first { it.name == "微积分A(1)" }.colorIndex)
        assertEquals(1, result.courses.first { it.name == "大学物理B(1)" }.colorIndex)
    }

    @Test fun rejectsWholeBatchOnAnyInvalidRecord() {
        for (bad in listOf(
            record("2026-09-14", "  "),                                  // 空课程名
            record("2026/09/14", "微积分A(1)"),                           // 日期格式
            record("2027-02-01", "微积分A(1)"),                           // 学期外
            record("2026-09-14", "微积分A(1)", kssj = "08:10", jssj = "09:35"), // 时间不在作息表
            record("2026-09-14", "微积分A(1)", kssj = "09:35", jssj = "08:00"), // 起止倒置
            mapOf("nr" to "微积分A(1)"),                                  // 缺字段
            listOf("不是对象"),                                           // 非对象
        )) {
            val result = ThuJxrlParser.parse(listOf(record("2026-09-14", "微积分A(1)"), bad),
                termStart, totalWeeks)
            assertTrue("应整批拒绝：$bad", result.courses.isEmpty() && result.issues.isNotEmpty())
        }
    }

    @Test fun reportsIssueWithRecordIndex() {
        val result = ThuJxrlParser.parse(listOf(
            record("2026-09-14", "微积分A(1)"),
            record("2026-09-14", "体育(1)", kssj = "99:99", jssj = "10:35"),
        ), termStart, totalWeeks)
        assertTrue(result.issues.single().contains("第2条记录"))
    }

    @Test fun emptyRecordsReportLoginHint() {
        val result = ThuJxrlParser.parse(emptyList<Any>(), termStart, totalWeeks)
        assertTrue(result.courses.isEmpty())
        assertTrue(result.issues.single().contains("登录"))
    }

    @Test fun eveningCoursesMapToLatePeriods() {
        val result = ThuJxrlParser.parse(listOf(
            record("2026-09-16", "新生研讨课", kssj = "19:20", jssj = "20:55"),
        ), termStart, totalWeeks)
        val course = result.courses.single()
        assertEquals(12, course.startPeriod)
        assertEquals(13, course.endPeriod)
    }

    @Suppress("unused")
    private fun unusedCourse() = Course(name = "", teacher = "", room = "", dayOfWeek = 1,
        startPeriod = 1, endPeriod = 1, weeks = setOf(1))
}
