package com.raebae.ytdl.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Image
import com.raebae.ytdl.R
import com.raebae.ytdl.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionHeader(stringResource(R.string.settings_section_save))

            var subfolderText by remember(settings.subFolder) { mutableStateOf(settings.subFolder) }
            var subfolderFocused by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = subfolderText,
                onValueChange = { subfolderText = it },
                label = { Text(stringResource(R.string.settings_subfolder)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (!state.isFocused && subfolderFocused && subfolderText != settings.subFolder) {
                            viewModel.setSubFolder(subfolderText)
                        }
                        subfolderFocused = state.isFocused
                    }
            )

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

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader(stringResource(R.string.settings_section_appearance))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = settings.themeMode == ThemeMode.SYSTEM,
                    onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                    label = { Text(stringResource(R.string.theme_system)) }
                )
                FilterChip(
                    selected = settings.themeMode == ThemeMode.LIGHT,
                    onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                    label = { Text(stringResource(R.string.theme_light)) }
                )
                FilterChip(
                    selected = settings.themeMode == ThemeMode.DARK,
                    onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                    label = { Text(stringResource(R.string.theme_dark)) }
                )
            }

            SwitchRow(
                title = stringResource(R.string.settings_dynamic),
                subtitle = stringResource(R.string.settings_dynamic_desc),
                checked = settings.dynamicColor,
                onChange = viewModel::setDynamicColor
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader(stringResource(R.string.settings_section_engine))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                Spacer(Modifier.size(8.dp))
                Button(onClick = viewModel::updateEngine, enabled = !updating) {
                    if (updating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.settings_update))
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionHeader(stringResource(R.string.settings_section_about))

            val context = LocalContext.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl)))
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            val versionName = remember {
                runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                }.getOrNull() ?: "?"
            }
            Text(
                stringResource(R.string.app_version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
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
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
