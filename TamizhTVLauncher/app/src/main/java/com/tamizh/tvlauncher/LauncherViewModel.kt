package com.tamizh.tvlauncher

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tamizh.tvlauncher.data.AppInfo
import com.tamizh.tvlauncher.data.AppRepository
import com.tamizh.tvlauncher.data.KnownApps
import com.tamizh.tvlauncher.data.Prefs
import com.tamizh.tvlauncher.data.Weather
import com.tamizh.tvlauncher.data.WeatherApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

enum class Screen { Home, Settings }

data class LauncherSettings(
    val wallpaperPreset: Int,
    val wallpaperUri: String?,
    val accentIndex: Int,
    val liveTiles: Boolean,
    val transparentTiles: Boolean,
    val cityName: String,
    val use24h: Boolean,
    val fahrenheit: Boolean,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = Prefs(application)
    private val repo = AppRepository(application)

    var screen by mutableStateOf(Screen.Home)

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set

    /** Apps pinned to the "Pinned" group, in order. */
    var favourites by mutableStateOf(prefs.favourites)
        private set
    /** package -> last launch time */
    var recents by mutableStateOf(prefs.recents)
        private set
    var tileSizes by mutableStateOf(prefs.tileSizes)
        private set
    var hiddenTiles by mutableStateOf(prefs.hiddenTiles)
        private set
    var liveOffTiles by mutableStateOf(prefs.liveOffTiles)
        private set

    var weather by mutableStateOf<Weather?>(null)
        private set
    var weatherError by mutableStateOf(false)
        private set
    var citySearchStatus by mutableStateOf<String?>(null)
        private set

    var settings by mutableStateOf(readSettings())
        private set
    var wallpaperImage by mutableStateOf<ImageBitmap?>(null)
        private set

    /** Incremented when HOME is pressed while the launcher is already showing. */
    var homeResetSignal by mutableIntStateOf(0)
        private set

    private var appsJob: Job? = null

    init {
        loadWallpaper()
        viewModelScope.launch {
            while (isActive) {
                fetchWeather()
                delay(WEATHER_REFRESH_MS)
            }
        }
    }

    private fun readSettings() = LauncherSettings(
        wallpaperPreset = prefs.wallpaperPreset,
        wallpaperUri = prefs.wallpaperUri,
        accentIndex = prefs.accentIndex,
        liveTiles = prefs.liveTiles,
        transparentTiles = prefs.transparentTiles,
        cityName = prefs.cityName,
        use24h = prefs.use24h,
        fahrenheit = prefs.fahrenheit,
    )

    // ---------- Apps ----------

    fun refreshApps() {
        if (appsJob?.isActive == true) return
        appsJob = viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { repo.loadApps() }
            apps = list
            val installed = list.map { it.packageName }.toSet()

            if (!prefs.favouritesInitialised && list.isNotEmpty()) {
                // First run: pin up to four installed streaming apps (or any TV apps).
                val defaults = KnownApps.streaming.filter { it in installed }.take(4).toMutableList()
                for (app in list) {
                    if (defaults.size >= 4) break
                    if (app.isTvApp && app.packageName !in defaults) defaults += app.packageName
                }
                setFavourites(defaults)
                prefs.favouritesInitialised = true
            } else if (list.isNotEmpty()) {
                val cleaned = favourites.filter { it in installed }
                if (cleaned.size != favourites.size) setFavourites(cleaned)
            }
            loaded = true
        }
    }

    fun launch(context: Context, app: AppInfo) {
        val fallback = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (context.startSafely(app.launchIntent(), fallback)) {
            recents = recents + (app.packageName to System.currentTimeMillis())
            prefs.recents = recents
        }
    }

    fun isPinned(pkg: String) = pkg in favourites && appTileId(pkg) !in hiddenTiles

    /** Adds an app to the Pinned group (and un-hides it). */
    fun pinApp(pkg: String) {
        if (pkg !in favourites) setFavourites(favourites + pkg)
        setHidden(hiddenTiles - appTileId(pkg))
    }

    /** Removes a tile from Home. Apps are also removed from the Pinned group. */
    fun unpin(tileId: String) {
        if (tileId.startsWith("app:")) {
            val pkg = tileId.removePrefix("app:")
            if (pkg in favourites) setFavourites(favourites - pkg)
        }
        setHidden(hiddenTiles + tileId)
    }

    fun restoreTiles() = setHidden(emptySet())

    fun moveFavourite(pkg: String, delta: Int) {
        val list = favourites.toMutableList()
        val from = list.indexOf(pkg)
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        Collections.swap(list, from, to)
        setFavourites(list)
    }

    fun setTileSize(tileId: String, size: TileSize) {
        tileSizes = tileSizes + (tileId to size.name)
        prefs.tileSizes = tileSizes
    }

    fun toggleLive(tileId: String) {
        liveOffTiles = if (tileId in liveOffTiles) liveOffTiles - tileId else liveOffTiles + tileId
        prefs.liveOffTiles = liveOffTiles
    }

    private fun setFavourites(list: List<String>) {
        favourites = list
        prefs.favourites = list
    }

    private fun setHidden(set: Set<String>) {
        hiddenTiles = set
        prefs.hiddenTiles = set
    }

    // ---------- Navigation ----------

    fun goHome() {
        screen = Screen.Home
        homeResetSignal++
    }

    // ---------- Look & feel ----------

    fun setAccent(index: Int) { prefs.accentIndex = index; settings = readSettings() }
    fun setLiveTiles(on: Boolean) { prefs.liveTiles = on; settings = readSettings() }
    fun setTransparentTiles(on: Boolean) { prefs.transparentTiles = on; settings = readSettings() }
    fun setFahrenheit(value: Boolean) { prefs.fahrenheit = value; settings = readSettings() }
    fun setUse24h(value: Boolean) { prefs.use24h = value; settings = readSettings() }

    // ---------- Weather ----------

    private suspend fun fetchWeather() {
        try {
            weather = WeatherApi.current(prefs.cityLat, prefs.cityLon)
            weatherError = false
        } catch (e: Exception) {
            weatherError = true
        }
    }

    fun setCity(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            citySearchStatus = "Searching…"
            val place = try {
                WeatherApi.findPlace(query)
            } catch (e: Exception) {
                citySearchStatus = "Couldn't reach the weather service. Check the internet connection."
                return@launch
            }
            if (place == null) {
                citySearchStatus = "No place found for \"$query\""
                return@launch
            }
            prefs.cityName = place.name
            prefs.cityLat = place.latitude
            prefs.cityLon = place.longitude
            settings = readSettings()
            citySearchStatus = "Weather location set to ${place.name}"
            weather = null
            fetchWeather()
        }
    }

    // ---------- Wallpaper ----------

    fun setWallpaperPreset(index: Int) {
        prefs.wallpaperPreset = index
        prefs.wallpaperUri = null
        settings = readSettings()
        wallpaperImage = null
    }

    fun setWallpaperImage(uri: Uri) {
        prefs.wallpaperUri = uri.toString()
        settings = readSettings()
        loadWallpaper()
    }

    private fun loadWallpaper() {
        val uri = prefs.wallpaperUri
        if (uri == null) {
            wallpaperImage = null
            return
        }
        viewModelScope.launch {
            val resolver = getApplication<Application>().contentResolver
            val bitmap = withContext(Dispatchers.IO) {
                runCatching { decodeSampled(resolver, Uri.parse(uri)) }.getOrNull()
            }
            wallpaperImage = bitmap?.asImageBitmap()
        }
    }

    /** Decodes an image scaled down to roughly screen size to keep memory low. */
    private fun decodeSampled(resolver: ContentResolver, uri: Uri, targetWidth: Int = 1920): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetWidth || bounds.outHeight / (sample * 2) >= 1080) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    companion object {
        private const val WEATHER_REFRESH_MS = 30 * 60 * 1000L
    }
}
