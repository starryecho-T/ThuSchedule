package com.thuschedule.app.data

import com.thuschedule.app.model.Course

/**
 * 示例课表数据（2026-2027 学年秋季学期 · 虚构的 2026 级工科新生课表）。
 *
 * 课程、教师均为虚构，仅用于首次安装演示；主动删空后不会重新填入。
 * 教学楼名称（六教、三教、四教、理科楼等）为清华常见上课地点。
 *
 * 同一门课在不同天/节次的记录使用同一颜色索引，便于在周视图中识别。
 * 周次由 [TermCalendar] 按日期计算；节次对应 [ThuTimeTable] 的 14 小节。
 */
object SampleData {

    val COURSES: List<Course> = listOf(
        // ---------- 周一 ----------
        Course("微积分A(1)", "陈志远", "六教-200", 1, 1, 2, Course.allWeeks(1..16), 0),
        Course("线性代数(1)", "林慧敏", "理科楼-106", 1, 3, 4, Course.allWeeks(1..16), 3),
        Course("思想道德与法治", "王建国", "六教-300", 1, 6, 7, Course.allWeeks(1..13), 5),
        Course("程序设计基础实验", "周子昂", "四教-4102", 1, 8, 9, Course.evenWeeks(2..16), 2),
        // ---------- 周二 ----------
        Course("大学英语(1)", "顾雅萍", "六教-405", 2, 1, 2, Course.allWeeks(1..16), 6),
        Course("大学物理B(1)", "孙立新", "三教-1200", 2, 3, 5, Course.allWeeks(1..16), 1),
        Course("体育(1)", "刘畅", "综合体育馆", 2, 6, 7, Course.allWeeks(1..16), 9),
        // ---------- 周三 ----------
        Course("微积分A(1)", "陈志远", "六教-200", 3, 1, 2, Course.allWeeks(1..16), 0),
        Course("程序设计基础", "赵天宇", "三教-1103", 3, 3, 4, Course.allWeeks(1..16), 2),
        Course("军事理论", "李国强", "六教-100", 3, 8, 9, Course.allWeeks(1..8), 8),
        // ---------- 周四 ----------
        Course("线性代数(1)", "林慧敏", "理科楼-106", 4, 1, 2, Course.allWeeks(1..16), 3),
        Course("大学物理B(1)", "孙立新", "三教-1200", 4, 3, 4, Course.allWeeks(1..16), 1),
        Course("大学英语(1)", "顾雅萍", "六教-405", 4, 6, 7, Course.allWeeks(1..16), 6),
        Course("新生研讨课", "郑晓彤", "六教-210", 4, 12, 13, Course.allWeeks(1..8), 4),
        // ---------- 周五 ----------
        Course("微积分A(1)习题课", "助教", "六教-208", 5, 3, 4, Course.oddWeeks(3..15), 0),
        Course("科技史", "何雨桐", "六教-500", 5, 6, 7, Course.allWeeks(1..16), 7),
        // 周六、周日全天无课
    )
}
