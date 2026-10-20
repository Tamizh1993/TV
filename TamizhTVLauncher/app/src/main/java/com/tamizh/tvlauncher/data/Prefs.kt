package com.tamizh.tvlauncher.data

import android.content.Context

/** Simple SharedPreferences wrapper for everything the launcher remembers. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    private fun getList(key: String): List<String> =
        sp.getString(key, null)?.split(',')?.filter { it.isNotBlank() } ?: emptyList()

    private fun putList(key: String, value: Collection<String>) =
        sp.edit().putString(key, value.joinToString(",")).apply()

    /** Apps the user pinned to the "Pinned" group, in display order. */
    var favourites: List<String>
        get() = getList("favourites")
        set(value) = putList("favourites", value)

    var favouritesInitialised: Boolean
        get() = sp.getBoolean("favourites_initialised", false)
        set(value) = sp.edit().putBoolean("favourites_initialised", value).apply()

    /** Recently launched apps: package -> last launch time (ms). */
    var recents: Map<String, Long>
        get() = getList("recents_v2").mapNotNull { entry ->
            val i = entry.lastIndexOf('@')
            if (i <= 0) null else entry.substring(i + 1).toLongOrNull()?.let { entry.substring(0, i) to it }
        }.toMap()
        set(value) = putList("recents_v2", value.entries.sortedByDescending { it.value }.take(20).map { "${it.key}@${it.value}" })

    /** Tile id -> size name (S/M/W/L) for tiles the user resized. */
    var tileSizes: Map<String, String>
        get() = sp.getString("tile_sizes", null)?.split(';')?.mapNotNull {
            val parts = it.split('=')
            if (parts.size == 2) parts[0] to parts[1] else null
        }?.toMap() ?: emptyMap()
        set(value) = sp.edit().putString("tile_sizes", value.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()

    /** Tiles the user unpinned from Home. */
    var hiddenTiles: Set<String>
        get() = getList("hidden_tiles").toSet()
        set(value) = putList("hidden_tiles", value)

    /** Tiles whose live (flipping) content the user turned off. */
    var liveOffTiles: Set<String>
        get() = getList("live_off_tiles").toSet()
        set(value) = putList("live_off_tiles", value)

    var liveTiles: Boolean
        get() = sp.getBoolean("live_tiles", true)
        set(value) = sp.edit().putBoolean("live_tiles", value).apply()

    var transparentTiles: Boolean
        get() = sp.getBoolean("transparent_tiles", false)
        set(value) = sp.edit().putBoolean("transparent_tiles", value).apply()

    var accentIndex: Int
        get() = sp.getInt("accent_index", 0)
        set(value) = sp.edit().putInt("accent_index", value).apply()

    var wallpaperPreset: Int
        get() = sp.getInt("wallpaper_preset", 0)
        set(value) = sp.edit().putInt("wallpaper_preset", value).apply()

    /** content:// URI of a user-picked image, or null for a gradient preset. */
    var wallpaperUri: String?
        get() = sp.getString("wallpaper_uri", null)
        set(value) = sp.edit().putString("wallpaper_uri", value).apply()

    var cityName: String
        get() = sp.getString("city_name", null) ?: "Chennai"
        set(value) = sp.edit().putString("city_name", value).apply()

    var cityLat: Double
        get() = sp.getString("city_lat", null)?.toDoubleOrNull() ?: 13.0878
        set(value) = sp.edit().putString("city_lat", value.toString()).apply()

    var cityLon: Double
        get() = sp.getString("city_lon", null)?.toDoubleOrNull() ?: 80.2785
        set(value) = sp.edit().putString("city_lon", value.toString()).apply()

    var use24h: Boolean
        get() = sp.getBoolean("use_24h", false)
        set(value) = sp.edit().putBoolean("use_24h", value).apply()

    var fahrenheit: Boolean
        get() = sp.getBoolean("fahrenheit", false)
        set(value) = sp.edit().putBoolean("fahrenheit", value).apply()
}
