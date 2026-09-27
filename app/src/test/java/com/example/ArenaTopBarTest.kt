package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.components.ArenaTopBar
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for the slim top bar: navigation buttons, URL pill, and
 * the overflow menu that hosts reload / desktop toggle / tools.
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
        onNavigateBack: () -> Unit = {},
        onNavigateForward: () -> Unit = {},
        onReload: () -> Unit = {},
        onToggleDesktop: () -> Unit = {},
        onOpenPrompts: () -> Unit = {},
        onOpenBattleTracker: () -> Unit = {},
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
                onOpenSettings = onOpenSettings
            )
        }
    }

    private fun openOverflowMenu() {
        composeRule.onNodeWithTag("overflow_menu_button")
            .assertIsDisplayed()
            .performClick()
    }

    @Test
    fun topBar_showsNavButtonsAndHost() {
        setContent()
        composeRule.onNodeWithTag("nav_back_button").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_forward_button").assertIsDisplayed()
        composeRule.onNodeWithTag("overflow_menu_button").assertIsDisplayed()
        composeRule.onNodeWithText("arena.ai").assertIsDisplayed()
    }

    @Test
    fun overflowMenu_exposesSecondaryActions() {
        setContent()
        openOverflowMenu()
        composeRule.onNodeWithTag("reload_button").assertIsDisplayed()
        composeRule.onNodeWithTag("desktop_toggle_button").assertIsDisplayed()
        composeRule.onNodeWithTag("prompts_button").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_button").assertIsDisplayed()
    }

    @Test
    fun reloadMenuItem_firesCallbackAndDismisses() {
        var reloaded = false
        setContent(onReload = { reloaded = true })
        openOverflowMenu()
        composeRule.onNodeWithTag("reload_button").performClick()
        assertTrue(reloaded)
        composeRule.onNodeWithTag("reload_button").assertDoesNotExist()
    }

    @Test
    fun promptsMenuItem_opensPrompts() {
        var opened = false
        setContent(onOpenPrompts = { opened = true })
        openOverflowMenu()
        composeRule.onNodeWithTag("prompts_button").performClick()
        assertTrue(opened)
    }

    @Test
    fun desktopToggleMenuItem_firesCallback() {
        var toggled = false
        setContent(onToggleDesktop = { toggled = true })
        openOverflowMenu()
        composeRule.onNodeWithTag("desktop_toggle_button").performClick()
        assertTrue(toggled)
    }

    @Test
    fun settingsMenuItem_opensSettings() {
        var opened = false
        setContent(onOpenSettings = { opened = true })
        openOverflowMenu()
        composeRule.onNodeWithTag("settings_button").performClick()
        assertTrue(opened)
    }

    @Test
    fun battleLogMenuItem_opensTracker() {
        var opened = false
        setContent(onOpenBattleTracker = { opened = true })
        openOverflowMenu()
        composeRule.onNodeWithTag("battle_log_menu_item").performClick()
        assertTrue(opened)
    }

    @Test
    fun copyLinkMenuItem_dismissesWithoutCrashing() {
        setContent()
        openOverflowMenu()
        composeRule.onNodeWithTag("copy_link_menu_item").performClick()
        composeRule.onNodeWithTag("copy_link_menu_item").assertDoesNotExist()
    }

    @Test
    fun openBrowserMenuItem_dismissesWithoutCrashing() {
        setContent()
        openOverflowMenu()
        composeRule.onNodeWithTag("open_browser_menu_item").performClick()
        composeRule.onNodeWithTag("open_browser_menu_item").assertDoesNotExist()
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
}
