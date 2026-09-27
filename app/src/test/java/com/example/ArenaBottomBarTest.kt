package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.components.ArenaBottomBar
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for the bottom navigation bar: all destinations render,
 * web destinations navigate, and tool destinations open their sheets.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArenaBottomBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        currentUrl: String = "https://arena.ai/",
        onNavigateToUrl: (String) -> Unit = {},
        onOpenBattleTracker: () -> Unit = {},
        onOpenPrompts: () -> Unit = {}
    ) {
        composeRule.setContent {
            ArenaBottomBar(
                currentUrl = currentUrl,
                onNavigateToUrl = onNavigateToUrl,
                onOpenBattleTracker = onOpenBattleTracker,
                onOpenPrompts = onOpenPrompts
            )
        }
    }

    @Test
    fun allDestinations_areDisplayed() {
        setContent()
        composeRule.onNodeWithTag("battle_nav_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("leaderboard_nav_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("history_nav_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("battle_log_nav_chip").assertIsDisplayed()
        composeRule.onNodeWithTag("prompts_lib_nav_chip").assertIsDisplayed()
        composeRule.onNodeWithText("Battle").assertIsDisplayed()
        composeRule.onNodeWithText("History").assertIsDisplayed()
    }

    @Test
    fun battleDestination_selectedOnHome() {
        setContent(currentUrl = "https://arena.ai/")
        composeRule.onNodeWithTag("battle_nav_chip").assertIsSelected()
    }

    @Test
    fun leaderboardDestination_navigatesToLeaderboard() {
        var navigated: String? = null
        setContent(onNavigateToUrl = { navigated = it })
        composeRule.onNodeWithTag("leaderboard_nav_chip").performClick()
        assertEquals("https://arena.ai/leaderboard", navigated)
    }

    @Test
    fun historyDestination_navigatesToHistory() {
        var navigated: String? = null
        setContent(onNavigateToUrl = { navigated = it })
        composeRule.onNodeWithTag("history_nav_chip").performClick()
        assertEquals("https://arena.ai/history/search", navigated)
    }

    @Test
    fun battleLogDestination_opensTracker() {
        var opened = false
        setContent(onOpenBattleTracker = { opened = true })
        composeRule.onNodeWithTag("battle_log_nav_chip").performClick()
        assertTrue(opened)
    }

    @Test
    fun promptsDestination_opensPrompts() {
        var opened = false
        setContent(onOpenPrompts = { opened = true })
        composeRule.onNodeWithTag("prompts_lib_nav_chip").performClick()
        assertTrue(opened)
    }
}
