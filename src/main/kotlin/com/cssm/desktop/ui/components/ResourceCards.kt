package com.cssm.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cssm.desktop.data.Server
import kotlin.math.absoluteValue

/**
 * 资源库风格卡片（Tony iPad 实拍参照）：淡彩渐变底圆角卡片。
 * 服务器卡：左上「名称 + 红点」，右上系统徽标，左下两行小字（连接目标 / 系统），右下相对时间。
 * 文件夹卡：左上名称，右上文件夹图标，左下小字说明。
 */

/** 淡彩渐变色板（浅绿 / 浅粉 / 浅紫 / 浅蓝 / 浅黄），按 server id 稳定取色 */
private val cardPalettes = listOf(
    Color(0xFFE7F6EC) to Color(0xFFF6FCF3),
    Color(0xFFFDECEB) to Color(0xFFFFF7F5),
    Color(0xFFF1E7FC) to Color(0xFFFAF3FF),
    Color(0xFFE7F1FD) to Color(0xFFF1F8FF),
    Color(0xFFFFF3D9) to Color(0xFFFFFBEE),
)

/** 深色版色板：保留同色系的色相，加深为暗面底 */
private val cardPalettesDark = listOf(
    Color(0xFF16281E) to Color(0xFF122016),
    Color(0xFF2E1B1A) to Color(0xFF251715),
    Color(0xFF271E33) to Color(0xFF201827),
    Color(0xFF1A2431) to Color(0xFF141D28),
    Color(0xFF2F2812) to Color(0xFF26200F),
)

/** 文件夹卡浅色渐变 */
private val folderGradientLight = listOf(Color(0xFFFFF6DC), Color(0xFFFFFDF4))

/** 文件夹卡深色渐变 */
private val folderGradientDark = listOf(Color(0xFF2F2812), Color(0xFF26200F))

/** 当前是否为深色主题（按背景亮度判断，跟随系统的三档外观都适用） */
@Composable
private fun isDarkTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun Server.cardPalette(): Pair<Color, Color> {
    val palettes = if (isDarkTheme()) cardPalettesDark else cardPalettes
    return palettes[(id.hashCode().absoluteValue) % palettes.size]
}

/** 两种卡片统一高度：名称顶对齐，底部信息沉底 */
private val cardHeight = 112.dp

/** 相对时间：刚刚 / X分钟前 / X小时前 / X天前 */
fun relativeTime(ts: Long): String {
    if (ts <= 0) return "未连接"
    val m = (System.currentTimeMillis() - ts) / 60000
    return when {
        m < 1 -> "刚刚"
        m < 60 -> "${m}分钟前"
        m < 1440 -> "${m / 60}小时前"
        else -> "${m / 1440}天前"
    }
}

@Composable
fun ServerResourceCard(
    server: Server,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (c1, c2) = server.cardPalette()
    val gray = MaterialTheme.colorScheme.onSurfaceVariant
    val titleColor = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(c1, c2)))
            .clickable(onClick = onClick)
            .height(cardHeight)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServerNameWithFlag(
                    server = server,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                )
                Spacer(Modifier.width(10.dp))
                OsBadge(osId = server.osId, size = 30.dp)
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.displayTarget,
                        fontSize = 11.sp,
                        color = gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = server.osName.ifBlank { "未知系统" },
                        fontSize = 11.sp,
                        color = gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = relativeTime(server.lastConnectedAt),
                    fontSize = 11.sp,
                    color = gray,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun FolderResourceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gray = MaterialTheme.colorScheme.onSurfaceVariant
    val titleColor = MaterialTheme.colorScheme.onSurface
    val gradient = if (isDarkTheme()) folderGradientDark else folderGradientLight
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(gradient)
            )
            .clickable(onClick = onClick)
            .height(cardHeight)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
