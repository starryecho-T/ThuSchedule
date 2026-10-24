// 个人网站内容数据 —— 编辑这个文件即可更新整站内容（index.html 会自动渲染）。
// 三种修改方式（任选其一，改完保存/提交后约 1 分钟生效）：
//   1. 打开本站的 editor.html（网页表单编辑，推荐）
//   2. 在 GitHub 网页上直接编辑本文件（仓库页面点铅笔，或按 . 打开网页版 VS Code）
//   3. GitHub 手机 App 里编辑文件
window.SITE_CONTENT = {
  avatar: "",
  name: "starryecho",
  tagline: "清华大学 · 工科新生 · Android 开发爱好者",
  about: [
    "你好，我是 starryecho。喜欢把课程生活里的小麻烦做成小工具：现在是清华的一名工科新生，业余时间写 Android 应用、折腾硬件和网页。",
    "相信「工具应当简单、可靠、尊重用户」——所以写的应用都尽量本地运行、无广告、不催你登录。",
  ],
  projects: [
    {
      name: "ThuSchedule · 清华课表",
      description: "面向清华的 Android 课表应用：周视图、日程概览、桌面小组件矩阵、上课提醒，支持从 INFO 门户 / 教务系统一键导入课表。本地运行，无广告。",
      tags: ["Kotlin", "Room", "WebView"],
      link: "https://github.com/starryecho-T/ThuSchedule",
    },
    {
      name: "ClassSchedule · 课表",
      description: "ThuSchedule 的前身：为吉大教务适配的课表应用，完整测试、备份恢复、深色模式一应俱全。",
      tags: ["Kotlin", "Material 3"],
      link: "https://github.com/starryecho-T/ClassSchedule",
    },
    {
      name: "本网站",
      description: "单文件手写页面：无框架、无构建、离线可用，也内置在 ThuSchedule 的「主页」Tab 里。",
      tags: ["HTML", "CSS", "零依赖"],
      link: "",
    },
  ],
  skills: ["Kotlin", "Android", "Python", "C / C++", "Git", "LaTeX"],
  links: [
    { label: "GitHub", url: "https://github.com/starryecho-T" },
  ],
  footer: "© 2026 starryecho · 手写 HTML，无框架无构建",
};
