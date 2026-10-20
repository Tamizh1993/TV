@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.tamizh.tvlauncher.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.tamizh.tvlauncher.LauncherViewModel
import com.tamizh.tvlauncher.Screen
import com.tamizh.tvlauncher.startSafely
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val QuickCities = listOf("Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Bengaluru", "Mumbai", "Delhi")

/** Left navigation entries -> index of the first card of that section in the list. */
private val NavSections = listOf(
    Triple("🎨", "Personalization", 1),
    Triple("▦", "Home & tiles", 3),
    Triple("🕒", "Time & weather", 6),
    Triple("ⓘ", "About", 9),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: LauncherViewModel) {
    val context = LocalContext.current
    val s = vm.settings
    val accent = s.accent()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val navFocus = remember { FocusRequester() }
    var selectedNav by remember { mutableIntStateOf(0) }
    var cityQuery by remember { mutableStateOf("") }
    var fieldFocused by remember { mutableStateOf(false) }

    BackHandler { vm.screen = Screen.Home }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.setWallpaperImage(uri)
        }
    }

    LaunchedEffect(Unit) {
        delay(80)
        runCatching { navFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        WallpaperBackground(s, vm.wallpaperImage)
        Box(Modifier.fillMaxSize().background(Color(0xE6191A1E)))

        Row(Modifier.fillMaxSize()) {
            // ---- Navigation pane ----
            Column(Modifier.width(230.dp).fillMaxHeight().padding(horizontal = 10.dp, vertical = 22.dp)) {
                Row(Modifier.padding(start = 10.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).background(Brush.linearGradient(listOf(accent, accent.light())), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("TV", fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Living room TV", fontSize = 13.sp)
                        Text("Tamizh Launcher", fontSize = 12.sp, modifier = Modifier.alpha(0.65f))
                    }
                }
                NavSections.forEachIndexed { i, (glyph, label, target) ->
                    val selected = selectedNav == i
                    WinButton(
                        onClick = {
                            selectedNav = i
                            scope.launch { listState.animateScrollToItem(target) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp)
                            .then(if (i == 0) Modifier.focusRequester(navFocus) else Modifier),
                        container = if (selected) Color.White.copy(alpha = 0.07f) else Color.Transparent,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .padding(vertical = 10.dp)
                                    .size(width = 3.dp, height = 16.dp)
                                    .background(if (selected) accent.light() else Color.Transparent, RoundedCornerShape(2.dp))
                            )
                            Text(glyph, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp, end = 10.dp))
                            Text(label, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                WinButton(onClick = { vm.screen = Screen.Home }, modifier = Modifier.fillMaxWidth(), container = Color.White.copy(alpha = 0.06f)) {
                    Text("←  Back to Home", fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
                }
            }

            // ---- Content pane ----
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 10.dp, end = 28.dp, top = 22.dp, bottom = 200.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                item { Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp)) }

                // 1 — Background
                item {
                    SettingCard("Background", "Shown behind your tiles") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            WallpaperPresets.forEachIndexed { i, preset ->
                                Swatch(selected = s.wallpaperUri == null && s.wallpaperPreset == i, onClick = { vm.setWallpaperPreset(i) }) {
                                    Box(Modifier.fillMaxSize().wallpaper(preset))
                                    Text(preset.name, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomStart).padding(5.dp))
                                }
                            }
                            SegButton("Choose image…", selected = s.wallpaperUri != null, accent = accent) {
                                try {
                                    imagePicker.launch(arrayOf("image/*"))
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(context, "This TV has no file picker installed", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
                // 2 — Accent
                item {
                    SettingCard("Accent colour", "Used for system tiles, highlights and the taskbar") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Accents.forEachIndexed { i, c ->
                                val sel = s.accentIndex == i
                                WinButton(onClick = { vm.setAccent(i) }, shape = CircleShape) {
                                    Box(
                                        Modifier
                                            .padding(3.dp)
                                            .size(30.dp)
                                            .background(c, CircleShape)
                                            .border(if (sel) 2.dp else 0.dp, if (sel) Color.White else Color.Transparent, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) { if (sel) Text("✓", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
                // 3 — Live tiles
                item {
                    SettingRow("Live tiles", "Tiles flip to show the calendar, forecast and when you last opened an app") {
                        WinToggle(on = s.liveTiles, accent = accent) { vm.setLiveTiles(!s.liveTiles) }
                    }
                }
                // 4 — Transparent tiles
                item {
                    SettingRow("Transparent tiles", "Let the background show through the tiles") {
                        WinToggle(on = s.transparentTiles, accent = accent) { vm.setTransparentTiles(!s.transparentTiles) }
                    }
                }
                // 5 — Unpinned tiles
                item {
                    val count = vm.hiddenTiles.size
                    SettingRow(
                        "Unpinned tiles",
                        if (count == 0) "Every tile is on Home" else "$count tile${if (count == 1) "" else "s"} hidden from Home",
                    ) {
                        SegButton("Restore all", selected = false, accent = accent) {
                            vm.restoreTiles()
                            Toast.makeText(context, "All tiles restored", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                // 6 — Clock
                item {
                    SettingRow("Clock", "Format used on the clock tile and taskbar") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SegButton("12-hour", selected = !s.use24h, accent = accent) { vm.setUse24h(false) }
                            SegButton("24-hour", selected = s.use24h, accent = accent) { vm.setUse24h(true) }
                        }
                    }
                }
                // 7 — Temperature
                item {
                    SettingRow("Temperature", "Unit for the weather tile") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SegButton("Celsius", selected = !s.fahrenheit, accent = accent) { vm.setFahrenheit(false) }
                            SegButton("Fahrenheit", selected = s.fahrenheit, accent = accent) { vm.setFahrenheit(true) }
                        }
                    }
                }
                // 8 — Weather location
                item {
                    SettingCard("Weather location", "Currently ${s.cityName} · data from Open-Meteo") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickCities.forEach { city ->
                                SegButton(city, selected = city.equals(s.cityName, ignoreCase = true), accent = accent) { vm.setCity(city) }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicTextField(
                                value = cityQuery,
                                onValueChange = { cityQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                cursorBrush = SolidColor(Color.White),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { vm.setCity(cityQuery) }),
                                modifier = Modifier
                                    .width(280.dp)
                                    .onFocusChanged { fieldFocused = it.isFocused }
                                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(5.dp))
                                    .border(
                                        BorderStroke(if (fieldFocused) 2.dp else 1.dp, if (fieldFocused) Color.White else Color.White.copy(alpha = 0.1f)),
                                        RoundedCornerShape(5.dp),
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                decorationBox = { inner ->
                                    Box {
                                        if (cityQuery.isEmpty()) Text("Type any city…", fontSize = 14.sp, color = Color.White.copy(alpha = 0.5f))
                                        inner()
                                    }
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            SegButton("Search", selected = false, accent = accent) { vm.setCity(cityQuery) }
                        }
                        vm.citySearchStatus?.let {
                            Text(it, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp).alpha(0.75f))
                        }
                    }
                }
                // 9 — Default home
                item {
                    SettingRow("Make this my home screen", "Press HOME and choose \"Tamizh TV Launcher\" → Always") {
                        SegButton("Open Home-app settings", selected = false, accent = accent) {
                            context.startSafely(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                }
                // 10 — About
                item {
                    SettingCard("Tamizh TV Launcher", "Version 2.0 · Live tiles · Weather by Open-Meteo") {}
                }
            }
        }
    }
}

// ---------------- Settings building blocks ----------------

@Composable
private fun SettingCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(7.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(7.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(title, fontSize = 14.sp)
        Text(subtitle, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp).alpha(0.65f))
        content()
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(7.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(7.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp).alpha(0.65f))
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun Swatch(selected: Boolean, onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier.size(width = 84.dp, height = 50.dp),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
        border = ClickableSurfaceDefaults.border(
            border = if (selected) Border(BorderStroke(2.dp, Color(0xFF9CC3F5)), shape = shape) else Border.None,
            focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = shape),
        ),
    ) {
        content()
        if (selected) Text("✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
    }
}
