package com.thuschedule.app.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Message
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
import androidx.fragment.app.Fragment
import com.thuschedule.app.R

/**
 * 底部导航「BBS」Tab：内置 WebView 打开 https://www.free-bbs.cn/
 * （电子系学习发展共同体）。
 *
 * 适配要点：启用 JS 与 DOM 存储；论坛链接常用 target="_blank" 新窗口
 * 打开，与导入页一致通过 onCreateWindow 接管到当前 WebView；
 * 非 http(s) 链接（如 mailto:）交给系统处理。Tab 采用 show/hide，
 * 视图在切换时存活，浏览状态自然保留。
 */
class BbsFragment : Fragment(R.layout.fragment_bbs), WebBackSupport {

    private var webView: WebView? = null
    private var progress: ProgressBar? = null
    private var newWindowWebView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val web = view.findViewById<WebView>(R.id.bbs_webview)
        progress = view.findViewById(R.id.bbs_progress)
        webView = web
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(true)
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(web, true)
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress?.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
                progress?.progress = newProgress
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
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (request == null) return false
                val scheme = request.url.scheme?.lowercase()
                if (scheme == "http" || scheme == "https") return false
                dispatchUrl(request.url)
                return true
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true && isAdded) {
                    Toast.makeText(requireContext(), R.string.bbs_page_error, Toast.LENGTH_LONG).show()
                }
            }
        }
        if (savedInstanceState == null || web.restoreState(savedInstanceState) == null) {
            web.loadUrl(getString(R.string.bbs_url))
        }
    }

    /** 返回键优先在网页历史内后退；无历史时返回 false 交给 Activity 切回课表。 */
    override fun handleBack(): Boolean = webView?.canGoBack()?.also { webView?.goBack() } == true

    private fun dispatchUrl(url: Uri) {
        val scheme = url.scheme?.lowercase()
        if (scheme == "http" || scheme == "https") {
            webView?.loadUrl(url.toString())
            return
        }
        val context = context ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.bbs_link_unsupported, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() { super.onResume(); webView?.onResume() }
    override fun onPause() { webView?.onPause(); super.onPause() }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView?.saveState(outState)
    }

    override fun onDestroyView() {
        (webView?.parent as? ViewGroup)?.removeView(webView)
        newWindowWebView?.destroy()
        newWindowWebView = null
        webView?.stopLoading()
        webView?.destroy()
        webView = null
        progress = null
        super.onDestroyView()
    }
}
