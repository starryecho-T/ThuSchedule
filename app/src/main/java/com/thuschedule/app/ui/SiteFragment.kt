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
 * 「主页」Tab：加载打包在 assets/site/ 里的个人网站（离线可用）。
 * 文件是自包含单页；想发布到公网时把它上传到 GitHub Pages 即可。
 * 外链（GitHub 等）交给系统浏览器打开。
 */
class SiteFragment : Fragment(R.layout.fragment_site), WebBackSupport {

    private var webView: WebView? = null
    private var progress: ProgressBar? = null
    private var newWindowWebView: WebView? = null

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
                if (request.url.scheme.equals("file", true)) return false
                dispatchUrl(request.url)
                return true
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true && isAdded) {
                    Toast.makeText(requireContext(), R.string.site_load_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
        web.loadUrl("file:///android_asset/site/index.html")
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
