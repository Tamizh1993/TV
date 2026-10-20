@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.tamizh.tvlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import com.tamizh.tvlauncher.LauncherSettings

// ---------------- Theme & colours ----------------

val Accents = listOf(
    Color(0xFF0067C0), Color(0xFF2F7D32), Color(0xFFC239B3), Color(0xFFCA5010),
    Color(0xFF038387), Color(0xFF8764B8), Color(0xFFE81123), Color(0xFF4A5459),
)

fun LauncherSettings.accent(): Color = Accents.getOrElse(accentIndex) { Accents[0] }

/** p > 0 mixes toward white, p < 0 toward black. */
fun Color.shade(p: Float): Color = if (p >= 0) lerp(this, Color.White, p) else lerp(this, Color.Black, -p)

/** Light accent used for indicators, toggles and selected states. */
fun Color.light(): Color = shade(0.35f)

val GlassDark = Color(0xE0242428)
val GlassBorder = Color(0x14FFFFFF)

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF3B8EEA),
            background = Color.Black,
            onBackground = Color.White,
            surface = Color(0xFF202024),
            onSurface = Color.White,
        ),
        content = content,
    )
}

// ---------------- Wallpaper ----------------

data class Glow(val color: Color, val cx: Float, val cy: Float, val radius: Float)

data class WallpaperPreset(val name: String, val base: List<Color>, val glows: List<Glow>)

val WallpaperPresets = listOf(
    WallpaperPreset(
        "Glow", listOf(Color(0xFF0B1830), Color(0xFF0D2A57)),
        listOf(Glow(Color(0xFF2B6CC4), .75f, .35f, .6f), Glow(Color(0xFF1B3D7A), .2f, .9f, .5f)),
    ),
    WallpaperPreset(
        "Dusk", listOf(Color(0xFF1B1030), Color(0xFF2D1A4A)),
        listOf(Glow(Color(0xFFC4567A), .8f, .2f, .55f), Glow(Color(0xFF3B2A7A), .1f, .8f, .5f)),
    ),
    WallpaperPreset(
        "Lagoon", listOf(Color(0xFF06222C), Color(0xFF0A3A48)),
        listOf(Glow(Color(0xFF1FA39A), .7f, .3f, .55f), Glow(Color(0xFF0D4A6E), .2f, .9f, .5f)),
    ),
    WallpaperPreset(
        "Ember", listOf(Color(0xFF1F0D08), Color(0xFF3A150C)),
        listOf(Glow(Color(0xFFD0602A), .75f, .4f, .55f), Glow(Color(0xFF6A1E1E), .15f, .85f, .5f)),
    ),
    WallpaperPreset(
        "Slate", listOf(Color(0xFF111318), Color(0xFF1F2430)),
        listOf(Glow(Color(0xFF4B5563), .7f, .3f, .6f)),
    ),
)

fun Modifier.wallpaper(preset: WallpaperPreset): Modifier = this
    .background(Brush.linearGradient(preset.base))
    .drawBehind {
        for (g in preset.glows) {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(g.color, Color.Transparent),
                    center = Offset(size.width * g.cx, size.height * g.cy),
                    radius = size.width * g.radius,
                )
            )
        }
    }

@Composable
fun WallpaperBackground(settings: LauncherSettings, image: ImageBitmap?) {
    Box(Modifier.fillMaxSize()) {
        if (image != null) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        } else {
            val preset = WallpaperPresets.getOrElse(settings.wallpaperPreset) { WallpaperPresets[0] }
            Box(Modifier.fillMaxSize().wallpaper(preset))
        }
    }
}

// ---------------- Focusable building blocks (Windows 11 style) ----------------

/** Flat button with a white focus ring, used in the taskbar, menus and settings. */
@Composable
fun WinButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
    container: Color = Color.Transparent,
    focusedContainer: Color = Color.White.copy(alpha = 0.14f),
    focusedContent: Color = Color.White,
    scale: Float = 1.0f,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = Color.White,
            focusedContainerColor = focusedContainer,
            focusedContentColor = focusedContent,
            pressedContainerColor = focusedContainer,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = scale),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(border = BorderStroke(2.dp, Color.White), shape = shape)
        ),
    ) { content() }
}

/** Segmented option ("12-hour", "Celsius", city names…). */
@Composable
fun SegButton(label: String, selected: Boolean, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    WinButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(5.dp),
        container = if (selected) accent.light() else Color.White.copy(alpha = 0.06f),
        focusedContainer = if (selected) accent.light() else Color.White.copy(alpha = 0.16f),
        focusedContent = if (selected) Color(0xFF000011) else Color.White,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color(0xFF000011) else Color.Unspecified,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

/** Windows 11 style on/off switch. */
@Composable
fun WinToggle(on: Boolean, accent: Color, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    WinButton(onClick = onToggle, modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (on) "On" else "Off", fontSize = 13.sp)
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier
                    .size(width = 40.dp, height = 20.dp)
                    .background(if (on) accent.light() else Color.Transparent, RoundedCornerShape(10.dp))
                    .border(1.dp, if (on) accent.light() else Color.White.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
            ) {
                Box(
                    Modifier
                        .offset(x = if (on) 24.dp else 4.dp, y = 4.dp)
                        .size(10.dp)
                        .background(if (on) Color(0xFF000011) else Color.White, CircleShape)
                )
            }
        }
    }
}
