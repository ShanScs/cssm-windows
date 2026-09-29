package com.cssm.desktop.data

/** 服务器地区选项：名称 + 国旗 emoji */
data class Region(val name: String, val flag: String) {
    val label: String get() = "$flag $name"
}

/** 添加/编辑服务器时的常用国家/地区列表 */
val REGIONS = listOf(
    Region("美国", "🇺🇸"),
    Region("英国", "🇬🇧"),
    Region("德国", "🇩🇪"),
    Region("法国", "🇫🇷"),
    Region("荷兰", "🇳🇱"),
    Region("新加坡", "🇸🇬"),
    Region("日本", "🇯🇵"),
    Region("韩国", "🇰🇷"),
    Region("澳洲", "🇦🇺"),
    Region("加拿大", "🇨🇦"),
    Region("波兰", "🇵🇱"),
    Region("香港", "🇭🇰"),
    Region("台湾", "🇹🇼"),
    Region("其他", "🌐"),
)

fun findRegion(name: String): Region? = REGIONS.firstOrNull { it.name == name }

/** 常用国家 ISO 代码 -> Region（自动检测时优先用中文名） */
private val CODE_TO_REGION = mapOf(
    "US" to Region("美国", "🇺🇸"),
    "GB" to Region("英国", "🇬🇧"),
    "DE" to Region("德国", "🇩🇪"),
    "FR" to Region("法国", "🇫🇷"),
    "NL" to Region("荷兰", "🇳🇱"),
    "SG" to Region("新加坡", "🇸🇬"),
    "JP" to Region("日本", "🇯🇵"),
    "KR" to Region("韩国", "🇰🇷"),
    "AU" to Region("澳洲", "🇦🇺"),
    "CA" to Region("加拿大", "🇨🇦"),
    "PL" to Region("波兰", "🇵🇱"),
    "HK" to Region("香港", "🇭🇰"),
    "TW" to Region("台湾", "🇹🇼"),
)

/**
 * 由 ISO 国家代码生成 Region：常用国家用中文名，其余国家用接口返回的
 * 英文国名 + 按代码生成的旗帜 emoji。
 */
fun regionForCountryCode(code: String, country: String): Region {
    CODE_TO_REGION[code.uppercase()]?.let { return it }
    return Region(country.ifBlank { "其他" }, flagEmoji(code))
}

/** 由 ISO 国家代码生成旗帜 emoji（Regional Indicator Symbols），非法代码返回 🌐 */
fun flagEmoji(code: String): String {
    val c = code.uppercase()
    if (c.length != 2 || !c.all { it in 'A'..'Z' }) return "🌐"
    val sb = StringBuilder()
    for (ch in c) sb.appendCodePoint(0x1F1E6 + (ch - 'A'))
    return sb.toString()
}
