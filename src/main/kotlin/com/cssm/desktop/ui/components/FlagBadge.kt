package com.cssm.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server
import org.jetbrains.skia.Image as SkiaImage

/** 国旗 emoji（区域指示符对，如 🇦🇺）→ 小写 ISO 代码（"au"）；非国旗返回 null */
fun flagCodeFromEmoji(flag: String): String? {
    val cps = flag.trim().codePoints().toArray()
    if (cps.size != 2) return null
    if (cps.any { it !in 0x1F1E6..0x1F1FF }) return null
    return cps.map { (it - 0x1F1E6 + 'A'.code).toChar() }
        .joinToString("").lowercase()
}

private val flagBitmapCache = mutableMapOf<String, androidx.compose.ui.graphics.ImageBitmap?>()

private fun flagBitmap(code: String): androidx.compose.ui.graphics.ImageBitmap? =
    flagBitmapCache.getOrPut(code) {
        val b64 = FLAG_IMAGE_BASE64[code] ?: return@getOrPut null
        try {
            val bytes = java.util.Base64.getDecoder().decode(b64)
            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

/**
 * 国旗图片：Windows 不渲染旗帜 emoji（只显示 AU 字母），这里用真实国旗图片代替。
 * 没有收录的国家显示双字母徽标兜底；🌐 这类普通 emoji 直接显示原文。
 */
@Composable
fun FlagImage(
    flag: String,
    height: Dp = 14.dp,
    modifier: Modifier = Modifier,
) {
    val f = flag.trim()
    if (f.isEmpty()) return
    val code = flagCodeFromEmoji(f)
    val bitmap = code?.let { flagBitmap(it) }
    when {
        bitmap != null -> Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = modifier
                .height(height)
                .width(height * 1.5f)
                .clip(RoundedCornerShape(3.dp))
        )
        code != null -> Box(
            modifier = modifier
                .height(height)
                .width(height * 1.5f)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF5B8DEF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = code.uppercase(),
                fontSize = (height.value * 0.55f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
        else -> Text(text = f, fontSize = (height.value * 1.1f).sp, maxLines = 1)
    }
}

/** iPad 风格服务器名（不含国旗）：地区/名称去重逻辑 */
fun Server.ipadBaseName(): String {
    val r = region.trim()
    val n = name.trim().ifBlank { host }
    return when {
        r.isEmpty() -> n
        r.startsWith(n) -> r   // 如地区「澳洲AU」已含名称「澳洲」，只显示地区
        n.startsWith(r) -> n   // 名称已含地区时只显示名称
        else -> "$r$n"
    }
}

/** 服务器名（国旗图片在前）+ 可选尾部内容 */
@Composable
fun ServerNameWithFlag(
    server: Server,
    fontSize: TextUnit = 15.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    color: Color = MaterialTheme.colorScheme.onSurface,
    flagHeight: Dp = 14.dp,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val flag = server.regionFlag.trim()
        if (flag.isNotEmpty()) {
            FlagImage(flag = flag, height = flagHeight)
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = server.ipadBaseName(),
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
