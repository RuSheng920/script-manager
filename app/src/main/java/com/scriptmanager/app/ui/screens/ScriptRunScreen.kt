package com.scriptmanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scriptmanager.app.model.ScriptRepository
import com.scriptmanager.app.ui.components.ShellOutputItem
import com.scriptmanager.app.ui.components.TerminalOutput
import com.scriptmanager.app.ui.components.toOutputItem
import com.scriptmanager.app.util.RootShell
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ScriptRunScreen(
    scriptId: String,
    repository: ScriptRepository,
    onNavigateBack: () -> Unit
) {
    val scripts by repository.scripts.collectAsState()
    val script = scripts.find { it.id == scriptId } ?: return

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isRunning by remember { mutableStateOf(false) }
    var outputLines by remember { mutableStateOf<List<ShellOutputItem>>(emptyList()) }
    var runJob by remember { mutableStateOf<Job?>(null) }
    var showStopDialog by remember { mutableStateOf(false) }
    var hasRunOnce by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { runJob?.cancel() }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = script.displayName,
                navigationIcon = {
                    IconButton(onClick = {
                        if (isRunning) showStopDialog = true
                        else onNavigateBack()
                    }) {
                        Icon(MiuixIcons.Back, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MiuixTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    var showScriptPreview by remember { mutableStateOf(false) }
                    if (showScriptPreview) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = script.content.take(500).ifBlank { "(empty)" },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                modifier = Modifier.padding(8.dp),
                                maxLines = 6
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            text = if (showScriptPreview) "Hide Script" else "View Script",
                            onClick = { showScriptPreview = !showScriptPreview }
                        )

                        Button(
                            onClick = {
                                if (isRunning) {
                                    runJob?.cancel()
                                    isRunning = false
                                    outputLines = outputLines +
                                        ShellOutputItem.Info("── Terminated by user ──")
                                } else {
                                    isRunning = true
                                    hasRunOnce = true
                                    coroutineScope.launch {
                                        repository.recordRun(scriptId)
                                    }
                                    val preview = script.content.take(100)
                                        .let { if (script.content.length > 100) "$it..." else it }
                                    outputLines = outputLines +
                                        ShellOutputItem.Info("\$ $preview")
                                    runJob = coroutineScope.launch {
                                        try {
                                            RootShell.execute(script.content).collect { line ->
                                                outputLines = outputLines + line.toOutputItem()
                                            }
                                        } catch (e: Exception) {
                                            outputLines = outputLines +
                                                ShellOutputItem.Stderr("Error: ${e.message}")
                                            outputLines = outputLines +
                                                ShellOutputItem.Exit(-1)
                                        } finally {
                                            isRunning = false
                                        }
                                    }
                                }
                            },
                            colors = if (isRunning)
                                ButtonDefaults.buttonColorsPrimary().copy(color = MiuixTheme.colorScheme.error)
                            else
                                ButtonDefaults.buttonColorsPrimary()
                        ) {
                            Icon(
                                imageVector = if (isRunning) MiuixIcons.Undo else MiuixIcons.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRunning) "Stop" else "Run")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, top = 8.dp)
        ) {
            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = MiuixIcons.VerticalSplit,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = script.displayName,
                            style = MiuixTheme.textStyles.title4,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Runs: ${script.runCount}  ·  ${script.content.lines().size} lines",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Terminal output
            TerminalOutput(
                lines = outputLines,
                isRunning = isRunning,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Exit confirmation
    if (showStopDialog) {
        OverlayDialog(
            title = "Confirm Exit",
            summary = "A script is running. Exit and terminate it?",
            show = true,
            onDismissRequest = { showStopDialog = false }
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = "Stay",
                    onClick = { showStopDialog = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    text = "Terminate & Exit",
                    onClick = {
                        runJob?.cancel()
                        showStopDialog = false
                        onNavigateBack()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}
