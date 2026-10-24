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
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.thuschedule.app.R

/**
 * 「主页」Tab：加载打包在 assets/site/ 里的个人网站（离线可用）。
 * 文件是自包含单页；想发布到公网时把它上传到 GitHub Pages 即可。
 * 外链（GitHub 等）交给系统浏览器打开。
 */
class SiteFragment : Fragment(R.layout.fragment_site), WebBackSupport {

    private var webView: WebView? = null
    private var progress: ProgressBar? = null
    private var newWindowWebView: WebView? = null
    private var usedFallback = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val web = view.findViewById<WebView>(R.id.site_webview)
        progress = view.findViewById(R.id.site_progress)
        webView = web
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(true)
            // 站点内容要跟随线上编辑器的更新；GitHub Pages 有 10 分钟缓存头，
            // 这里每次进入都取最新（页面极小，开销可忽略）。
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        CookieManager.getInstance().setAcceptCookie(true)
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
                val url = request.url.toString()
                if (request.url.scheme.equals("https", true) && request.url.host == LIVE_HOST) return false
                if (request.url.scheme.equals("file", true)) return false
                dispatchUrl(request.url)
                return true
            }

            // 优先加载公网站点（编辑后自动同步）；无网络/失败时回退到打包副本。
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) failToBundled()
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, response: WebResourceResponse?) {
                if (request?.isForMainFrame == true) failToBundled()
            }
        }
        web.loadUrl(LIVE_URL)
    }

    private fun failToBundled() {
        if (usedFallback) return
        usedFallback = true
        webView?.loadUrl("file:///android_asset/site/index.html")
    }

    override fun handleBack(): Boolean = webView?.canGoBack()?.also { webView?.goBack() } == true

    private fun dispatchUrl(url: Uri) {
        val context = context ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.bbs_link_unsupported, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() { super.onResume(); webView?.onResume() }
    override fun onPause() { webView?.onPause(); super.onPause() }

    companion object {
        /** 公网站点（GitHub Pages）：编辑器保存后约 1 分钟自动更新。 */
        const val LIVE_URL = "https://starryecho-t.github.io/"
        const val LIVE_HOST = "starryecho-t.github.io"
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
