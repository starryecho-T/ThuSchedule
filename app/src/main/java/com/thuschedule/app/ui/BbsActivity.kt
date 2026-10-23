package com.thuschedule.app.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.thuschedule.app.R

/**
 * BBS 论坛页面：在内置 WebView 中打开 https://www.free-bbs.cn/（电子系学习发展共同体）。
 *
 * 适配要点：启用 JS 与 DOM 存储；论坛链接常用 target="_blank" 新窗口打开，
 * 与导入页一致通过 onCreateWindow 接管到当前 WebView；非 http(s) 链接
 * （如 mailto:）交给系统处理；支持返回键逐级后退与系统浏览器打开。
 */
class BbsActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var progress: ProgressBar
    private var newWindowWebView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bbs)
        toolbar = findViewById(R.id.bbs_toolbar)
        progress = findViewById(R.id.bbs_progress)
        webView = findViewById(R.id.bbs_webview)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bbs_root)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        val light = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK != android.content.res.Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.bbs_reload -> { webView.reload(); true }
                R.id.bbs_browser -> { openInBrowser(); true }
                else -> false
            }
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(true)
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                progress.progress = newProgress
            }

            override fun onCreateWindow(
                view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?,
            ): Boolean {
                if (view == null || resultMsg == null) return false
                newWindowWebView?.destroy()
                val transient = WebView(view.context)
                newWindowWebView = transient
                transient.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                        dispatchUrl(request.url)
                        v.post { v.destroy() }
                        return true
                    }
                }
                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = transient
                resultMsg.sendToTarget()
                return true
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (request == null) return false
                if (request.url.scheme.equals("http", true) || request.url.scheme.equals("https", true)) return false
                dispatchUrl(request.url)
                return true
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    Toast.makeText(this@BbsActivity, R.string.bbs_page_error, Toast.LENGTH_LONG).show()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) webView.goBack() else finish()
        }
        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(getString(R.string.bbs_url))
        }
    }

    /** http(s) 在当前 WebView 打开；其它 scheme 交给系统（不可用则提示）。 */
    private fun dispatchUrl(url: Uri) {
        val scheme = url.scheme?.lowercase()
        if (scheme == "http" || scheme == "https") {
            webView.loadUrl(url.toString())
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.bbs_link_unsupported, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openInBrowser() {
        val url = webView.url ?: getString(R.string.bbs_url)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.bbs_link_unsupported, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        newWindowWebView?.destroy()
        newWindowWebView = null
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
