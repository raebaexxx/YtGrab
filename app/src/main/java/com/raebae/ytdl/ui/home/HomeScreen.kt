package com.raebae.ytdl.ui.home

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.raebae.ytdl.R
import com.raebae.ytdl.data.VideoFormatOption
import com.raebae.ytdl.ui.glassTopBarSlot
import com.raebae.ytdl.ui.glass.GlassBarBottomPadding
import com.raebae.ytdl.ui.glass.GlassBarTopPadding
import com.raebae.ytdl.ui.glass.GlassButton
import com.raebae.ytdl.ui.glass.GlassCard
import com.raebae.ytdl.ui.glass.GlassChip
import com.raebae.ytdl.ui.glass.GlassIconButton
import com.raebae.ytdl.ui.glass.GlassSnackbarHost
import com.raebae.ytdl.ui.glass.GlassSpinner
import com.raebae.ytdl.ui.glass.GlassTextField
import com.raebae.ytdl.ui.glass.GlassTopBar
import com.raebae.ytdl.ui.glass.GlassSelectableCard
import com.raebae.ytdl.ui.home.HomeViewModel.UiState
import com.raebae.ytdl.util.FormatUtils
import com.raebae.ytdl.util.rememberNotificationPermission
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenPlaylist: (String) -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: HomeViewModel = viewModel { HomeViewModel(app) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val url by viewModel.url.collectAsStateWithLifecycle()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedMessage = stringResource(R.string.added_to_queue)
    val ensureNotificationPermission = rememberNotificationPermission()

    LaunchedEffect(Unit) {
        viewModel.openPlaylist.collect { onOpenPlaylist(it) }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = GlassBarTopPadding, bottom = GlassBarBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassTextField(
                        value = url,
                        onValueChange = viewModel::onUrlChanged,
                        hint = stringResource(R.string.url_hint),
                        modifier = Modifier.weight(1f),
                        trailing = {
                            if (url.isNotEmpty()) {
                                GlassIconButton(
                                    onClick = { viewModel.onUrlChanged("") },
                                    icon = Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    )
                    Spacer(Modifier.size(8.dp))
                    val clipboard = LocalClipboardManager.current
                    GlassIconButton(
                        onClick = {
                            clipboard.getText()?.toString()?.let { viewModel.onUrlChanged(it.trim()) }
                        },
                        icon = Icons.Filled.ContentPaste,
                        contentDescription = stringResource(R.string.paste)
                    )
                }
            }
            item {
                GlassButton(
                    onClick = { viewModel.fetch() },
                    enabled = state !is UiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.fetch),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            when (val s = state) {
                is UiState.Loading -> item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(24.dp))
                        GlassSpinner()
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
                is UiState.Error -> item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                s.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(12.dp))
                            GlassChip(
                                selected = false,
                                onClick = { viewModel.fetch() },
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
                        }
                    }
                }
                is UiState.Ready -> {
                    item { VideoCard(s.video) }
                    if (viewModel.playlistUrl() != null) {
                        item {
                            GlassChip(
                                selected = false,
                                onClick = { viewModel.playlistUrl()?.let(onOpenPlaylist) },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        stringResource(R.string.playlist_chip),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }
                    item {
                        Text(
                            stringResource(R.string.formats_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(s.video.formats, key = { it.formatSpec }) { option ->
                        FormatRow(
                            option = option,
                            selected = s.selected?.formatSpec == option.formatSpec,
                            onClick = { viewModel.selectFormat(option) }
                        )
                    }
                    item {
                        GlassButton(
                            onClick = {
                                ensureNotificationPermission()
                                viewModel.download()
                                scope.launch { snackbarHostState.showSnackbar(addedMessage) }
                            },
                            enabled = s.selected != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                stringResource(R.string.btn_download),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                UiState.Idle -> Unit
            }
        }

        GlassTopBar(
            title = stringResource(R.string.app_name),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .glassTopBarSlot()
        )

        GlassSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun VideoCard(video: com.raebae.ytdl.data.VideoUi) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (video.thumbnail != null) {
                    AsyncImage(
                        model = video.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(
                    video.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = listOfNotNull(
                    video.uploader,
                    FormatUtils.formatDuration(video.durationSec).takeIf { it != "—" }
                ).joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatRow(
    option: VideoFormatOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    GlassSelectableCard(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${option.height}p${if (option.fps > 30) option.fps.toString() else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "${option.vcodecLabel} · ${option.ext.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val progressiveText = stringResource(R.string.progressive_tag)
                val audioText = option.audioCodecLabel?.let { stringResource(R.string.audio_of, it) }
                val details = buildString {
                    append(FormatUtils.formatBytes(option.sizeBytes))
                    if (option.isProgressive) {
                        append(" · ")
                        append(progressiveText)
                    } else if (audioText != null) {
                        append(" · ")
                        append(audioText)
                    }
                }
                Text(
                    details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
