package com.scriptmanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.scriptmanager.app.model.Script
import com.scriptmanager.app.model.ScriptRepository
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ScriptEditScreen(
    scriptId: String?,
    repository: ScriptRepository,
    onNavigateBack: () -> Unit
) {
    val scripts by repository.scripts.collectAsState()
    val existingScript = scriptId?.let { id -> scripts.find { it.id == id } }

    var name by remember(existingScript) {
        mutableStateOf(existingScript?.name ?: "")
    }
    var description by remember(existingScript) {
        mutableStateOf(existingScript?.description ?: "")
    }
    var content by remember(existingScript) {
        mutableStateOf(existingScript?.content ?: "")
    }
    var hasChanges by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isNew = existingScript == null
    val coroutineScope = rememberCoroutineScope()

    fun markChanged() { hasChanges = true }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = if (isNew) "New Script" else "Edit Script",
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges) showDiscardDialog = true
                        else onNavigateBack()
                    }) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Script name
            TextField(
                value = name,
                onValueChange = { name = it; markChanged() },
                label = "Script Name",
                singleLine = true
            )

            // Description
            TextField(
                value = description,
                onValueChange = { description = it; markChanged() },
                label = "Description (optional)",
                singleLine = true
            )

            // Script content
            TextField(
                value = content,
                onValueChange = { content = it; markChanged() },
                label = "Script Content",
                maxLines = Int.MAX_VALUE,
                textStyle = MiuixTheme.textStyles.main.copy(
                    fontFamily = FontFamily.Monospace
                )
            )

            // Quick insert chips
            SmallTitle(text = "Quick Insert")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        content += "\necho \"[INFO] \""
                        markChanged()
                    },
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("echo", style = MiuixTheme.textStyles.footnote2)
                }
                Button(
                    onClick = {
                        content += "\nif [ ]; then\n    \nfi"
                        markChanged()
                    },
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("if", style = MiuixTheme.textStyles.footnote2)
                }
                Button(
                    onClick = {
                        content += "\nfor i in ; do\n    \ndone"
                        markChanged()
                    },
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("for", style = MiuixTheme.textStyles.footnote2)
                }
                Button(
                    onClick = {
                        content += "\nwhile [ ]; do\n    \ndone"
                        markChanged()
                    },
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("while", style = MiuixTheme.textStyles.footnote2)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    coroutineScope.launch {
                        val script = Script(
                            id = existingScript?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name.ifBlank { "untitled.sh" },
                            description = description,
                            content = content,
                            filePath = existingScript?.filePath,
                            createdAt = existingScript?.createdAt ?: System.currentTimeMillis(),
                            modifiedAt = System.currentTimeMillis(),
                            lastRunAt = existingScript?.lastRunAt,
                            runCount = existingScript?.runCount ?: 0,
                            isFavorite = existingScript?.isFavorite ?: false
                        )
                        repository.saveScript(script)
                        onNavigateBack()
                    }
                },
                enabled = content.isNotBlank(),
                colors = ButtonDefaults.buttonColorsPrimary(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }

    // Discard changes dialog
    if (showDiscardDialog) {
        OverlayDialog(
            title = "Discard Changes?",
            summary = "You have unsaved changes. Discard them?",
            show = true,
            onDismissRequest = { showDiscardDialog = false }
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = "Keep Editing",
                    onClick = { showDiscardDialog = false },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                TextButton(
                    text = "Discard",
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary()
                )
            }
        }
    }
}
