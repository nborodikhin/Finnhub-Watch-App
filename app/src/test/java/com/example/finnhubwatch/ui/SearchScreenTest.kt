package com.example.finnhubwatch.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.finnhubwatch.domain.model.Instrument
import com.example.finnhubwatch.domain.model.Quote
import com.example.finnhubwatch.theme.FinnhubWatchTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

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
                                SearchResultUi(
                                    Instrument("AAPL", "Apple Inc."),
                                    SearchQuoteState.Available(Quote(327.10)),
                                    false,
                                ),
                            ),
                    ),
                    {},
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

    @Test
    fun unavailableResultCannotBeAdded() {
        composeRule.setContent {
            FinnhubWatchTheme {
                SearchContent(
                    SearchUiState(
                        query = "a",
                        status = SearchStatus.Results,
                        results =
                            listOf(
                                SearchResultUi(
                                    Instrument("APPL.TO", "Apple Inc."),
                                    SearchQuoteState.Unavailable("403"),
                                    false,
                                ),
                            ),
                    ),
                    {},
                    {},
                    {},
                    {},
                    {},
                )
            }
        }

        composeRule.onNodeWithText("Unavailable").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("APPL.TO unavailable, cannot add to watchlist").assertIsNotEnabled()
    }

    @Test
    fun watchedUnavailableResultCanBeRemoved() {
        composeRule.setContent {
            FinnhubWatchTheme {
                SearchContent(
                    SearchUiState(
                        query = "a",
                        status = SearchStatus.Results,
                        results =
                            listOf(
                                SearchResultUi(
                                    Instrument("APPL.TO", "Apple Inc."),
                                    SearchQuoteState.Unavailable("403"),
                                    true,
                                ),
                            ),
                    ),
                    {},
                    {},
                    {},
                    {},
                    {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Remove APPL.TO from watchlist").assertIsEnabled()
    }

    @Test
    fun pendingAndRetryableResultsCannotBeAdded() {
        composeRule.setContent {
            FinnhubWatchTheme {
                SearchContent(
                    SearchUiState(
                        query = "a",
                        status = SearchStatus.Results,
                        results =
                            listOf(
                                SearchResultUi(
                                    Instrument("AAPL", "Apple Inc."),
                                    SearchQuoteState.Pending,
                                    false,
                                ),
                                SearchResultUi(
                                    Instrument("APPL.TO", "Apple Inc."),
                                    SearchQuoteState.Retryable("503"),
                                    false,
                                ),
                            ),
                    ),
                    {},
                    {},
                    {},
                    {},
                    {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("AAPL price loading, cannot add to watchlist").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("APPL.TO quote retryable, cannot add to watchlist").assertIsNotEnabled()
        composeRule.onNodeWithText("Retryable error").assertIsDisplayed()
    }
}
