@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.tamizh.tvlauncher.ui

import android.text.format.DateUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.tamizh.tvlauncher.LauncherSettings
import com.tamizh.tvlauncher.TileKind
import com.tamizh.tvlauncher.TileSize
import com.tamizh.tvlauncher.TileSpec
import com.tamizh.tvlauncher.data.Weather
import com.tamizh.tvlauncher.data.describeWeather
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

val TileUnit = 56.dp
val TileGap = 8.dp
private val TileShape = RoundedCornerShape(8.dp)

fun units(n: Int): Dp = TileUnit * n + TileGap * (n - 1)

/** Everything a tile needs to draw its live content. */
data class TileContext(
    val now: Long,
    val weather: Weather?,
    val recents: Map<String, Long>,
    val settings: LauncherSettings,
)

@Composable
fun LiveTile(
    tile: TileSpec,
    ctx: TileContext,
    flipped: Boolean,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val accent = ctx.settings.accent()
    val base = when (val k = tile.kind) {
        is TileKind.App -> k.app.tileColor?.let { Color(it) } ?: accent
        else -> accent
    }
    val alpha = if (ctx.settings.transparentTiles) 0.6f else 1f
    val brush = Brush.linearGradient(listOf(base.shade(0.12f).copy(alpha = alpha), base.shade(-0.18f).copy(alpha = alpha)))

    val progress by animateFloatAsState(
        targetValue = if (flipped) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "tileFlip",
    )

    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier
            .size(units(tile.size.w), units(tile.size.h))
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        shape = ClickableSurfaceDefaults.shape(shape = TileShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = Color.White,
            pressedContainerColor = Color.Transparent,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f, pressedScale = 0.96f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(border = BorderStroke(2.dp, Color.White), shape = TileShape)
        ),
    ) {
        Box(Modifier.fillMaxSize().clip(TileShape).background(brush)) {
            // Front face slides up and out; back face slides in from below.
            Box(Modifier.fillMaxSize().graphicsLayer { translationY = -progress * size.height }) {
                FrontFace(tile, ctx)
            }
            if (progress > 0f) {
                Box(Modifier.fillMaxSize().graphicsLayer { translationY = (1f - progress) * size.height }) {
                    BackFace(tile, ctx)
                }
            }
        }
    }
}

// ---------------- Faces ----------------

@Composable
private fun BoxScope.TileName(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, end = 10.dp, bottom = 7.dp),
    )
}

