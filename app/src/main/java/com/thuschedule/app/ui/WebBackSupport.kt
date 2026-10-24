package com.thuschedule.app.ui

/** 返回键优先交给 Tab 内的 WebView 后退（BBS、主页共用）。 */
interface WebBackSupport {
    fun handleBack(): Boolean
}
