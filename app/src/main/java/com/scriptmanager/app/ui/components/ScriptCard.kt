package com.scriptmanager.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scriptmanager.app.model.Script
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Script card component — Miuix style
 */
@Composable
fun ScriptCard(
    script: Script,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        pressFeedbackType = PressFeedbackType.Sink,
        showIndication = true,
        onClick = onRun,
        onLongPress = onEdit
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type icon surface
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (script.isFavorite)
                        MiuixTheme.colorScheme.tertiaryContainer
                    else
                        MiuixTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (script.isFavorite)
                                MiuixIcons.Favorites
                            else
                                MiuixIcons.VerticalSplit,
                            contentDescription = null,
                            tint = if (script.isFavorite)
                                MiuixTheme.colorScheme.onTertiaryContainer
                            else
                                MiuixTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = script.displayName,
                        style = MiuixTheme.textStyles.title4,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    if (script.description.isNotEmpty()) {
                        Text(
                            text = script.description,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Favorite toggle
                IconButton(
                    onClick = onToggleFavorite
                ) {
                    Icon(
                        imageVector = if (script.isFavorite)
                            MiuixIcons.FavoritesFill
                        else
                            MiuixIcons.Favorites,
                        contentDescription = if (script.isFavorite) "Unfavorite" else "Favorite",
                        tint = if (script.isFavorite)
                            MiuixTheme.colorScheme.primary
                        else
                            MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Bottom info row
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Run count and time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (script.runCount > 0) {
                        Text(
                            text = "Run ${script.runCount}x",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions
                        )
                    }

                    if (script.lastRunAt != null) {
                        if (script.runCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = formatTime(script.lastRunAt),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions
                        )
                    }
                }

                // Action buttons
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Rename,
                            contentDescription = "Edit",
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = MiuixIcons.Undo,
                            contentDescription = "Delete",
                            tint = MiuixTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}min ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
