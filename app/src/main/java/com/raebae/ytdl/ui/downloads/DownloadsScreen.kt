package com.raebae.ytdl.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raebae.ytdl.R
import com.raebae.ytdl.data.DownloadRepository
import com.raebae.ytdl.data.DownloadRepository.Status
import com.raebae.ytdl.ui.glassTopBarSlot
import com.raebae.ytdl.ui.glass.GlassBarBottomPadding
import com.raebae.ytdl.ui.glass.GlassBarTopPadding
import com.raebae.ytdl.ui.glass.GlassCard
import com.raebae.ytdl.ui.glass.GlassChip
import com.raebae.ytdl.ui.glass.GlassIconButton
import com.raebae.ytdl.ui.glass.GlassProgressBar
import com.raebae.ytdl.ui.glass.GlassTopBar
import com.raebae.ytdl.util.FileOpener
import com.raebae.ytdl.util.FormatUtils

@Composable
fun DownloadsScreen() {
    val tasks by DownloadRepository.tasks.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val active = tasks.filter { it.status == Status.QUEUED || it.status == Status.RUNNING }
    val finished = tasks.filterNot { it.status == Status.QUEUED || it.status == Status.RUNNING }.reversed()

    Box(Modifier.fillMaxSize()) {
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.empty_downloads),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = GlassBarTopPadding, bottom = GlassBarBottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (active.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.downloads_active),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(active, key = { it.id }) { task ->
                        ActiveTaskCard(task) { DownloadRepository.cancel(task.id) }
                    }
                }
                if (finished.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.downloads_finished),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(finished, key = { it.id }) { task ->
                        FinishedTaskCard(
                            task,
                            onOpen = { FileOpener.open(context, task.filePath) },
                            onRetry = { DownloadRepository.retry(task.id) }
                        )
                    }
                }
            }
        }

        GlassTopBar(
            title = stringResource(R.string.nav_downloads),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .glassTopBarSlot(),
            actions = {
                if (finished.isNotEmpty()) {
                    GlassChip(
                        selected = false,
                        onClick = { DownloadRepository.clearFinished() },
                        label = {
                            Text(
                                stringResource(R.string.clear_finished),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun ActiveTaskCard(task: DownloadRepository.DownloadTask, onCancel: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        task.formatLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                GlassIconButton(
                    onClick = onCancel,
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.size(8.dp))
            when (task.status) {
                Status.RUNNING -> {
                    GlassProgressBar(
                        progress = { task.progress.takeIf { it > 0 }?.div(100f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.size(6.dp))
                    val parts = mutableListOf<String>()
                    if (task.progress > 0) {
                        parts += "${task.progress.toInt()}%"
                    } else {
                        task.bytesDone?.takeIf { it > 0 }
                            ?.let { parts += FormatUtils.formatBytes(it) }
                    }
                    task.speed?.let { parts += it }
                    if (task.progress > 0) {
                        FormatUtils.formatEta(task.etaSec).takeIf { it.isNotEmpty() }?.let {
                            parts += it
                        }
                    }
                    Text(
                        parts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    GlassProgressBar(progress = { null }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.size(6.dp))
                    Text(
                        stringResource(R.string.status_queued),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FinishedTaskCard(
    task: DownloadRepository.DownloadTask,
    onOpen: () -> Unit,
    onRetry: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (task.status) {
                    Status.COMPLETED -> Icons.Filled.CheckCircle
                    else -> Icons.Filled.ErrorOutline
                },
                contentDescription = null,
                tint = when (task.status) {
                    Status.COMPLETED -> Color(0xFF2E7D32)
                    Status.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = when (task.status) {
                    Status.COMPLETED -> listOfNotNull(
                        stringResource(R.string.status_done),
                        task.filePath?.let { path ->
                            path.removePrefix("/storage/emulated/0/").let { "…/${
                                path.substringAfterLast('/')
                            }" }
                        }
                    ).joinToString(" · ")
                    Status.FAILED -> task.error ?: stringResource(R.string.status_failed)
                    else -> stringResource(R.string.status_canceled)
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (task.status == Status.FAILED) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                when (task.status) {
                    Status.COMPLETED -> GlassChip(
                        selected = false,
                        onClick = onOpen,
                        label = {
                            Text(
                                stringResource(R.string.open),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    Status.FAILED, Status.CANCELED -> GlassChip(
                        selected = false,
                        onClick = onRetry,
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text(
                                stringResource(R.string.retry),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    else -> Unit
                }
            }
        }
    }
}
