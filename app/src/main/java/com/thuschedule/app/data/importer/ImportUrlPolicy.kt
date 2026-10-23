package com.thuschedule.app.data.importer

import java.net.URI

/**
 * Accept a user-selected HTTPS entry without rewriting its path, query or fragment.
 *
 * 例外：清华本科教务系统（zhjw.cic.tsinghua.edu.cn）仅提供 HTTP 明文入口，
 * 对该域名放行 http 并配合 network_security_config 允许其明文流量；
 * 其余地址一律要求 HTTPS。
 */
object ImportUrlPolicy {
    const val DEFAULT_URL = "https://info.tsinghua.edu.cn"

    /** 允许 HTTP 明文访问的域名白名单（小写）。 */
    val CLEARTEXT_HOSTS = setOf("zhjw.cic.tsinghua.edu.cn")

    fun normalize(input: String): String {
        val value = input.trim()
        require(value.isNotEmpty()) { "请输入教务系统或学校 WebVPN 的网址" }
        require(value.length <= 8192 && value.none { it.isWhitespace() || it.isISOControl() || it == '\\' }) { "网址格式不正确，请检查空格和特殊字符" }
        val url = when {
            value.startsWith("//") -> "https:$value"
            Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(value) -> value
            Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(value) &&
                !Regex("^[^/?#:]+:[0-9]+(?:[/?#].*)?$").matches(value) -> value
            else -> "https://$value"
        }
        val parsed = try { URI(url) } catch (_: Exception) {
            throw IllegalArgumentException("网址格式不正确，请检查后重试")
        }
        require(!parsed.host.isNullOrBlank() && parsed.rawUserInfo == null &&
            (parsed.port == -1 || parsed.port in 1..65535)) { "请输入有效的网站地址，网址中不能包含账号密码" }
        val scheme = parsed.scheme.lowercase()
        val host = parsed.host.lowercase()
        val secure = scheme == "https" || (scheme == "http" && host in CLEARTEXT_HOSTS)
        require(secure) { "请输入 HTTPS 网址（https://），不支持此链接类型" }
        return scheme + url.substring(url.indexOf(':'))
    }
}
