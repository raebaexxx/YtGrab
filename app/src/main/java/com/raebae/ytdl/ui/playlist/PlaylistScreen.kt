package com.raebae.ytdl.ui.playlist

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.raebae.ytdl.R
import com.raebae.ytdl.data.PlaylistEntryUi
import com.raebae.ytdl.ui.playlist.PlaylistViewModel.UiState
import com.raebae.ytdl.util.FormatUtils
import com.raebae.ytdl.util.rememberNotificationPermission
import kotlinx.coroutines.launch

private val heightOptions = listOf(2160, 1440, 1080, 720, 480, 360)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlaylistScreen(
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: PlaylistViewModel = viewModel { PlaylistViewModel(app) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedMessage = stringResource(R.string.added_to_queue)
    val ensureNotificationPermission = rememberNotificationPermission()
    val onDownloaded: (Int) -> Unit = { count ->
        ensureNotificationPermission()
        scope.launch { snackbarHostState.showSnackbar(addedMessage) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.playlist_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val s = state) {
            is UiState.Loading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            is UiState.Error -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(s.message, style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            }
            is UiState.Ready -> PlaylistContent(
                state = s,
                viewModel = viewModel,
                onDownloaded = onDownloaded,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistContent(
    state: UiState.Ready,
    viewModel: PlaylistViewModel,
    onDownloaded: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showQualityDialog by remember { mutableStateOf(false) }
    val allSelected = state.selectedIds.size == state.playlist.entries.size
    val anyCodecLabel = stringResource(R.string.codec_any)
    val codecOptions = listOf(
        null to anyCodecLabel,
        "avc1" to "H.264",
        "vp09" to "VP9",
        "av01" to "AV1"
    )

    Column(modifier = modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.playlist.thumbnail != null) {
                    AsyncImage(
                        model = state.playlist.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 96.dp, height = 64.dp)
                    )
                    Spacer(Modifier.size(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        state.playlist.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sub = listOfNotNull(
                        state.playlist.uploader,
                        stringResource(R.string.playlist_videos, state.playlist.entries.size)
                    ).joinToString(" · ")
                    Text(
                        sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = allSelected,
                onClick = { if (allSelected) viewModel.deselectAll() else viewModel.selectAll() },
                label = {
                    Text(
                        stringResource(
                            if (allSelected) R.string.playlist_deselect_all
                            else R.string.playlist_select_all
                        )
                    )
                }
            )
            FilterChip(
                selected = false,
                onClick = { showQualityDialog = true },
                label = {
                    val codec = state.preset.vcodecPrefix?.let { p ->
                        codecOptions.firstOrNull { c -> c.first == p }?.second
                    }
                    Text(
                        stringResource(R.string.playlist_quality) + ": " +
                            "${state.preset.maxHeight}p" + (codec?.let { " · $it" } ?: "")
                    )
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
        ) {
            items(state.playlist.entries, key = { it.id.ifEmpty { it.url } }) { entry ->
                EntryRow(
                    entry = entry,
                    checked = entry.id in state.selectedIds,
                    onToggle = { viewModel.toggle(entry.id) }
                )
            }
        }

        Button(
            onClick = {
                viewModel.download()
                onDownloaded(state.selectedIds.size)
            },
            enabled = state.selectedIds.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp)
        ) {
            Text(stringResource(R.string.playlist_download_n, state.selectedIds.size))
        }
    }

    if (showQualityDialog) {
        QualityDialog(
            preset = state.preset,
            onSetHeight = viewModel::setMaxHeight,
            onSetCodec = viewModel::setCodec,
            onDismiss = { showQualityDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QualityDialog(
    preset: PlaylistViewModel.Preset,
    onSetHeight: (Int) -> Unit,
    onSetCodec: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val anyCodecLabel = stringResource(R.string.codec_any)
    val codecOptions = listOf(
        null to anyCodecLabel,
        "avc1" to "H.264",
        "vp09" to "VP9",
        "av01" to "AV1"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_quality)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.quality_resolution),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    heightOptions.forEach { h ->
                        FilterChip(
                            selected = preset.maxHeight == h,
                            onClick = { onSetHeight(h) },
                            label = { Text("${h}p") }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.quality_codec),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    codecOptions.forEach { (prefix, label) ->
                        FilterChip(
                            selected = preset.vcodecPrefix == prefix,
                            onClick = { onSetCodec(prefix) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("ОК") }
        }
    )
}

@Composable
private fun EntryRow(
    entry: PlaylistEntryUi,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val d = FormatUtils.formatDuration(entry.durationSec)
            if (d != "—") {
                Text(
                    d,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
