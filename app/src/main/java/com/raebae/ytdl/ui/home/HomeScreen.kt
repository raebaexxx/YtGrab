package com.raebae.ytdl.ui.home

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.raebae.ytdl.R
import com.raebae.ytdl.data.VideoFormatOption
import com.raebae.ytdl.ui.home.HomeViewModel.UiState
import com.raebae.ytdl.util.FormatUtils
import com.raebae.ytdl.util.rememberNotificationPermission
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPlaylist: (String) -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: HomeViewModel = viewModel { HomeViewModel(app) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val url by viewModel.url.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedMessage = stringResource(R.string.added_to_queue)
    val ensureNotificationPermission = rememberNotificationPermission()

    LaunchedEffect(Unit) {
        viewModel.openPlaylist.collect { onOpenPlaylist(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = viewModel::onUrlChanged,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.url_hint)) },
                        singleLine = true,
                        trailingIcon = {
                            if (url.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onUrlChanged("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = null)
                                }
                            }
                        }
                    )
                    val clipboard = LocalClipboardManager.current
                    IconButton(onClick = {
                        clipboard.getText()?.toString()?.let { viewModel.onUrlChanged(it.trim()) }
                    }) {
                        Icon(Icons.Filled.ContentPaste, contentDescription = stringResource(R.string.paste))
                    }
                }
            }
            item {
                Button(
                    onClick = { viewModel.fetch() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state !is UiState.Loading
                ) {
                    Text(stringResource(R.string.fetch))
                }
            }

            when (val s = state) {
                is UiState.Loading -> item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(24.dp))
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(24.dp))
                    }
                }
                is UiState.Error -> item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                s.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(onClick = { viewModel.fetch() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(Modifier.size(4.dp))
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                }
                is UiState.Ready -> {
                    item {
                        VideoCard(s.video)
                    }
                    if (viewModel.playlistUrl() != null) {
                        item {
                            AssistChip(
                                onClick = { viewModel.playlistUrl()?.let(onOpenPlaylist) },
                                label = { Text(stringResource(R.string.playlist_chip)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        Modifier.size(18.dp)
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
                        Button(
                            onClick = {
                                ensureNotificationPermission()
                                viewModel.download()
                                scope.launch { snackbarHostState.showSnackbar(addedMessage) }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = s.selected != null
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.btn_download))
                        }
                    }
                }
                UiState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun VideoCard(video: com.raebae.ytdl.data.VideoUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
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
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) androidx.compose.foundation.BorderStroke(
            2.dp, MaterialTheme.colorScheme.primary
        ) else null,
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
                        style = MaterialTheme.typography.titleMedium
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
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}
