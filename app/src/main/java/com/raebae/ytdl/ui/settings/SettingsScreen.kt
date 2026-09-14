package com.raebae.ytdl.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import com.raebae.ytdl.R
import com.raebae.ytdl.data.ThemeMode
import com.raebae.ytdl.ui.glass.GlassBarBottomPadding
import com.raebae.ytdl.ui.glass.GlassBarTopPadding
import com.raebae.ytdl.ui.glass.GlassButton
import com.raebae.ytdl.ui.glass.GlassCard
import com.raebae.ytdl.ui.glass.GlassChip
import com.raebae.ytdl.ui.glass.GlassSnackbarHost
import com.raebae.ytdl.ui.glass.GlassSpinner
import com.raebae.ytdl.ui.glass.GlassTextField
import com.raebae.ytdl.ui.glass.GlassToggle

@Composable
fun SettingsScreen() {
    val app = LocalContext.current.applicationContext as android.app.Application
    val viewModel: SettingsViewModel = viewModel { SettingsViewModel(app) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val engineVersion by viewModel.engineVersion.collectAsStateWithLifecycle()
    val updating by viewModel.updating.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.message.collect { snackbarHostState.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionHeader(stringResource(R.string.settings_section_save))

            var subfolderText by remember(settings.subFolder) { mutableStateOf(settings.subFolder) }
            var subfolderFocused by remember { mutableStateOf(false) }
            GlassTextField(
                value = subfolderText,
                onValueChange = { subfolderText = it },
                hint = stringResource(R.string.settings_subfolder),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (!state.isFocused && subfolderFocused && subfolderText != settings.subFolder) {
                            viewModel.setSubFolder(subfolderText)
                        }
                        subfolderFocused = state.isFocused
                    }
            )

            Spacer(Modifier.size(12.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SwitchRow(
                        title = stringResource(R.string.settings_platform_folders),
                        subtitle = stringResource(R.string.settings_platform_folders_desc),
                        checked = settings.platformFolders,
                        onChange = viewModel::setPlatformFolders
                    )
                    SwitchRow(
                        title = stringResource(R.string.settings_fast_download),
                        subtitle = stringResource(R.string.settings_fast_download_desc),
                        checked = settings.fastDownload,
                        onChange = viewModel::setFastDownload
                    )
                }
            }

            SectionHeader(stringResource(R.string.settings_section_appearance))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    GlassChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = {
                            Text(
                                stringResource(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> R.string.theme_system
                                        ThemeMode.LIGHT -> R.string.theme_light
                                        ThemeMode.DARK -> R.string.theme_dark
                                    }
                                ),
                                color = if (settings.themeMode == mode) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    )
                }
            }

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SwitchRow(
                        title = stringResource(R.string.settings_dynamic),
                        subtitle = stringResource(R.string.settings_dynamic_desc),
                        checked = settings.dynamicColor,
                        onChange = viewModel::setDynamicColor
                    )
                }
            }

            SectionHeader(stringResource(R.string.settings_section_engine))

            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(
                                R.string.settings_engine_version,
                                engineVersion ?: "…"
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            stringResource(R.string.settings_engine_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    if (updating) {
                        GlassSpinner()
                    } else {
                        GlassButton(
                            onClick = viewModel::updateEngine,
                            modifier = Modifier
                                .fillMaxWidth(0.35f)
                        ) {
                            Text(
                                stringResource(R.string.settings_update),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            SectionHeader(stringResource(R.string.settings_section_about))

            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val context = LocalContext.current
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.developer_avatar),
                            contentDescription = stringResource(R.string.avatar),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.developer_name),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.developer_role),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val repoUrl = "https://github.com/raebaexxx/YtGrab"
                    Spacer(Modifier.size(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_repo),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                stringResource(R.string.settings_repo_url),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            val context = LocalContext.current
            val versionName = remember {
                runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrNull() ?: "?"
            }
            Text(
                stringResource(R.string.app_version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Spacer(Modifier.size(GlassBarBottomPadding))
        }

        GlassSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        com.raebae.ytdl.ui.glass.GlassToggle(checked = checked, onCheckedChange = onChange)
    }
}
