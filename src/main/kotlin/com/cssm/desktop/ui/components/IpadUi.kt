package com.cssm.desktop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/** iPad 风格的 iOS 蓝：浅色 #007AFF / 深色 #0A84FF */
@Composable
fun ipadBlue(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF0A84FF)
    else Color(0xFF007AFF)

/** iPad 圆环状态色：<60 绿，60~85 橙，>85 红 */
fun statusColor(pct: Double): Color = when {
    pct < 60 -> Color(0xFF34C77B)
    pct < 85 -> Color(0xFFFF9F0A)
    else -> Color(0xFFFF453A)
}

/** 大标题，如「指标」 */
@Composable
fun IpadTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

/** 地区分段选择：ALL | 🇺🇸美国 | 🇵🇱波兰 */
@Composable
fun RegionSegment(
    regions: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(17.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SegmentItem(label = "ALL", selected = selected == null, onClick = { onSelect(null) })
        regions.forEach { (region, flag) ->
            SegmentItem(
                flag = flag,
                label = region,
                selected = selected == region,
                onClick = { onSelect(region) }
            )
        }
    }
}

@Composable
private fun SegmentItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    flag: String = "",
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (flag.isNotBlank()) {
                FlagImage(flag = flag, height = 12.dp)
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** iPad 风格圆环：标题在上，百分比在环内（状态色），副标题在下 */
@Composable
fun IpadRing(
    label: String,
    pct: Double,
    sub: String,
    size: Dp = 52.dp,
    stroke: Dp = 6.dp,
    valueText: String? = null
) {
    val c = statusColor(pct)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val s = stroke.toPx()
                drawArc(
                    color = track,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(s, cap = StrokeCap.Round)
                )
                val f = min(1.0, pct.coerceIn(0.0, 100.0) / 100.0).toFloat()
                if (f > 0f) {
                    drawArc(
                        color = c,
                        startAngle = -90f,
                        sweepAngle = 360f * f,
                        useCenter = false,
                        style = Stroke(s, cap = StrokeCap.Round)
                    )
                }
            }
            Text(
                text = valueText ?: "${pct.toInt()}%",
                fontSize = (size.value * 0.24f).sp,
                fontWeight = FontWeight.SemiBold,
                color = c
            )
        }
        if (sub.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = sub,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 白卡容器（圆角 16dp，无阴影） */
@Composable
fun IpadCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = { Column(content = content) }
    )
}

/** iPad 风格子面板：卡片内的浅灰圆角底，装一组指标（圆环 / 网络 / I/O） */
@Composable
fun IpadSubPanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

/** 彩色圆角图标（更多页用）：36dp 圆角方块 + 白色 glyph */
@Composable
fun ColoredIcon(
    icon: ImageVector,
    bg: Color,
    modifier: Modifier = Modifier,
    boxSize: Dp = 34.dp,
    iconSize: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(boxSize)
            .clip(RoundedCornerShape(9.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** 更多页行：彩色图标 + 文字（+副标题）+ 右箭头 */
@Composable
fun IpadMoreRow(
    icon: ImageVector,
    iconBg: Color,
    label: String,
    subtitle: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColoredIcon(icon = icon, bg = iconBg)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = "›",
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

/** iPad 风格分组标题：彩色小图标 + 文字（+右侧可折叠箭头） */
@Composable
fun IpadSectionHeader(
    icon: ImageVector,
    iconTint: Color,
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null
) {
    val rowMod = if (onToggle != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    }
    Row(
        modifier = rowMod,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        if (expanded != null) {
            Text(
                text = if (expanded) "﹀" else "›",
                fontSize = 16.sp,
                color = ipadBlue()
            )
        }
    }
}
