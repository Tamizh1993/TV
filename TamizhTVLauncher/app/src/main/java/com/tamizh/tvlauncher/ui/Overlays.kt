@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.tamizh.tvlauncher.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.tamizh.tvlauncher.data.AppInfo
import kotlinx.coroutines.delay

// ---------------- Tile context menu ----------------

/** A context-menu row. A null entry in the list draws a separator. */
data class MenuEntry(val glyph: String, val label: String, val trailing: String? = null, val action: () -> Unit)

@Composable
fun TileMenu(title: String, entries: List<MenuEntry?>, accent: Color, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .width(270.dp)
                .background(GlassDark, RoundedCornerShape(8.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                .padding(5.dp),
        ) {
            Text(
                title,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            var firstAssigned = false
            entries.forEach { entry ->
                if (entry == null) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp).height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                } else {
                    val mod = if (!firstAssigned) Modifier.focusRequester(first) else Modifier
                    firstAssigned = true
                    WinButton(
                        onClick = entry.action,
                        modifier = mod.fillMaxWidth(),
                        shape = RoundedCornerShape(5.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(entry.glyph, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.width(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(entry.label, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (entry.trailing != null) {
                                Text(entry.trailing, fontSize = 12.sp, color = accent.light())
                            }
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { first.requestFocus() }
    }
}

// ---------------- All apps / search panel ----------------

@Composable
fun AllAppsPanel(
    apps: List<AppInfo>,
    isPinned: (String) -> Boolean,
    focusSearch: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onTogglePin: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var fieldFocused by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    val firstApp = remember { FocusRequester() }
    val shown = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier
                    .padding(bottom = 62.dp)
                    .size(width = 620.dp, height = 430.dp)
                    .background(Color(0xEB24242A), RoundedCornerShape(10.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 26.dp, vertical = 20.dp),
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocus)
                        .onFocusChanged { fieldFocused = it.isFocused }
                        .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
                        .border(
                            BorderStroke(if (fieldFocused) 2.dp else 1.dp, if (fieldFocused) Color.White else Color.White.copy(alpha = 0.1f)),
                            RoundedCornerShape(18.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) Text("🔍  Search apps", fontSize = 14.sp, color = Color.White.copy(alpha = 0.6f))
                            inner()
                        }
                    },
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (query.isBlank()) "All apps" else "Results", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("Hold OK to pin or unpin", fontSize = 12.sp, modifier = Modifier.alpha(0.6f))
                }
                Spacer(Modifier.height(10.dp))
                if (shown.isEmpty()) {
                    Text("No apps match \"$query\"", fontSize = 13.sp, modifier = Modifier.alpha(0.7f))
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(shown, key = { it.packageName }) { app ->
                        val isFirst = app.packageName == shown.firstOrNull()?.packageName
                        WinButton(
                            onClick = { onLaunch(app) },
                            onLongClick = { onTogglePin(app) },
                            modifier = if (isFirst) Modifier.focusRequester(firstApp) else Modifier,
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    if (app.icon != null) Image(app.icon, contentDescription = null, modifier = Modifier.size(36.dp))
                                    else Text(app.label.take(1), fontSize = 20.sp)
                                    if (isPinned(app.packageName)) Text("📌", fontSize = 10.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    app.label,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(120)
        runCatching { if (focusSearch) searchFocus.requestFocus() else firstApp.requestFocus() }
    }
}
