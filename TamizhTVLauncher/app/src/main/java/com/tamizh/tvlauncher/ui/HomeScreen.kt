@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.tamizh.tvlauncher.ui

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings as SysSettings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.tamizh.tvlauncher.GRID_ROWS
import com.tamizh.tvlauncher.LauncherViewModel
import com.tamizh.tvlauncher.Screen
import com.tamizh.tvlauncher.TileKind
import com.tamizh.tvlauncher.TileSpec
import com.tamizh.tvlauncher.appTileId
import com.tamizh.tvlauncher.buildTileGroups
import com.tamizh.tvlauncher.data.AppInfo
import com.tamizh.tvlauncher.data.describeWeather
import com.tamizh.tvlauncher.isLiveCapable
import com.tamizh.tvlauncher.packGroup
import com.tamizh.tvlauncher.startSafely
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private sealed interface Overlay {
    data class Menu(val tile: TileSpec) : Overlay
    data class Apps(val search: Boolean) : Overlay
}

@Composable
fun HomeScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val firstFocus = remember { FocusRequester() }
    var overlay by remember { mutableStateOf<Overlay?>(null) }
    val now by rememberNow()
    val s = vm.settings

    // We ARE the home screen: Back has nowhere to go.
    BackHandler(enabled = true) { }

    val groups = remember(vm.apps, vm.favourites, vm.recents, vm.tileSizes, vm.hiddenTiles) {
        buildTileGroups(vm.apps, vm.favourites, vm.recents, vm.tileSizes, vm.hiddenTiles).map { packGroup(it) }
    }
    val ctx = TileContext(now = now, weather = vm.weather, recents = vm.recents, settings = s)

    // ---- Live tiles: flip a random live tile every couple of seconds ----
    var flipped by remember { mutableStateOf(emptySet<String>()) }
    val liveIds = remember(groups, vm.liveOffTiles, s.liveTiles, vm.recents) {
        if (!s.liveTiles) emptyList()
        else groups.flatMap { g -> g.tiles.map { it.tile } }
            .filter { it.isLiveCapable(vm.recents) && it.id !in vm.liveOffTiles }
            .map { it.id }
    }
    val currentLive by rememberUpdatedState(liveIds)
    LaunchedEffect(Unit) {
        while (true) {
            delay(2600)
            val ids = currentLive
            flipped = flipped.intersect(ids.toSet())
            if (ids.isNotEmpty() && overlay == null) {
                val id = ids.random()
                flipped = if (id in flipped) flipped - id else flipped + id
            }
        }
    }

    // ---- Focus the first tile on start and whenever HOME is pressed again ----
    LaunchedEffect(vm.homeResetSignal, vm.loaded) {
        if (vm.loaded) {
            overlay = null
            scroll.animateScrollTo(0)
            delay(80)
            runCatching { firstFocus.requestFocus() }
        }
    }

    val firstTileId = groups.firstOrNull()?.tiles?.firstOrNull()?.tile?.id

    fun openTile(tile: TileSpec) {
        when (val k = tile.kind) {
            is TileKind.App -> vm.launch(context, k.app)
            TileKind.LauncherSettings -> vm.screen = Screen.Settings
            is TileKind.SystemShortcut -> context.startSafely(Intent(k.action), Intent(SysSettings.ACTION_SETTINGS))
            TileKind.Clock -> context.startSafely(Intent(AlarmClock.ACTION_SHOW_ALARMS), Intent(SysSettings.ACTION_DATE_SETTINGS))
            TileKind.Weather -> vm.screen = Screen.Settings
        }
    }

    Box(Modifier.fillMaxSize()) {
        WallpaperBackground(s, vm.wallpaperImage)

        Column(Modifier.fillMaxSize()) {
            // ---- Header ----
            Row(
                Modifier.fillMaxWidth().height(66.dp).padding(start = 40.dp, top = 18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(greeting(now), fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(14.dp))
                Text(
                    SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 14.dp).alpha(0.75f),
                )
            }

            // ---- Tile groups (scroll sideways) ----
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .horizontalScroll(scroll)
                    .padding(start = 40.dp, end = 40.dp, top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(44.dp),
            ) {
                if (!vm.loaded) Text("Loading apps…", fontSize = 16.sp, modifier = Modifier.padding(top = 30.dp))
                groups.forEach { group ->
                    Column {
                        Text(
                            group.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.height(26.dp).padding(start = 2.dp).alpha(0.85f),
                        )
                        Box(Modifier.size(width = units(group.columns.coerceAtLeast(1)), height = units(GRID_ROWS))) {
                            group.tiles.forEach { placed ->
                                val tile = placed.tile
                                LiveTile(
                                    tile = tile,
                                    ctx = ctx,
                                    flipped = tile.id in flipped,
                                    modifier = Modifier.offset(
                                        x = (TileUnit + TileGap) * placed.x,
                                        y = (TileUnit + TileGap) * placed.y,
                                    ),
                                    focusRequester = if (tile.id == firstTileId) firstFocus else null,
                                    onClick = { openTile(tile) },
                                    onLongClick = { overlay = Overlay.Menu(tile) },
                                )
                            }
                        }
                    }
                }
            }

            // ---- Taskbar ----
            Taskbar(
                vm = vm,
                now = now,
                onHome = { vm.goHome() },
                onSearch = { overlay = Overlay.Apps(search = true) },
                onAllApps = { overlay = Overlay.Apps(search = false) },
                onSettings = { vm.screen = Screen.Settings },
                onLaunch = { vm.launch(context, it) },
            )
        }
    }

    // ---- Overlays ----
    when (val o = overlay) {
        is Overlay.Menu -> {
            val tile = o.tile
            val close: () -> Unit = { overlay = null }
            val app = (tile.kind as? TileKind.App)?.app
            val entries = buildList<MenuEntry?> {
                add(MenuEntry("↗", "Open") { close(); openTile(tile) })
                add(null)
                if (tile.allowedSizes.size > 1) {
                    val next = tile.allowedSizes[(tile.allowedSizes.indexOf(tile.size) + 1) % tile.allowedSizes.size]
                    add(MenuEntry("⤢", "Resize", "${tile.size.label} → ${next.label}") {
                        vm.setTileSize(tile.id, next)
                        overlay = Overlay.Menu(tile.copy(size = next)) // keep the menu open to cycle sizes
                    })
                }
                if (tile.kind is TileKind.Clock || tile.kind is TileKind.Weather || app != null) {
                    val off = tile.id in vm.liveOffTiles
                    add(MenuEntry("◐", if (off) "Turn live tile on" else "Turn live tile off") { vm.toggleLive(tile.id); close() })
                }
                if (app != null && app.packageName in vm.favourites) {
                    val i = vm.favourites.indexOf(app.packageName)
                    if (i > 0) add(MenuEntry("←", "Move earlier") { vm.moveFavourite(app.packageName, -1); close() })
                    if (i < vm.favourites.size - 1) add(MenuEntry("→", "Move later") { vm.moveFavourite(app.packageName, 1); close() })
                }
                if (app != null && app.packageName !in vm.favourites) {
                    add(MenuEntry("📌", "Pin to Pinned group") { vm.pinApp(app.packageName); close() })
                }
                add(MenuEntry("✕", "Unpin from Home") {
                    vm.unpin(tile.id)
                    close()
                    Toast.makeText(context, "Unpinned. Restore it from All apps or Settings.", Toast.LENGTH_SHORT).show()
                })
                if (app != null) {
                    val pkgUri = Uri.parse("package:${app.packageName}")
                    add(null)
                    add(MenuEntry("ⓘ", "App info") { close(); context.startSafely(Intent(SysSettings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)) })
                    add(MenuEntry("🗑", "Uninstall") { close(); context.startSafely(Intent(Intent.ACTION_DELETE, pkgUri)) })
                }
            }
            TileMenu(title = tile.name, entries = entries, accent = s.accent(), onDismiss = close)
        }
        is Overlay.Apps -> AllAppsPanel(
            apps = vm.apps,
            isPinned = { vm.isPinned(it) },
            focusSearch = o.search,
            onLaunch = { overlay = null; vm.launch(context, it) },
            onTogglePin = { app ->
                if (vm.isPinned(app.packageName)) {
                    vm.unpin(appTileId(app.packageName))
                    Toast.makeText(context, "${app.label} unpinned", Toast.LENGTH_SHORT).show()
                } else {
                    vm.pinApp(app.packageName)
                    Toast.makeText(context, "${app.label} pinned to Home", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { overlay = null },
        )
        null -> Unit
    }
}

private fun greeting(now: Long): String {
    val h = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    return when {
        h < 12 -> "Good morning"
        h < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

/** Current time, ticking on every second boundary. */
@Composable
fun rememberNow(): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val t = System.currentTimeMillis()
            now.longValue = t
            delay(1000 - t % 1000)
        }
    }
    return now
}

// ---------------- Taskbar ----------------

@Composable
private fun Taskbar(
    vm: LauncherViewModel,
    now: Long,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onAllApps: () -> Unit,
    onSettings: () -> Unit,
    onLaunch: (AppInfo) -> Unit,
) {
    val s = vm.settings
    val accent = s.accent()
    val byPkg = remember(vm.apps) { vm.apps.associateBy { it.packageName } }
    val recentApps = vm.recents.entries.sortedByDescending { it.value }.mapNotNull { byPkg[it.key] }.take(3)

    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(GlassDark.copy(alpha = 0.78f)),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))

        // Left: weather
        Row(Modifier.align(Alignment.CenterStart).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            val w = vm.weather
            Text(if (w != null) describeWeather(w.code, w.isDay).first else "🌡️", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Column {
                val t = w?.let { (if (s.fahrenheit) it.temperatureC * 9 / 5 + 32 else it.temperatureC).roundToInt() }
                Text(if (t != null) "$t°${if (s.fahrenheit) "F" else "C"}" else "--", fontSize = 13.sp)
                Text(s.cityName, fontSize = 11.sp, modifier = Modifier.alpha(0.7f))
            }
        }

        // Centre: buttons + recently used apps
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TaskbarButton(onClick = onHome, accent = accent, active = true) { Icon(Icons.Filled.Home, "Home", Modifier.size(22.dp)) }
            TaskbarButton(onClick = onSearch, accent = accent) { Icon(Icons.Filled.Search, "Search", Modifier.size(22.dp)) }
            TaskbarButton(onClick = onAllApps, accent = accent) { AppsGlyph() }
            TaskbarButton(onClick = onSettings, accent = accent) { Icon(Icons.Filled.Settings, "Settings", Modifier.size(22.dp)) }
            recentApps.forEach { app ->
                TaskbarButton(onClick = { onLaunch(app) }, accent = accent, running = true) {
                    if (app.icon != null) Image(app.icon, contentDescription = app.label, modifier = Modifier.size(24.dp))
                    else Text(app.label.take(1), fontSize = 14.sp)
                }
            }
        }

        // Right: clock
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 16.dp), horizontalAlignment = Alignment.End) {
            val d = Date(now)
            Text(SimpleDateFormat(if (s.use24h) "HH:mm" else "h:mm a", Locale.getDefault()).format(d), fontSize = 12.sp)
            Text(SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(d), fontSize = 12.sp)
        }
    }
}

@Composable
private fun TaskbarButton(
    onClick: () -> Unit,
    accent: Color,
    active: Boolean = false,
    running: Boolean = false,
    content: @Composable () -> Unit,
) {
    WinButton(onClick = onClick, modifier = Modifier.size(width = 44.dp, height = 42.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
            if (active || running) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .size(width = if (active) 16.dp else 6.dp, height = 3.dp)
                        .background(if (active) accent.light() else Color(0xFF99AABB), RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

/** 3×3 dots "All apps" glyph (not in material-icons-core). */
@Composable
private fun AppsGlyph() {
    Canvas(Modifier.size(20.dp)) {
        val cell = size.width / 3f
        val dot = cell * 0.62f
        for (i in 0 until 3) for (j in 0 until 3) {
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(i * cell + (cell - dot) / 2, j * cell + (cell - dot) / 2),
                size = Size(dot, dot),
                cornerRadius = CornerRadius(dot * 0.25f),
            )
        }
    }
}
