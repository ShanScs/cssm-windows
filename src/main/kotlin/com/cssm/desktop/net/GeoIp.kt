package com.cssm.desktop.net

import com.cssm.desktop.data.Region
import com.cssm.desktop.data.regionForCountryCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.InetAddress
import java.net.URL

/**
 * 通过 IP 归属地自动识别服务器地区（ip-api.com 免费接口，无需 key）。
 * host 可为 IP 或域名；内网/回环 IP、解析失败、查询失败时返回 null，
 * 调用方应静默忽略（不打断主流程）。
 */
object GeoIp {

    @Serializable
    private data class IpApiResp(
        val status: String = "",
        val country: String = "",
        val countryCode: String = ""
    )

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun detectRegion(host: String): Region? = withContext(Dispatchers.IO) {
        try {
            val ip = resolveIp(host.trim()) ?: return@withContext null
            if (isPrivateOrLoopback(ip)) return@withContext null
            val url = URL("http://ip-api.com/json/$ip?fields=status,country,countryCode")
            val conn = url.openConnection().apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Cssm/1.0")
            }
            val body = conn.getInputStream().bufferedReader().use { it.readText() }
            val resp = json.decodeFromString(IpApiResp.serializer(), body)
            if (resp.status != "success") return@withContext null
            val code = resp.countryCode.ifBlank { return@withContext null }
            regionForCountryCode(code, resp.country)
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveIp(host: String): String? {
        return try {
            if (host.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) || host.contains(":")) {
                host // 纯 IP（v4 / v6）直接用
            } else {
                InetAddress.getByName(host)?.hostAddress
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isPrivateOrLoopback(ip: String): Boolean {
        return try {
            val addr = InetAddress.getByName(ip)
            addr.isLoopbackAddress || addr.isSiteLocalAddress || addr.isLinkLocalAddress
        } catch (_: Exception) {
            true
        }
    }
}
