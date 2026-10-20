package com.tamizh.tvlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette

data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    /** 16:9 TV banner, if the app provides one. */
    val banner: ImageBitmap?,
    val icon: ImageBitmap?,
    /** True when the app declares a LEANBACK_LAUNCHER activity (a proper TV app). */
    val isTvApp: Boolean,
    val isGame: Boolean,
    /** Tile background colour picked from the app icon (ARGB), or null to use the accent colour. */
    val tileColor: Int?,
) {
    fun launchIntent(): Intent = Intent(Intent.ACTION_MAIN)
        .addCategory(if (isTvApp) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
        .setComponent(ComponentName(packageName, activityName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
}

/** Well-known streaming apps used for the "Streaming" row and default favourites. Edit freely. */
object KnownApps {
    val streaming = listOf(
        "com.netflix.ninja",                       // Netflix (TV)
        "com.google.android.youtube.tv",           // YouTube (TV)
        "com.amazon.amazonvideo.livingroom",       // Prime Video (TV)
        "in.startv.hotstar",                       // JioHotstar
        "com.jio.media.stb.ondemand",              // JioCinema (STB)
        "com.graymatrix.did",                      // ZEE5
        "com.sonyliv",                             // SonyLIV
        "com.sunnxt.app",                          // Sun NXT
        "com.disney.disneyplus",                   // Disney+
        "com.apple.atve.androidtv.appletv",        // Apple TV
        "com.google.android.youtube.tvmusic",      // YouTube Music
        "com.spotify.tv.android",                  // Spotify
        "com.plexapp.android",                     // Plex
        "org.xbmc.kodi",                           // Kodi
        "com.mxtech.videoplayer.television",       // MX Player (TV)
    )
}

class AppRepository(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    /** Lists launchable apps, preferring TV (leanback) entry points. Call off the main thread. */
    fun loadApps(): List<AppInfo> {
        val byPackage = LinkedHashMap<String, AppInfo>()
        for (ri in query(Intent.CATEGORY_LEANBACK_LAUNCHER)) addIfNew(byPackage, ri, isTv = true)
        for (ri in query(Intent.CATEGORY_LAUNCHER)) addIfNew(byPackage, ri, isTv = false)
        return byPackage.values.sortedBy { it.label.lowercase() }
    }

    private fun addIfNew(map: MutableMap<String, AppInfo>, ri: ResolveInfo, isTv: Boolean) {
        val ai = ri.activityInfo ?: return
        val pkg = ai.packageName
        if (pkg == context.packageName || map.containsKey(pkg)) return
        map[pkg] = toAppInfo(ri, isTv)
    }

    private fun query(category: String): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        return if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
    }

    private fun toAppInfo(ri: ResolveInfo, isTv: Boolean): AppInfo {
        val ai = ri.activityInfo
        val appInfo: ApplicationInfo? = ai.applicationInfo
        val label = ri.loadLabel(pm).toString().trim().ifEmpty { ai.packageName }

        val banner = if (isTv) {
            runCatching {
                (ai.loadBanner(pm) ?: appInfo?.loadBanner(pm))
                    ?.toBitmap(320, 180)
                    ?.asImageBitmap()
            }.getOrNull()
        } else null

        val iconBitmap = runCatching { ri.loadIcon(pm).toBitmap(144, 144) }.getOrNull()
        val icon = iconBitmap?.asImageBitmap()
        val tileColor = iconBitmap?.let { bmp ->
            runCatching {
                val p = Palette.from(bmp).generate()
                (p.darkVibrantSwatch ?: p.vibrantSwatch ?: p.darkMutedSwatch ?: p.dominantSwatch)?.rgb
            }.getOrNull()
        }

        @Suppress("DEPRECATION")
        val flaggedGame = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
        val categoryGame = appInfo != null && Build.VERSION.SDK_INT >= 26 &&
            appInfo.category == ApplicationInfo.CATEGORY_GAME

        return AppInfo(
            packageName = ai.packageName,
            activityName = ai.name,
            label = label,
            banner = banner,
            icon = icon,
            isTvApp = isTv,
            isGame = flaggedGame || categoryGame,
            tileColor = tileColor,
        )
    }
}
