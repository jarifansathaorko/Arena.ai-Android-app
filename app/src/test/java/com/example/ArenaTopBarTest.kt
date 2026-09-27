package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.ui.components.ArenaTopBar
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for the top navigation bar: chips render, selection
 * follows the URL, and taps route to the right callbacks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArenaTopBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        currentUrl: String = "https://arena.ai/",
        canGoBack: Boolean = false,
        canGoForward: Boolean = false,
        onNavigateToUrl: (String) -> Unit = {},
        onOpenBattleTracker: () -> Unit = {},
        onOpenPrompts: () -> Unit = {},
        onNavigateBack: () -> Unit = {},
        onNavigateForward: () -> Unit = {},
        onReload: () -> Unit = {},
        onToggleDesktop: () -> Unit = {},
        onOpenSettings: () -> Unit = {}
    ) {
        composeRule.setContent {
            ArenaTopBar(
                currentUrl = currentUrl,
                pageTitle = "Arena AI",
                progress = 0f,
                isDesktopMode = false,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                onNavigateBack = onNavigateBack,
                onNavigateForward = onNavigateForward,
                onReload = onReload,
                onToggleDesktop = onToggleDesktop,
                onOpenPrompts = onOpenPrompts,
                onOpenBattleTracker = onOpenBattleTracker,
                onOpenSettings = onOpenSettings,
                onNavigateToUrl = onNavigateToUrl
            )
        }
    }

    @Test
    fun allQuickChips_areDisplayed() {
        setContent()
        // performScrollTo() forces composition inside the LazyRow so this
        // holds even on narrow test viewports.
        composeRule.onNodeWithTag("battle_nav_chip").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("leaderboard_nav_chip").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("history_nav_chip").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("battle_log_nav_chip").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("prompts_lib_nav_chip").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun leaderboardChip_navigatesToLeaderboard() {
        var navigated: String? = null
        setContent(onNavigateToUrl = { navigated = it })
        composeRule.onNodeWithTag("leaderboard_nav_chip").performClick()
        assertEquals("https://arena.ai/leaderboard", navigated)
    }

    @Test
    fun battleLogChip_opensTracker() {
        var opened = false
        setContent(onOpenBattleTracker = { opened = true })
        composeRule.onNodeWithTag("battle_log_nav_chip").performClick()
        assertTrue(opened)
    }

    @Test
    fun promptsChip_opensPrompts() {
        var opened = false
        setContent(onOpenPrompts = { opened = true })
        composeRule.onNodeWithTag("prompts_lib_nav_chip").performClick()
        assertTrue(opened)
    }

    @Test
    fun backButton_disabledWithoutHistory() {
        setContent(canGoBack = false)
        composeRule.onNodeWithTag("nav_back_button").assertIsNotEnabled()
    }

    @Test
    fun backButton_enabledFiresCallbackWithHistory() {
        var backed = false
        setContent(canGoBack = true, onNavigateBack = { backed = true })
        composeRule.onNodeWithTag("nav_back_button").performClick()
        assertTrue(backed)
    }

    @Test
    fun historyChip_labelVisibleOnHistoryPage() {
        setContent(currentUrl = "https://arena.ai/history/search")
        composeRule.onNodeWithText("History").assertIsDisplayed()
    }
}
