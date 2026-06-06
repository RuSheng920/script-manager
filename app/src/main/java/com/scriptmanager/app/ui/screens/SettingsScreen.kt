package com.scriptmanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scriptmanager.app.ui.theme.persistThemeMode
import com.scriptmanager.app.ui.theme.themeModeFlow
import com.scriptmanager.app.util.PreferencesManager
import com.scriptmanager.app.util.RootShell
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    preferencesManager: PreferencesManager,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Collect settings
    val confirmBeforeRun by preferencesManager.confirmBeforeRun.collectAsState(initial = true)
    val keepScreenOn by preferencesManager.keepScreenOn.collectAsState(initial = true)
    val autoSaveOnRun by preferencesManager.autoSaveOnRun.collectAsState(initial = true)
    val defaultScriptDir by preferencesManager.defaultScriptDir.collectAsState(initial = "/sdcard/scripts")
    val maxOutputLines by preferencesManager.maxOutputLines.collectAsState(initial = 5000)
    val themeMode by themeModeFlow(context).collectAsState(initial = "follow_system")

    val isRooted = remember { RootShell.isDeviceRooted() }

    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showMaxLinesDialog by remember { mutableStateOf(false) }
    var tempMaxLines by remember { mutableStateOf(maxOutputLines.toString()) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Settings",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(MiuixIcons.Back, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // === Appearance ===
            SmallTitle(text = "Appearance")

            ArrowPreference(
                title = "Dark Mode",
                summary = when (themeMode) {
                    "always_dark" -> "Always dark"
                    "always_light" -> "Always light"
                    else -> "Follow system"
                },
                onClick = { showDarkModeDialog = true }
            )

            HorizontalDivider()

            // === Execution ===
            SmallTitle(text = "Execution")

            SwitchPreference(
                title = "Confirm before run",
                summary = "Show confirmation dialog before executing",
                checked = confirmBeforeRun,
                onCheckedChange = {
                    coroutineScope.launch { preferencesManager.setConfirmBeforeRun(it) }
                }
            )

            SwitchPreference(
                title = "Keep screen on",
                summary = "Keep screen awake during script execution",
                checked = keepScreenOn,
                onCheckedChange = {
                    coroutineScope.launch { preferencesManager.setKeepScreenOn(it) }
                }
            )

            SwitchPreference(
                title = "Auto-save on run",
                summary = "Record run info automatically",
                checked = autoSaveOnRun,
                onCheckedChange = {
                    coroutineScope.launch { preferencesManager.setAutoSaveOnRun(it) }
                }
            )

            HorizontalDivider()

            // === Advanced ===
            SmallTitle(text = "Advanced")

            ArrowPreference(
                title = "Default Script Directory",
                summary = defaultScriptDir,
                onClick = { /* Directory picker — stub */ }
            )

            ArrowPreference(
                title = "Max Output Lines",
                summary = "$maxOutputLines lines",
                onClick = {
                    tempMaxLines = maxOutputLines.toString()
                    showMaxLinesDialog = true
                }
            )

            HorizontalDivider()

            // === About ===
            SmallTitle(text = "About")

            Card(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                insideMargin = PaddingValues(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version", style = MiuixTheme.textStyles.body1)
                        Text("1.0.0", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Root Status", style = MiuixTheme.textStyles.body1)
                        Text(
                            text = if (isRooted) "✓ Available" else "✗ Not detected",
                            style = MiuixTheme.textStyles.body2,
                            color = if (isRooted) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dark mode selection dialog
    if (showDarkModeDialog) {
        OverlayDialog(
            title = "Dark Mode",
            show = true,
            onDismissRequest = { showDarkModeDialog = false }
        ) {
            Card {
                Column {
                    listOf(
                        Triple("follow_system", "Follow System", "Use system setting"),
                        Triple("always_light", "Always Light", "Light theme always"),
                        Triple("always_dark", "Always Dark", "Dark theme always"),
                    ).forEach { (key, title, summary) ->
                        ArrowPreference(
                            title = title,
                            summary = summary,
                            onClick = {
                                coroutineScope.launch {
                                    persistThemeMode(context, key)
                                }
                                showDarkModeDialog = false
                            },
                            holdDownState = false
                        )
                        if (key != "always_dark") {
                            HorizontalDivider()
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(
                text = "Cancel",
                onClick = { showDarkModeDialog = false },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Max lines dialog
    if (showMaxLinesDialog) {
        OverlayDialog(
            title = "Max Output Lines",
            show = true,
            onDismissRequest = { showMaxLinesDialog = false }
        ) {
            Card {
                TextField(
                    value = tempMaxLines,
                    onValueChange = { tempMaxLines = it.filter { c -> c.isDigit() } },
                    label = "Lines",
                    singleLine = true
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = "Cancel",
                    onClick = { showMaxLinesDialog = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    text = "OK",
                    onClick = {
                        val lines = tempMaxLines.toIntOrNull() ?: 5000
                        coroutineScope.launch {
                            preferencesManager.setMaxOutputLines(lines.coerceIn(100, 100000))
                        }
                        showMaxLinesDialog = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}
