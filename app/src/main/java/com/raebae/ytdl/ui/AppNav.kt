package com.raebae.ytdl.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.raebae.ytdl.R
import com.raebae.ytdl.data.DownloadRepository
import com.raebae.ytdl.data.DownloadRepository.Status
import com.raebae.ytdl.ui.downloads.DownloadsScreen
import com.raebae.ytdl.ui.glass.GlassBottomBar
import com.raebae.ytdl.ui.glass.GlassChip
import com.raebae.ytdl.ui.glass.GlassIconButton
import com.raebae.ytdl.ui.glass.GlassTab
import com.raebae.ytdl.ui.glass.GlassTheme
import com.raebae.ytdl.ui.glass.GlassTopBar
import com.raebae.ytdl.ui.home.HomeScreen
import com.raebae.ytdl.ui.playlist.PlaylistScreen
import com.raebae.ytdl.ui.settings.SettingsScreen
import com.raebae.ytdl.util.PlaylistBridge
import com.raebae.ytdl.util.UrlBridge

/**
 * Backdrop for glass components. Two backdrops exist in the app:
 *
 * - the FULL backdrop (background + all screen content) is consumed only by
 *   the floating chrome (top/bottom bars) that lives in the overlay layer
 *   OUTSIDE the NavHost. A consumer inside the NavHost layer would make the
 *   hwui render graph cyclic — the consumer's display list would contain the
 *   layer that contains the consumer — which stack-overflows the
 *   RenderThread (SIGSEGV at startup).
 * - the CONTENT backdrop (static background only, never contains any
 *   consumer) is provided as [LocalAppBackdrop] inside the NavHost, so
 *   in-screen glass (cards, chips, buttons) still gets real vibrancy/blur
 *   of the background without any cycle.
 */
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }

object Routes {
    const val HOME = "home"
    const val DOWNLOADS = "downloads"
    const val SETTINGS = "settings"
    const val PLAYLIST = "playlist"
}

private data class TabSpec(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // A share/view intent can arrive while any tab is open; jump to Home so
    // the auto-fetch is actually visible to the user.
    LaunchedEffect(Unit) {
        UrlBridge.pendingUrl.collect { shared ->
            if (shared != null) {
                navController.navigate(Routes.HOME) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        }
    }

    // FULL backdrop: background first (the backdrop docs call this out:
    // "the background outside of the content should be drawn too"), then
    // the whole NavHost content — consumed by the floating bars only.
    val backgroundColor = MaterialTheme.colorScheme.background
    val appBackdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }
    // CONTENT backdrop: the static background layer below the screens,
    // recorded from a sibling box — can never contain a glass consumer.
    val contentBackdrop = rememberLayerBackdrop { drawContent() }

    val tabs = listOf(
        TabSpec(Routes.HOME, R.string.nav_home, Icons.Outlined.Home),
        TabSpec(Routes.DOWNLOADS, R.string.nav_downloads, Icons.Outlined.Download),
        TabSpec(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings)
    )
    val glassTabs = tabs.map { spec ->
        GlassTab(label = stringResource(spec.labelRes), icon = spec.icon)
    }
    val selectedTabIndex = remember(currentRoute) {
        tabs.indexOfFirst { it.route == currentRoute }.let { if (it < 0) 0 else it }
    }

    GlassTheme {
        Box(Modifier.fillMaxSize()) {
            // Static background layer: drawn below everything and recorded
            // into contentBackdrop for in-screen glass.
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind { drawRect(backgroundColor) }
                    .layerBackdrop(contentBackdrop)
            )

            // Screens see only the content backdrop; their glass refracts
            // the static background, never the layer that holds themselves.
            CompositionLocalProvider(LocalAppBackdrop provides contentBackdrop) {
                NavHost(
                    navController = navController,
                    startDestination = Routes.HOME,
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(appBackdrop)
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            onOpenPlaylist = { url ->
                                PlaylistBridge.pendingUrl = url
                                navController.navigate(Routes.PLAYLIST) { launchSingleTop = true }
                            }
                        )
                    }
                    composable(Routes.DOWNLOADS) { DownloadsScreen() }
                    composable(Routes.SETTINGS) { SettingsScreen() }
                    composable(Routes.PLAYLIST) {
                        PlaylistScreen(onBack = { navController.popBackStack() })
                    }
                }
            }

            // Floating chrome — siblings ABOVE the NavHost layer, never its
            // children. Only here the full backdrop is provided.
            CompositionLocalProvider(LocalAppBackdrop provides appBackdrop) {
                Box(Modifier.fillMaxSize()) {
                    GlassTopBarOverlay(
                        currentRoute = currentRoute,
                        onBack = { navController.popBackStack() }
                    )

                    GlassBottomBar(
                        selectedTabIndex = { selectedTabIndex },
                        onTabSelected = { index ->
                            val route = tabs[index].route
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        tabs = glassTabs,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Per-route floating top bar, rendered in the overlay layer of AppRoot so
 * it can safely refract the full app content below it.
 */
@Composable
private fun GlassTopBarOverlay(
    currentRoute: String?,
    onBack: () -> Unit
) {
    val homeTitle = stringResource(R.string.app_name)
    val downloadsTitle = stringResource(R.string.nav_downloads)
    val settingsTitle = stringResource(R.string.nav_settings)
    val playlistTitle = stringResource(R.string.playlist_title)
    val backLabel = stringResource(R.string.back)
    val clearLabel = stringResource(R.string.clear_finished)
    val slot = Modifier
        .statusBarsPadding()
        .padding(horizontal = 12.dp, vertical = 8.dp)

    when (currentRoute) {
        Routes.DOWNLOADS -> GlassTopBar(
            title = downloadsTitle,
            modifier = slot,
            actions = { GlassClearFinishedAction(label = clearLabel) }
        )
        Routes.SETTINGS -> GlassTopBar(
            title = settingsTitle,
            modifier = slot
        )
        Routes.PLAYLIST -> GlassTopBar(
            title = playlistTitle,
            modifier = slot,
            navigationIcon = {
                GlassIconButton(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backLabel
                )
            }
        )
        else -> GlassTopBar(
            title = homeTitle,
            modifier = slot
        )
    }
}

/** "Clear finished" chip in the Downloads top bar when finished tasks exist. */
@Composable
private fun GlassClearFinishedAction(label: String) {
    val tasks by DownloadRepository.tasks.collectAsStateWithLifecycle()
    val finished = tasks.filter {
        it.status == Status.COMPLETED ||
            it.status == Status.FAILED ||
            it.status == Status.CANCELED
    }
    if (finished.isNotEmpty()) {
        GlassChip(
            selected = false,
            onClick = { DownloadRepository.clearFinished() },
            label = {
                Text(label, color = MaterialTheme.colorScheme.primary)
            }
        )
    }
}
