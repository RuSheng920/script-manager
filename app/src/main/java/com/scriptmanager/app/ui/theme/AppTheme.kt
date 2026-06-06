package com.scriptmanager.app.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

private val Context.appThemeDataStore by preferencesDataStore(name = "app_theme")

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

/** Map persisted key to ColorSchemeMode */
private fun modeFromKey(key: String): ColorSchemeMode = when (key) {
    "always_dark" -> ColorSchemeMode.Dark
    "always_light" -> ColorSchemeMode.Light
    else -> ColorSchemeMode.System
}

@Composable
fun AppTheme(
    context: Context,
    content: @Composable () -> Unit
) {
    val themeMode by context.appThemeDataStore.data
        .map { it[THEME_MODE_KEY] ?: "follow_system" }
        .collectAsState(initial = "follow_system")

    val controller = remember(themeMode) {
        ThemeController(colorSchemeMode = modeFromKey(themeMode))
    }

    MiuixTheme(controller = controller, content = content)
}

/** Persist theme mode */
suspend fun persistThemeMode(context: Context, mode: String) {
    context.appThemeDataStore.edit { it[THEME_MODE_KEY] = mode }
}

/** Observe theme mode */
fun themeModeFlow(context: Context) =
    context.appThemeDataStore.data.map { it[THEME_MODE_KEY] ?: "follow_system" }
