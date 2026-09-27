package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.ui.components.ArenaLoadingPill
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArenaLoadingPillTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pill_showsWhileLoading() {
        composeRule.setContent { ArenaLoadingPill(progress = 0.5f) }
        composeRule.onNodeWithTag("loading_pill").assertIsDisplayed()
    }

    @Test
    fun pill_hidesWhenIdleOrDone() {
        val progress = androidx.compose.runtime.mutableStateOf(0f)
        composeRule.setContent { ArenaLoadingPill(progress = progress.value) }
        composeRule.onNodeWithTag("loading_pill").assertIsNotDisplayed()

        progress.value = 1f
        composeRule.onNodeWithTag("loading_pill").assertIsNotDisplayed()
    }
}
