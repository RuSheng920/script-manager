package com.scriptmanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scriptmanager.app.util.ShellLine
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Terminal output component — terminal-style script output display
 */
@Composable
fun TerminalOutput(
    lines: List<ShellOutputItem>,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom on new lines
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        // Terminal title bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MiuixTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Traffic light dots
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MiuixTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(10.dp)
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MiuixTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(10.dp)
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MiuixTheme.colorScheme.secondary,
                    modifier = Modifier.size(10.dp)
                ) {}
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Terminal Output",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                )
            }

            if (isRunning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InfiniteProgressIndicator(
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Running...",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.primary
                    )
                }
            } else if (lines.isNotEmpty()) {
                val lastExit = lines.lastOrNull { it is ShellOutputItem.Exit }
                if (lastExit != null) {
                    val exitCode = (lastExit as ShellOutputItem.Exit).code
                    Text(
                        text = if (exitCode == 0) "Success ✓" else "Exit code: $exitCode",
                        style = MiuixTheme.textStyles.footnote1,
                        color = if (exitCode == 0)
                            MiuixTheme.colorScheme.primary
                        else
                            MiuixTheme.colorScheme.error
                    )
                }
            }
        }

        // Output content
        if (lines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) "Waiting for output..." else "No output",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .horizontalScroll(scrollState)
                    .padding(8.dp)
            ) {
                items(lines) { item ->
                    TerminalLine(item)
                }
            }
        }
    }
}

@Composable
private fun TerminalLine(item: ShellOutputItem) {
    val (text, color) = when (item) {
        is ShellOutputItem.Stdout -> item.text to MiuixTheme.colorScheme.onSurface
        is ShellOutputItem.Stderr -> item.text to MiuixTheme.colorScheme.error
        is ShellOutputItem.Exit -> {
            val msg = if (item.code == 0) "── Process exited (0) ──" else "── Process exited (${item.code}) ──"
            msg to MiuixTheme.colorScheme.onSurfaceVariantActions
        }
        is ShellOutputItem.Info -> item.text to MiuixTheme.colorScheme.primary
    }

    SelectionContainer {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = color,
            lineHeight = 18.sp,
            modifier = Modifier.padding(vertical = 1.dp)
        )
    }
}

/**
 * Output item model
 */
sealed class ShellOutputItem {
    data class Stdout(val text: String) : ShellOutputItem()
    data class Stderr(val text: String) : ShellOutputItem()
    data class Exit(val code: Int) : ShellOutputItem()
    data class Info(val text: String) : ShellOutputItem()
}

/**
 * Convert ShellLine to ShellOutputItem
 */
fun ShellLine.toOutputItem(): ShellOutputItem = when (this) {
    is ShellLine.Stdout -> ShellOutputItem.Stdout(text)
    is ShellLine.Stderr -> ShellOutputItem.Stderr(text)
    is ShellLine.Exit -> ShellOutputItem.Exit(code)
}
