package com.cssm.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/**
 * 操作系统徽标：Debian 用官网官方旋涡 logo（无底直接摆）；
 * 其他系统用原创设计（品牌色圆形底 + 白色系统首字母），不使用官方 logo 图片资源。
 */
@Composable
fun OsBadge(
    osId: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val (bg, letter) = osStyle(osId)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (letter == "?") {
            // 未知系统：用服务器堆叠图标代替问号，看起来不像占位符
            Icon(
                imageVector = Icons.Filled.Dns,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.52f)
            )
        } else {
            Text(
                letter,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.45f).sp
            )
        }
    }
}

/** 返回该系统的 (品牌色, 首字母)；未知系统显示灰底 "?" */
fun osStyle(osId: String): Pair<Color, String> {    return when (osId.lowercase()) {
        "ubuntu" -> Color(0xFFE95420) to "U"
        "debian" -> Color(0xFFD70A53) to "D"
        "centos", "centos-stream" -> Color(0xFF932279) to "C"
        "rhel", "redhat" -> Color(0xFFEE0000) to "R"
        "fedora" -> Color(0xFF51A2DA) to "F"
        "alpine" -> Color(0xFF0D597F) to "A"
        "arch", "archarm" -> Color(0xFF1793D1) to "A"
        "opensuse-leap", "opensuse-tumbleweed", "opensuse" -> Color(0xFF73BA25) to "S"
        "rocky" -> Color(0xFF10B981) to "R"
        "almalinux" -> Color(0xFFD84315) to "A"
        "oracle" -> Color(0xFFC74634) to "O"
        "amzn", "amazon" -> Color(0xFFFF9900) to "A"
        "gentoo" -> Color(0xFF54487A) to "G"
        "void" -> Color(0xFF478061) to "V"
        "nixos" -> Color(0xFF7EBAE4) to "N"
        "freebsd" -> Color(0xFFAB2B28) to "B"
        "openbsd" -> Color(0xFFF2CA30) to "O"
        else -> Color(0xFF5B6472) to "?"
    }
}

/**
 * 由 os-release 信息生成简短系统展示名，如 "Debian 12"、"Ubuntu 22.04"。
 * 未识别时返回 ""，调用方可选择不显示。
 */
fun osDisplayName(osId: String, osName: String): String {
    val pretty = osName.substringBefore(" (").trim()
    if (pretty.isNotBlank()) {
        return pretty.replace(" GNU/Linux", "").trim().ifBlank { pretty }
    }
    val id = osId.lowercase().trim()
    if (id.isEmpty() || id == "unknown") return ""
    return id.replaceFirstChar { it.uppercase() }
}
