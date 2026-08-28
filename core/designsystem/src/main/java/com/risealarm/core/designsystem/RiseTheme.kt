package com.risealarm.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAlarm
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val RiseBackground = Color(0xFF080B10)
val RiseSurface = Color(0xFF111722)
val RiseSurfaceHigh = Color(0xFF18202D)
val RisePrimary = Color(0xFFFF6B35)
val RiseSecondary = Color(0xFF7C5CFF)
val RiseVerified = Color(0xFF32D6A0)
val RiseWarning = Color(0xFFFFB547)
val RiseError = Color(0xFFFF5364)
val RiseText = Color(0xFFF5F7FA)
val RiseTextMuted = Color(0xFF98A2B3)
val RiseOutline = Color(0xFF273244)

private val RiseColors = darkColorScheme(
    primary = RisePrimary,
    onPrimary = Color(0xFF271000),
    secondary = RiseSecondary,
    onSecondary = Color.White,
    tertiary = RiseVerified,
    onTertiary = Color(0xFF002115),
    background = RiseBackground,
    onBackground = RiseText,
    surface = RiseSurface,
    onSurface = RiseText,
    surfaceVariant = RiseSurfaceHigh,
    onSurfaceVariant = RiseTextMuted,
    outline = RiseOutline,
    error = RiseError,
    onError = Color.White,
)

private val RiseTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 64.sp, lineHeight = 68.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 52.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
)

@Composable
fun RiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RiseColors,
        typography = RiseTypography,
        shapes = androidx.compose.material3.Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
        ),
        content = content,
    )
}

object RiseSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

@Composable
fun RiseGradientBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF10101A), RiseBackground, RiseBackground),
                ),
            ),
    ) { content() }
}

@Composable
fun RiseCard(
    modifier: Modifier = Modifier,
    containerColor: Color = RiseSurface,
    contentPadding: PaddingValues = PaddingValues(RiseSpacing.md),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun RisePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = RisePrimary,
    leadingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = if (color == RisePrimary || color == RiseWarning) Color(0xFF241006) else Color.White,
        ),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(15.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun IconBadge(
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (action != null) Text(action, color = RisePrimary, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    supporting: String,
    modifier: Modifier = Modifier,
) {
    RiseCard(modifier = modifier) {
        IconBadge(icon, color, size = 40.dp)
        Spacer(Modifier.height(14.dp))
        Text(label, color = RiseTextMuted, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(supporting, color = color, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

enum class RiseTopLevel(val label: String, val icon: ImageVector) {
    Home("Trang chủ", Icons.Rounded.Home),
    Alarms("Báo thức", Icons.Rounded.AddAlarm),
    Settings("Cài đặt", Icons.Rounded.Settings),
}

@Composable
fun RiseBottomBar(selected: RiseTopLevel, onSelected: (RiseTopLevel) -> Unit) {
    NavigationBar(containerColor = Color(0xFF0D121B), tonalElevation = 0.dp) {
        RiseTopLevel.entries.forEach { item ->
            NavigationBarItem(
                selected = selected == item,
                onClick = { onSelected(item) },
                icon = { Icon(item.icon, item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RisePrimary,
                    selectedTextColor = RiseText,
                    indicatorColor = RisePrimary.copy(alpha = 0.15f),
                    unselectedIconColor = RiseTextMuted,
                    unselectedTextColor = RiseTextMuted,
                ),
            )
        }
    }
}

@Composable
fun RiseMainScaffold(
    selected: RiseTopLevel,
    onSelected: (RiseTopLevel) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = RiseBackground,
        bottomBar = { RiseBottomBar(selected, onSelected) },
        content = content,
    )
}