@Composable
private fun FrontFace(tile: TileSpec, ctx: TileContext) {
    val size = tile.size
    Box(Modifier.fillMaxSize()) {
        when (val k = tile.kind) {
            TileKind.Clock -> ClockFace(size, ctx)
            TileKind.Weather -> WeatherNow(size, ctx)
            TileKind.LauncherSettings -> {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(if (size == TileSize.S) 24.dp else 36.dp),
                )
                if (size != TileSize.S) TileName(tile.name)
            }
            is TileKind.SystemShortcut -> {
                Text(k.glyph, fontSize = if (size == TileSize.S) 22.sp else 32.sp, modifier = Modifier.align(Alignment.Center))
                if (size != TileSize.S) TileName(tile.name)
            }
            is TileKind.App -> {
                val app = k.app
                if ((size == TileSize.W || size == TileSize.L) && app.banner != null) {
                    Image(app.banner, contentDescription = app.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    val iconSize = when (size) { TileSize.S -> 30.dp; TileSize.M -> 44.dp; else -> 60.dp }
                    if (app.icon != null) {
                        Image(
                            app.icon,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(bottom = if (size == TileSize.S) 0.dp else 10.dp)
                                .size(iconSize),
                        )
                    } else {
                        Text(app.label.take(1), fontSize = 30.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
                    }
                    if (size != TileSize.S) TileName(app.label)
                }
            }
        }
    }
}

@Composable
private fun BackFace(tile: TileSpec, ctx: TileContext) {
    Box(Modifier.fillMaxSize()) {
        when (val k = tile.kind) {
            TileKind.Clock -> {
                if (tile.size == TileSize.L) MonthCalendar(ctx.now, ctx.settings.accent())
                else Column(Modifier.align(Alignment.CenterStart).padding(horizontal = 12.dp)) {
                    Text(fmt("EEEE", ctx.now), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(fmt("d MMMM", ctx.now), fontSize = if (tile.size == TileSize.M) 14.sp else 22.sp)
                }
                TileName("Calendar")
            }
            TileKind.Weather -> {
                WeatherForecast(tile.size, ctx)
                TileName("Forecast · ${ctx.settings.cityName}")
            }
            is TileKind.App -> {
                val last = ctx.recents[k.app.packageName]
                Column(Modifier.align(Alignment.CenterStart).padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    Text("LAST OPENED", fontSize = 10.sp, letterSpacing = 0.8.sp, modifier = Modifier.alpha(0.75f))
                    Text(
                        if (last != null) DateUtils.getRelativeTimeSpanString(last, ctx.now, DateUtils.MINUTE_IN_MILLIS).toString() else "—",
                        fontSize = if (tile.size == TileSize.M) 14.sp else 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                }
                TileName(k.app.label)
            }
            else -> FrontFace(tile, ctx)
        }
    }
}

// ---------------- Clock & calendar ----------------

private fun fmt(pattern: String, time: Long): String = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(time))

@Composable
private fun BoxScope.ClockFace(size: TileSize, ctx: TileContext) {
    val h24 = ctx.settings.use24h
    val time = fmt(if (h24) "HH:mm" else "h:mm", ctx.now)
    val amPm = if (h24) "" else fmt("a", ctx.now)
    val (big, small) = when (size) {
        TileSize.L -> 60.sp to 18.sp
        TileSize.W -> 44.sp to 15.sp
        else -> 26.sp to 11.sp
    }
    Column(Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = if (size == TileSize.M) 24.dp else 28.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(time, fontSize = big, fontWeight = FontWeight.Light)
            if (amPm.isNotEmpty()) Text(" $amPm", fontSize = small, modifier = Modifier.padding(bottom = 6.dp))
        }
        if (size != TileSize.M) Text(fmt("EEEE, d MMMM", ctx.now), fontSize = if (size == TileSize.L) 16.sp else 13.sp)
    }
    TileName("Clock")
}

@Composable
private fun MonthCalendar(now: Long, accent: Color) {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val today = cal.get(Calendar.DAY_OF_MONTH)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val lead = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY // 0..6
    val cells: List<Int?> = List(lead) { null } + (1..daysInMonth).toList()

    Column(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 28.dp)) {
        Text(fmt("MMMM yyyy", now), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        CalRow(listOf("S", "M", "T", "W", "T", "F", "S").map { it to false }, header = true, accent = accent)
        cells.chunked(7).forEach { week ->
            CalRow(week.map { (it?.toString() ?: "") to (it == today) } + List(7 - week.size) { "" to false }, header = false, accent = accent)
        }
    }
}

@Composable
private fun CalRow(cells: List<Pair<String, Boolean>>, header: Boolean, accent: Color) {
    Row(Modifier.fillMaxWidth()) {
        cells.forEach { (text, isToday) ->
            Box(Modifier.weight(1f).height(26.dp), contentAlignment = Alignment.Center) {
                if (isToday) Box(Modifier.size(22.dp).background(Color.White, CircleShape))
                Text(
                    text,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = if (header || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) accent else Color.White.copy(alpha = if (header) 0.7f else 1f),
                )
            }
        }
    }
}

// ---------------- Weather ----------------

private fun LauncherSettings.temp(c: Double): String =
    if (c.isNaN()) "--" else (if (fahrenheit) (c * 9 / 5 + 32) else c).roundToInt().toString() + "°"

@Composable
private fun BoxScope.WeatherNow(size: TileSize, ctx: TileContext) {
    val w = ctx.weather
    if (w == null) {
        Text("Loading weather…", fontSize = 13.sp, modifier = Modifier.align(Alignment.Center).alpha(0.8f))
        TileName("Weather")
        return
    }
    val (emoji, desc) = describeWeather(w.code, w.isDay)
    val compact = size == TileSize.M
    Row(
        modifier = Modifier.align(if (compact) Alignment.Center else Alignment.CenterStart)
            .padding(start = if (compact) 0.dp else 16.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = if (compact) 26.sp else 44.sp)
        Spacer(Modifier.width(if (compact) 6.dp else 14.dp))
        Column {
            Text(ctx.settings.temp(w.temperatureC), fontSize = if (compact) 26.sp else 44.sp, fontWeight = FontWeight.Light)
            if (!compact) Text("$desc · ${ctx.settings.cityName}", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    TileName(if (compact) ctx.settings.cityName else "Weather")
}

@Composable
private fun BoxScope.WeatherForecast(size: TileSize, ctx: TileContext) {
    val days = ctx.weather?.forecast.orEmpty()
    if (days.isEmpty()) {
        Text("Forecast unavailable", fontSize = 13.sp, modifier = Modifier.align(Alignment.Center).alpha(0.8f))
        return
    }
    val shown = if (size == TileSize.M) days.take(2) else days.take(4)
    Row(
        Modifier.fillMaxWidth().align(Alignment.Center).padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        shown.forEach { d ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(d.day, fontSize = 12.sp)
                Text(describeWeather(d.code, true).first, fontSize = if (size == TileSize.M) 18.sp else 22.sp)
                Text(ctx.settings.temp(d.maxC), fontSize = 13.sp)
            }
        }
    }
}

