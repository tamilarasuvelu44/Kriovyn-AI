package com.orbita.personalagent

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KriovynNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun navigatesThroughPrimaryTabsAfterOnboarding() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("Launch Kriovyn AI").assertIsDisplayed()
                true
            }.getOrDefault(false) || runCatching {
                composeRule.onNodeWithText("Chat").assertIsDisplayed()
                true
            }.getOrDefault(false)
        }

        val launchNode = runCatching {
            composeRule.onNodeWithText("Launch Kriovyn AI").assertIsDisplayed()
            true
        }.getOrDefault(false)

        if (launchNode) {
            composeRule.onNodeWithText("Launch Kriovyn AI").performClick()
        }

        composeRule.onNodeWithText("Tasks").performClick()
        composeRule.onNodeWithText("Tasks & Reminders").assertIsDisplayed()

        composeRule.onNodeWithText("Notes").performClick()
        composeRule.onNodeWithText("Local Notes").assertIsDisplayed()

        composeRule.onNodeWithText("Memory").performClick()
        composeRule.onNodeWithText("Approved Personal Memory").assertIsDisplayed()

        composeRule.onNodeWithText("Approvals").performClick()
        composeRule.onNodeWithText("Tool Approval & Safety Log").assertIsDisplayed()
    }
}
