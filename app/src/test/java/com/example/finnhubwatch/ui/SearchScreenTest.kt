package com.example.finnhubwatch.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.finnhubwatch.domain.model.Instrument
import com.example.finnhubwatch.domain.model.Quote
import com.example.finnhubwatch.domain.model.SearchResult
import com.example.finnhubwatch.theme.FinnhubWatchTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.intArrayOf

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchResultsShowInstrumentAndPrice() {
        composeRule.setContent {
            FinnhubWatchTheme {
                SearchContent(
                    SearchUiState(
                        query = "a",
                        status = SearchStatus.Results,
                        results =
                            listOf(
                                SearchResultUi(SearchResult(Instrument("AAPL", "Apple Inc."), Quote(327.10)), false),
                            ),
                    ),
                    {},
                    {},
                    {},
                    {},
                )
            }
        }

        composeRule.onNodeWithText("AAPL").assertIsDisplayed()
        composeRule.onNodeWithText("${'$'}327.10").assertIsDisplayed()
    }
}
