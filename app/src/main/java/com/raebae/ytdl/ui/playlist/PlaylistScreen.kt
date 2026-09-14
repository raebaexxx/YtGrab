package com.raebae.ytdl.ui.playlist

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.raebae.ytdl.ui.glassTopBarSlot
import com.raebae.ytdl.ui.glass.GlassBarBottomPadding
import com.raebae.ytdl.ui.glass.GlassBarTopPadding
import com.raebae.ytdl.ui.glass.GlassButton
import com.raebae.ytdl.ui.glass.GlassCard
import com.raebae.ytdl.ui.glass.GlassChip
import com.raebae.ytdl.ui.glass.GlassDialog
import com.raebae.ytdl.ui.glass.GlassIconButton
import com.raebae.ytdl.ui.glass.GlassSelectableCard
import com.raebae.ytdl.ui.glass.GlassSnackbarHost
import com.raebae.ytdl.ui.glass.GlassSpinner
import com.raebae.ytdl.ui.glass.GlassTopBar
import com.raebae.ytdl.ui.playlist.PlaylistViewModel.UiState
import com.raebae.ytdl.util.FormatUtils
import com.raebae.ytdl.util.rememberNotificationPermission
import kotlinx.coroutines.launch

private val heightOptions = listOf(2160, 1440, 1080, 720, 480, 360)

@OptIn(ExperimentalLayoutApi::class)
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
    val onDownloaded: (Int) -> Unit = { _ ->
        ensureNotificationPermission()
        scope.launch { snackbarHostState.showSnackbar(addedMessage) }
    }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            is UiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                GlassSpinner()
            }
            is UiState.Error -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.message, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    GlassChip(
                        selected = false,
                        onClick = onBack,
                        label = {
                            Text(
                                stringResource(R.string.back),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            }
            is UiState.Ready -> PlaylistContent(
                state = s,
                viewModel = viewModel,
                onDownloaded = onDownloaded,
                modifier = Modifier.fillMaxSize()
            )
        }

        GlassTopBar(
            title = stringResource(R.string.playlist_title),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .glassTopBarSlot(),
            navigationIcon = {
                GlassIconButton(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        )

        GlassSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
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

    Column(modifier = modifier) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = GlassBarTopPadding + 8.dp, start = 16.dp, end = 16.dp)
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassChip(
                selected = allSelected,
                onClick = { if (allSelected) viewModel.deselectAll() else viewModel.selectAll() },
                label = {
                    Text(
                        stringResource(
                            if (allSelected) R.string.playlist_deselect_all
                            else R.string.playlist_select_all
                        ),
                        color = if (allSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            )
            GlassChip(
                selected = false,
                onClick = { showQualityDialog = true },
                label = {
                    val codec = state.preset.vcodecPrefix?.let { p ->
                        codecOptions.firstOrNull { c -> c.first == p }?.second
                    }
                    Text(
                        stringResource(R.string.playlist_quality) + ": " +
                            "${state.preset.maxHeight}p" + (codec?.let { " · $it" } ?: ""),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(state.playlist.entries, key = { it.id.ifEmpty { it.url } }) { entry ->
                EntryRow(
                    entry = entry,
                    checked = entry.id in state.selectedIds,
                    onToggle = { viewModel.toggle(entry.id) }
                )
            }
        }

        val selectedCount = state.selectedIds.size
        GlassButton(
            onClick = {
                viewModel.download()
                onDownloaded(selectedCount)
            },
            enabled = selectedCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp, end = 16.dp,
                    top = 8.dp, bottom = GlassBarBottomPadding + 8.dp
                )
        ) {
            Icon(
                Icons.Filled.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.size(8.dp))
            Text(
                stringResource(R.string.playlist_download_n, selectedCount),
                color = MaterialTheme.colorScheme.onPrimary
            )
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
    val okLabel = stringResource(android.R.string.ok)
    GlassDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.playlist_quality),
        dismissText = okLabel,
        onDismissClick = onDismiss
    ) {
        Text(
            stringResource(R.string.quality_resolution),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            heightOptions.forEach { h ->
                GlassChip(
                    selected = preset.maxHeight == h,
                    onClick = { onSetHeight(h) },
                    label = {
                        Text(
                            "${h}p",
                            color = if (preset.maxHeight == h) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.quality_codec),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            codecOptions.forEach { (prefix, label) ->
                GlassChip(
                    selected = preset.vcodecPrefix == prefix,
                    onClick = { onSetCodec(prefix) },
                    label = {
                        Text(
                            label,
                            color = if (preset.vcodecPrefix == prefix) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: PlaylistEntryUi,
    checked: Boolean,
    onToggle: () -> Unit
) {
    GlassSelectableCard(
        selected = checked,
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (checked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
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
            if (checked) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
