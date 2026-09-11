package com.raebae.ytdl.ui.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.raebae.ytdl.util.FileOpener
import com.raebae.ytdl.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen() {
    val tasks by DownloadRepository.tasks.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val active = tasks.filter { it.status == Status.QUEUED || it.status == Status.RUNNING }
    val finished = tasks.filterNot { it.status == Status.QUEUED || it.status == Status.RUNNING }.reversed()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_downloads)) },
                actions = {
                    if (finished.isNotEmpty()) {
                        TextButton(onClick = { DownloadRepository.clearFinished() }) {
                            Text(stringResource(R.string.clear_finished))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.empty_downloads),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
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
}

@Composable
private fun ActiveTaskCard(task: DownloadRepository.DownloadTask, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                IconButton(onClick = onCancel) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cancel))
                }
            }
            Spacer(Modifier.size(8.dp))
            when (task.status) {
                Status.RUNNING -> {
                    LinearProgressIndicator(
                        progress = { task.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    val parts = mutableListOf<String>()
                    parts += "${task.progress.toInt()}%"
                    task.speed?.let { parts += it }
                    FormatUtils.formatEta(task.etaSec).takeIf { it.isNotEmpty() }?.let {
                        parts += it
                    }
                    Text(
                        parts.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
    Card(modifier = Modifier.fillMaxWidth()) {
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
                    Status.COMPLETED -> TextButton(onClick = onOpen) {
                        Text(stringResource(R.string.open))
                    }
                    Status.FAILED, Status.CANCELED -> TextButton(onClick = onRetry) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.retry))
                    }
                    else -> Unit
                }
            }
        }
    }
}
