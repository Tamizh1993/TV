package com.tamizh.tvlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.LocalContentColor
import com.tamizh.tvlauncher.ui.HomeScreen
import com.tamizh.tvlauncher.ui.LauncherTheme
import com.tamizh.tvlauncher.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauncherTheme {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    when (vm.screen) {
                        Screen.Home -> HomeScreen(vm)
                        Screen.Settings -> SettingsScreen(vm)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Picks up apps installed/uninstalled while we were in the background.
        vm.refreshApps()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // HOME pressed while the launcher is already in front: jump back to the top.
        vm.goHome()
    }
}
