package com.raebae.ytdl.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.raebae.ytdl.R
import com.raebae.ytdl.ui.downloads.DownloadsScreen
import com.raebae.ytdl.ui.glass.GlassBottomBar
import com.raebae.ytdl.ui.glass.GlassTab
import com.raebae.ytdl.ui.home.HomeScreen
import com.raebae.ytdl.ui.playlist.PlaylistScreen
import com.raebae.ytdl.ui.settings.SettingsScreen
import com.raebae.ytdl.util.PlaylistBridge
import com.raebae.ytdl.util.UrlBridge

/** Backdrop of the whole app content, used by glass components. */
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
    val icon: ImageVector,
    val iconSelected: ImageVector
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

    // The whole content layer feeds the backdrop; the background color is
    // drawn first so glass also refracts the area behind lists (the docs call
    // this out: "the background outside of the content should be drawn too").
    val backgroundColor = MaterialTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }

    val tabs = listOf(
        TabSpec(Routes.HOME, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
        TabSpec(Routes.DOWNLOADS, R.string.nav_downloads, Icons.Outlined.Download, Icons.Filled.Download),
        TabSpec(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings)
    )
    val glassTabs = tabs.map { spec ->
        GlassTab(label = stringResource(spec.labelRes), icon = spec.icon)
    }
    val selectedTabIndex = remember(currentRoute) {
        tabs.indexOfFirst { it.route == currentRoute }.let { if (it < 0) 0 else it }
    }

    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalAppBackdrop provides backdrop) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
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
            backdrop = backdrop,
            tabs = glassTabs,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        )
    }
}
