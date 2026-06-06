package com.scriptmanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scriptmanager.app.model.ScriptRepository
import com.scriptmanager.app.ui.components.ScriptCard
import com.scriptmanager.app.util.RootShell
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ScriptListScreen(
    repository: ScriptRepository,
    onCreateScript: () -> Unit,
    onEditScript: (String) -> Unit,
    onRunScript: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val scripts by repository.scripts.collectAsState()
    val isRooted = remember { RootShell.isDeviceRooted() }
    val coroutineScope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importPath by remember { mutableStateOf("/sdcard/scripts") }
    var showMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "Script Manager"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateScript
            ) {
                Icon(MiuixIcons.Add, contentDescription = "New Script")
            }
        }
    ) { paddingValues ->
        if (scripts.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = MiuixIcons.VerticalSplit,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No scripts yet",
                        style = MiuixTheme.textStyles.title4,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the button below to create a new script,\nor import from the menu.",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top action row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Root status badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isRooted)
                                MiuixTheme.colorScheme.primaryContainer
                            else
                                MiuixTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (isRooted) "✓ ROOT" else "✗ No ROOT",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MiuixTheme.textStyles.footnote2,
                                color = if (isRooted)
                                    MiuixTheme.colorScheme.onPrimaryContainer
                                else
                                    MiuixTheme.colorScheme.onErrorContainer
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Import button
                        Button(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.buttonColors(),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                MiuixIcons.AddFolder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Import",
                                style = MiuixTheme.textStyles.footnote2
                            )
                        }

                        // Settings button
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                MiuixIcons.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                items(scripts, key = { it.id }) { script ->
                    ScriptCard(
                        script = script,
                        onRun = { onRunScript(script.id) },
                        onEdit = { onEditScript(script.id) },
                        onDelete = { showDeleteDialog = script.id },
                        onToggleFavorite = {
                            coroutineScope.launch {
                                repository.toggleFavorite(script.id)
                            }
                        }
                    )
                }

                // Bottom spacer for FAB
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { scriptId ->
        val script = scripts.find { it.id == scriptId }
        OverlayDialog(
            title = "Confirm Delete",
            summary = "Delete script \"${script?.displayName ?: ""}\"? This cannot be undone.",
            show = true,
            onDismissRequest = { showDeleteDialog = null }
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = "Cancel",
                    onClick = { showDeleteDialog = null },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    text = "Delete",
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteScript(scriptId)
                        }
                        showDeleteDialog = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }

    // Import dialog
    if (showImportDialog) {
        OverlayDialog(
            title = "Import from Directory",
            summary = "Enter the directory path containing .sh scripts:",
            show = true,
            onDismissRequest = { showImportDialog = false }
        ) {
            Card {
                TextField(
                    value = importPath,
                    onValueChange = { importPath = it },
                    label = "Directory Path",
                    singleLine = true
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    text = "Cancel",
                    onClick = { showImportDialog = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    text = "Import",
                    onClick = {
                        coroutineScope.launch {
                            try {
                                repository.scanDirectory(java.io.File(importPath))
                            } catch (_: Exception) {}
                        }
                        showImportDialog = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}
