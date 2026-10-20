# Tamizh TV Launcher

A clean, ad-free home screen for Android TV in a **live tiles** style (Windows 10/11 inspired), built with Kotlin + Jetpack Compose for TV.
Open `docs/preview-live-tiles.html` in a browser to see and click through the design.

## Features

| Area | What it does |
|---|---|
| Header | "Good morning/afternoon/evening" greeting and date |
| Live tiles | Small, Medium, Wide and Large tiles packed into groups that scroll sideways. Live tiles flip every few seconds: Clock ↔ month calendar, Weather ↔ 4-day forecast, apps ↔ "last opened 2 hours ago" |
| Groups | Today (clock, weather, launcher settings, 2 most recent apps) · Pinned · Watch (streaming apps) · Play (games) · System (Wi-Fi, Bluetooth, Display, Sound, Apps, Date & time, All settings) |
| Tile colours | Each app tile takes its colour from the app icon; system tiles use your accent colour. Wide/Large app tiles show the app's TV banner |
| Tile menu | Hold **OK** on a tile: Open, Resize (cycles sizes), live tile on/off, move earlier/later, pin, unpin, App info, Uninstall |
| Taskbar | Weather · Home · Search · All apps · Settings · 3 recently used apps · clock |
| All apps / Search | Start-menu-style panel with search; hold OK on an app to pin or unpin it |
| Settings (Windows 11 style) | Background (5 gradients or your own photo), 8 accent colours, live tiles on/off, transparent tiles, restore unpinned tiles, 12/24 h clock, °C/°F, weather city |

Fully D-pad driven. Back does nothing on Home; pressing HOME again jumps to the first tile.

## Build

**Option A – Android Studio (recommended)**
1. Install Android Studio (Koala or newer).
2. *File → Open* → select this folder. Let Gradle sync (downloads SDK 34 automatically).
3. Plug in / connect your TV over ADB (see below) and press **Run**, or *Build → Build APK(s)*.

**Option B – GitHub (no Android Studio needed)**
1. Push this folder to a GitHub repo.
2. The included workflow `.github/workflows/build-apk.yml` builds the APK automatically.
3. Download it from the run's **Artifacts** section.

**Option C – command line** (needs JDK 17 + Android SDK): `./gradlew assembleRelease`
→ `app/build/outputs/apk/release/app-release.apk`

## Install on the TV

1. TV: *Settings → Device preferences → About* → click **Build** 7 times → enable **USB/Network debugging** in Developer options.
2. PC: `adb connect <TV-IP>:5555` then `adb install -r app-release.apk`
   (or copy the APK by USB drive / "Send Files to TV" and open it with a file manager).
3. Press **HOME** → choose **Tamizh TV Launcher** → **Always**.

### Important: Google TV / Fire TV
Stock Android TV boxes ask you to pick a home app. **Google TV** devices (Chromecast with Google TV, most 2021+ Sony/TCL/Xiaomi TVs) and **Fire TV** hard-wire the HOME button to their own launcher. The app still runs there (it shows in your app list), but HOME won't open it unless you use a remapping tool such as *Button Mapper*, or disable the stock launcher over ADB — the latter is at your own risk and can be reversed with `pm enable`.

## Customise
- Groups, default tile sizes, system shortcuts: `Tiles.kt` → `buildTileGroups`
- Streaming apps list / first-run pins: `data/AppRepository.kt` → `KnownApps.streaming`
- Backgrounds and accent colours: `ui/Components.kt` → `WallpaperPresets`, `Accents`
- Tile unit size (56dp) and flip speed: `ui/LiveTile.kt` → `TileUnit`, `ui/HomeScreen.kt` (`delay(2600)`)
- Default weather city: `data/Prefs.kt` (Chennai)

## Known limitations
- Live app tiles show when *you* last opened the app. Showing what's playing inside Netflix/Prime etc. isn't possible: Android only lets system-signed launchers read other apps' Watch Next data.
- "Transparent tiles" uses see-through colour; true frosted-glass blur of the wallpaper isn't available to Compose on Android 6–11.
- The image wallpaper picker needs a document picker on the TV; many TVs don't ship one (you'll see a message). Gradient wallpapers always work.
- Release build is signed with the debug key for personal sideloading. Create your own keystore before distributing.

## Project structure
```
app/src/main/java/com/tamizh/tvlauncher/
├── MainActivity.kt          Home activity, screen switching
├── LauncherViewModel.kt     State: apps, pins, tile sizes, recents, weather, look & feel
├── Tiles.kt                 Tile model, group building, grid packing
├── Intents.kt               Crash-safe activity launching
├── data/
│   ├── AppRepository.kt     Installed apps (banners, icons, icon colour, games)
│   ├── Prefs.kt             SharedPreferences storage
│   └── WeatherApi.kt        Open-Meteo geocoding, current weather + forecast
└── ui/
    ├── Components.kt        Theme, accents, wallpapers, Win-style buttons & toggle
    ├── LiveTile.kt          Tile rendering + live faces (clock, calendar, weather, last opened)
    ├── HomeScreen.kt        Header, tile groups, taskbar, tile menu wiring
    ├── Overlays.kt          Tile context menu, All apps / search panel
    └── SettingsScreen.kt    Windows 11-style settings
docs/preview-live-tiles.html Interactive browser preview of the design
```
