package com.raebae.ytdl.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.raebae.ytdl.R
import com.raebae.ytdl.ui.downloads.DownloadsScreen
import com.raebae.ytdl.ui.home.HomeScreen
import com.raebae.ytdl.ui.playlist.PlaylistScreen
import com.raebae.ytdl.ui.settings.SettingsScreen
import com.raebae.ytdl.util.PlaylistBridge

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

private val tabs = listOf(
    TabSpec(Routes.HOME, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    TabSpec(Routes.DOWNLOADS, R.string.nav_downloads, Icons.Outlined.Download, Icons.Filled.Download),
    TabSpec(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings)
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != Routes.PLAYLIST) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) tab.iconSelected else tab.icon,
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
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
}
