package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.ArenaSubMode
import com.example.ui.components.ArenaHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArenaHeaderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun header_displaysModeChipsAndHandlesSelection() {
        var selectedMode = ArenaSubMode.BATTLE
        var reloadClicked = false

        composeRule.setContent {
            ArenaHeader(
                currentSubMode = selectedMode,
                onSubModeSelected = { selectedMode = it },
                progress = 0f,
                isDesktopMode = false,
                isOnline = true,
                onReload = { reloadClicked = true },
                onToggleDesktop = {},
                onResetSession = {},
                currentUrl = "https://arena.ai"
            )
        }

        // Check header title
        composeRule.onNodeWithText("Arena.ai").assertIsDisplayed()

        // Check submode chips
        composeRule.onNodeWithTag("submode_battle").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithTag("submode_leaderboard").assertIsDisplayed()
        composeRule.onNodeWithTag("submode_history").assertIsDisplayed()

        // Click leaderboard
        composeRule.onNodeWithTag("submode_leaderboard").performClick()
        assertEquals(ArenaSubMode.LEADERBOARD, selectedMode)

        // Click reload
        composeRule.onNodeWithTag("header_reload_button").performClick()
        assertTrue(reloadClicked)
    }
}
