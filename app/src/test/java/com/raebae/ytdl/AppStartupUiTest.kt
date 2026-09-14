package com.raebae.ytdl

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.raebae.ytdl.data.SettingsState
import com.raebae.ytdl.ui.AppRoot
import com.raebae.ytdl.ui.theme.YtdlTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the full app composition (theme + AppRoot: GlassTheme, NavHost,
 * Home screen, glass bars) to catch startup crashes of the glass UI
 * locally — the class of bug that shipped twice in the first attempt.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = TestApp::class)
class AppStartupUiTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun appRoot_composes_and_draws_first_frame() {
        composeRule.setContent {
            YtdlTheme(SettingsState()) {
                AppRoot()
            }
        }
        composeRule.onRoot().assertExists()
        composeRule.waitForIdle()
    }
}

/** Plain Application: skip engine init and channels, we only draw UI here. */
class TestApp : android.app.Application()
