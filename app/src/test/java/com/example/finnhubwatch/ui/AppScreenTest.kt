package com.example.finnhubwatch.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.finnhubwatch.theme.FinnhubWatchTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class AppScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsDialogShowsRequiredActions() {
        composeRule.setContent {
            FinnhubWatchTheme {
                SettingsDialog("", {}, {}, {})
            }
        }

        composeRule.onNodeWithText("Settings").assertIsDisplayed()
        composeRule.onNodeWithText("finnhub.io API key (leave empty for demo mode)").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    }
}
