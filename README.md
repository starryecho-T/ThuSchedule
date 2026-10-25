# ThuSchedule 清华课表 📅

我的**个人课表工作站**：为我在清华的日常定制的 Android 课表应用——周视图、日程概览、
小组件矩阵、上课提醒、教务一键导入；本地运行、无广告、无账号。
由我此前的项目 [ClassSchedule](https://github.com/starryecho-T/ClassSchedule)（吉大版课表，**同为本人开发**）演化而来。

> **定位说明：这是私人项目，不提供成品安装包（Release）。**
> 它按我的学校（清华）、作息与使用习惯定制。如果你想用同类应用，请自行构建并改成你的校历作息；
> 如果你是开发者，欢迎**借用这套框架**——自绘周视图课表、严格校验的教务导入管线、
> 备份恢复、小组件矩阵、精确闹钟调度都是边界清晰的独立模块（见下方目录结构），MIT 许可随便拆。

> 当前版本：**0.1.0（1）**。

## ✨ 功能

自 ClassSchedule 演化并持续增强的能力：

- [x] 周视图课表、周次切换、日期与当前周显示（自绘 TimetableView）
- [x] 日程概览：按天查看课程安排
- [x] 课程添加 / 编辑 / 删除，支持每周、单周、双周与离散周次
- [x] 课程颜色标签、教室与教师信息；隐藏部分或全部周次并可恢复
- [x] Room 本地持久化，JSON 备份 / 恢复（预览、追加去重、事务回滚）
- [x] 桌面小组件矩阵（今日课程 / 下节课 / 周列表）
- [x] 上课前提醒通知（提前 20 分钟；未授权精确闹钟时自动降级）
- [x] 底部五 Tab 导航（课表 / 日程 / BBS / 主页 / 我的）、外观模式（跟随系统 / 浅色 / 深色）
- [x] 教务 WebView 登录、页面快照导出（对任意 HTTPS 站点可用）
- [x] 内置个人主页 Tab（GitHub Pages 站点离线副本 + 在线同步）

清华适配（0.1.0）：

- [x] 清华作息时间表：一天 6 大节 14 小节（第一小节 08:00，晚课 19:20 起，第五大节保留）
- [x] 2026-2027 学年秋季学期校历：第 1 教学周 2026-09-14（周一），共 18 教学周
- [x] 示例课表使用清华常见上课地点（六教、三教、理科楼等，课程为虚构演示数据）
- [x] 教务一键导入：INFO 门户教学日历接口（JSONP），整学期一次抓取、严格校验、逐周合并

## 📌 已知状态与限制

- **教务导入**：可在应用内登录 `info.tsinghua.edu.cn` 或学校 WebVPN 并导出页面快照；「一键识别」走 INFO 教学日历接口，实验/调课等未安排时间的课程需排课后重新识别追加。
- **更新检查**：指向本仓库 `starryecho-T/ThuSchedule` 的 GitHub Releases；未发布 Release 时提示已是最新版本。
- **备份格式**：`thuschedule-backup` version=1，与 ClassSchedule 的备份互不通用（学期配置不同，恢复会被拒绝，属预期行为）。
- 作息时间与校历集中在 `app/src/main/java/com/thuschedule/app/data/` 下的 `ThuTimeTable.kt` 与 `TermCalendar.kt`，与实际不符时直接修改即可，界面与周次自动重算。

## 🛠 技术栈

| 分类 | 技术 |
| --- | --- |
| 语言 | Kotlin |
| 最低支持 | Android 8.0（API 26） |
| 目标版本 | Android 15（API 35） |
| UI | View 体系 + Material Design 3 |
| 架构 | MVVM（ViewModel + Repository） |
| 数据库 | Room 2.6.1 |
| 构建 | Gradle（Kotlin DSL）+ AGP 8.x |

## 🚀 构建运行

- JDK 17（本机路径示例：`D:\dev-tools\jdk-17`，需设置 `JAVA_HOME`）
- Android SDK Platform 35

```bash
git clone https://github.com/starryecho-T/ThuSchedule.git
cd ThuSchedule
export JAVA_HOME=/path/to/jdk-17   # Windows Git Bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

APK 输出在 `app/build/outputs/apk/debug/app-debug.apk`；当前测试基线：**136 项 JVM 单元测试全部通过**（2026-10-23，JDK 17 + Gradle 8.10.2）。

## 🗺 路线图

- [ ] 应用内学期设置（开学日期 / 总周数可配置）
- [ ] 上课自动勿扰、节假日数据
- [ ] 多学期管理

## 📄 许可证

[MIT License](./LICENSE) © 2026 starryecho-T · ClassSchedule 与 ThuSchedule 均为本人开发，框架与代码欢迎借用（按 MIT 条款保留署名）。
