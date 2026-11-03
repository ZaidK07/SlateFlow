package com.zaid01.slateflow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SettingsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun gearOpensDedicatedSettingsPageAndBackReturnsToTheApp() {
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Your reading space").assertIsDisplayed()
        compose.onNodeWithText("Android theme colors").assertIsDisplayed()
        compose.onNodeWithText("Side margins").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }
}
