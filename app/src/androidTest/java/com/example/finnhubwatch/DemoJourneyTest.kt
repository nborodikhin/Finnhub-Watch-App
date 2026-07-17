package com.example.finnhubwatch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun searchAddAndRestoreCachedWatchlistItem() {
        composeRule.onNodeWithContentDescription("Watchlist tab").performClick()
        listOf("DOCS", "NVDA", "AAPL", "AMZN", "MSFT").forEach { symbol ->
            if (composeRule.onAllNodesWithText(symbol).fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithText(symbol).performClick()
                composeRule.onNodeWithText("Remove from watchlist").performClick()
            }
        }
        composeRule.onNodeWithText("No results").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Search tab").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("aapl")
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("AAPL").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Apple Inc.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add AAPL to watchlist").performClick()
        composeRule.onNodeWithContentDescription("Watchlist tab").performClick()
        composeRule.onNodeWithText("AAPL").assertIsDisplayed()
        composeRule.onNodeWithText("CACHED").assertIsDisplayed()
    }

    @Test
    fun settingsDialogOpens() {
        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
