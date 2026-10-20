package com.tamizh.tvlauncher

import android.provider.Settings
import com.tamizh.tvlauncher.data.AppInfo
import com.tamizh.tvlauncher.data.KnownApps

/** Tile sizes in grid units (1 unit = 56dp, 8dp gap), like Windows Start tiles. */
enum class TileSize(val w: Int, val h: Int, val label: String) {
    S(1, 1, "Small"), M(2, 2, "Medium"), W(4, 2, "Wide"), L(4, 4, "Large");

    companion object {
        fun parse(name: String?): TileSize? = entries.firstOrNull { it.name == name }
    }
}

sealed interface TileKind {
    data object Clock : TileKind
    data object Weather : TileKind
    data object LauncherSettings : TileKind
    data class SystemShortcut(val label: String, val glyph: String, val action: String) : TileKind
    data class App(val app: AppInfo) : TileKind
}

data class TileSpec(
    val id: String,
    val kind: TileKind,
    val size: TileSize,
    val allowedSizes: List<TileSize>,
) {
    val name: String
        get() = when (kind) {
            TileKind.Clock -> "Clock"
            TileKind.Weather -> "Weather"
            TileKind.LauncherSettings -> "Launcher settings"
            is TileKind.SystemShortcut -> kind.label
            is TileKind.App -> kind.app.label
        }
}

data class TileGroup(val title: String, val tiles: List<TileSpec>)

data class PlacedTile(val tile: TileSpec, val x: Int, val y: Int)

data class PackedGroup(val title: String, val tiles: List<PlacedTile>, val columns: Int)

const val GRID_ROWS = 6

fun appTileId(pkg: String) = "app:$pkg"

private val SystemShortcuts = listOf(
    TileKind.SystemShortcut("Wi‑Fi & network", "📶", Settings.ACTION_WIFI_SETTINGS),
    TileKind.SystemShortcut("Bluetooth & remotes", "🎧", Settings.ACTION_BLUETOOTH_SETTINGS),
    TileKind.SystemShortcut("Display", "🖥️", Settings.ACTION_DISPLAY_SETTINGS),
    TileKind.SystemShortcut("Sound", "🔊", Settings.ACTION_SOUND_SETTINGS),
    TileKind.SystemShortcut("Apps", "📦", Settings.ACTION_APPLICATION_SETTINGS),
    TileKind.SystemShortcut("Date & time", "🕒", Settings.ACTION_DATE_SETTINGS),
    TileKind.SystemShortcut("All settings", "⚙️", Settings.ACTION_SETTINGS),
)

private val AppSizes = listOf(TileSize.S, TileSize.M, TileSize.W, TileSize.L)

/**
 * Builds the Home tile groups from the launcher state. Everything the user changed
 * (sizes, unpinned tiles) is applied here.
 */
fun buildTileGroups(
    apps: List<AppInfo>,
    favourites: List<String>,
    recents: Map<String, Long>,
    sizes: Map<String, String>,
    hidden: Set<String>,
): List<TileGroup> {
    val byPkg = apps.associateBy { it.packageName }
    val used = HashSet<String>()

    fun spec(id: String, kind: TileKind, default: TileSize, allowed: List<TileSize>): TileSpec {
        val chosen = TileSize.parse(sizes[id])?.takeIf { it in allowed } ?: default
        return TileSpec(id, kind, chosen, allowed)
    }
    fun appSpec(app: AppInfo, default: TileSize): TileSpec? {
        val id = appTileId(app.packageName)
        if (id in hidden || !used.add(id)) return null
        return spec(id, TileKind.App(app), default, AppSizes)
    }

    val groups = ArrayList<TileGroup>()

    // 1. Pinned apps (user order)
    val pinned = favourites.mapNotNull { byPkg[it] }
        .mapIndexedNotNull { i, app -> appSpec(app, if (i == 0) TileSize.W else TileSize.M) }

    // 2. Today: clock, weather, launcher settings + two most recent apps
    val today = buildList {
        if ("sys:clock" !in hidden) add(spec("sys:clock", TileKind.Clock, TileSize.L, listOf(TileSize.L, TileSize.W, TileSize.M)))
        if ("sys:weather" !in hidden) add(spec("sys:weather", TileKind.Weather, TileSize.W, listOf(TileSize.W, TileSize.M, TileSize.L)))
        if ("sys:launcher" !in hidden) add(spec("sys:launcher", TileKind.LauncherSettings, TileSize.M, listOf(TileSize.S, TileSize.M)))
        val pinnedIds = favourites.map { appTileId(it) }.toSet()
        recents.entries.sortedByDescending { it.value }
            .mapNotNull { byPkg[it.key] }
            .filter { appTileId(it.packageName) !in pinnedIds }
            .take(2)
            .forEach { app -> appSpec(app, TileSize.M)?.let { add(it) } }
    }
    if (today.isNotEmpty()) groups += TileGroup("Today", today)
    if (pinned.isNotEmpty()) groups += TileGroup("Pinned", pinned)

    // 3. Streaming apps not already shown
    val streaming = KnownApps.streaming.mapNotNull { byPkg[it] }
        .mapIndexedNotNull { i, app -> appSpec(app, if (i == 0) TileSize.W else TileSize.M) }
    if (streaming.isNotEmpty()) groups += TileGroup("Watch", streaming)

    // 4. Games
    val games = apps.filter { it.isGame }.mapIndexedNotNull { i, app -> appSpec(app, if (i == 0) TileSize.W else TileSize.M) }
    if (games.isNotEmpty()) groups += TileGroup("Play", games)

    // 5. System shortcuts
    val system = SystemShortcuts.mapIndexedNotNull { i, s ->
        val id = "set:${s.label}"
        if (id in hidden) null else spec(id, s, if (i < 5) TileSize.M else TileSize.S, listOf(TileSize.S, TileSize.M))
    }
    if (system.isNotEmpty()) groups += TileGroup("System", system)

    return groups
}

/** True if this tile has a second, "live" face that flips into view. */
fun TileSpec.isLiveCapable(recents: Map<String, Long>): Boolean = when (kind) {
    TileKind.Clock, TileKind.Weather -> true
    is TileKind.App -> size != TileSize.S && recents.containsKey(kind.app.packageName)
    else -> false
}

/**
 * Packs tiles into a grid that is [GRID_ROWS] units tall and grows sideways,
 * filling holes first (like CSS grid "dense"). Medium and larger tiles snap to even cells.
 */
fun packGroup(group: TileGroup): PackedGroup {
    val columns = ArrayList<BooleanArray>()
    fun ensure(cols: Int) { while (columns.size < cols) columns += BooleanArray(GRID_ROWS) }
    fun isFree(x: Int, y: Int, w: Int, h: Int): Boolean {
        for (i in x until x + w) for (j in y until y + h) {
            if (i < columns.size && columns[i][j]) return false
        }
        return true
    }

    val placed = ArrayList<PlacedTile>()
    for (tile in group.tiles) {
        val w = tile.size.w
        val h = tile.size.h
        val step = if (w == 1 && h == 1) 1 else 2
        var x = 0
        var done = false
        while (!done) {
            var y = 0
            while (y + h <= GRID_ROWS) {
                if (isFree(x, y, w, h)) {
                    ensure(x + w)
                    for (i in x until x + w) for (j in y until y + h) columns[i][j] = true
                    placed += PlacedTile(tile, x, y)
                    done = true
                    break
                }
                y += step
            }
            x += step
        }
    }
    return PackedGroup(group.title, placed, columns.size)
}
