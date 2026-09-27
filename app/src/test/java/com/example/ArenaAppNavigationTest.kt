package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.ArenaTab
import com.example.ui.components.ArenaNavigationBar
import com.example.ui.components.ArenaNavigationRail
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArenaAppNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigationBar_displaysAllTabsAndSelectsTab() {
        var selected = ArenaTab.ARENA
        composeRule.setContent {
            ArenaNavigationBar(
                selectedTab = selected,
                onTabSelected = { selected = it }
            )
        }

        // Check tabs exist
        composeRule.onNodeWithTag("tab_arena").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithTag("tab_prompts").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_battles").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_settings").assertIsDisplayed()

        // Click prompts tab
        composeRule.onNodeWithTag("tab_prompts").performClick()
        assertEquals(ArenaTab.PROMPTS, selected)
    }

    @Test
    fun navigationRail_displaysAllTabsAndSelectsTab() {
        var selected = ArenaTab.ARENA
        composeRule.setContent {
            ArenaNavigationRail(
                selectedTab = selected,
                onTabSelected = { selected = it }
            )
        }

        // Check rail items exist
        composeRule.onNodeWithTag("rail_tab_arena").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithTag("rail_tab_prompts").assertIsDisplayed()
        composeRule.onNodeWithTag("rail_tab_battles").assertIsDisplayed()
        composeRule.onNodeWithTag("rail_tab_settings").assertIsDisplayed()

        // Click battles tab
        composeRule.onNodeWithTag("rail_tab_battles").performClick()
        assertEquals(ArenaTab.BATTLES, selected)
    }
}
