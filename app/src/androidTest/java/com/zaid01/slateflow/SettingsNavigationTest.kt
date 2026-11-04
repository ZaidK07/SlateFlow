package com.zaid01.slateflow

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SettingsNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun dashboardAndExploreTabsNavigateWithoutTheOldSidebar() {
        compose.onAllNodesWithText("Explore").onLast().performClick()
        compose.onNodeWithText("Your folders").assertIsDisplayed()
        compose.onAllNodesWithText("Dashboard").onLast().performClick()
        compose.onNodeWithText("Reading history").assertIsDisplayed()
        compose.onNodeWithContentDescription("Customize dashboard").performClick()
        compose.onNodeWithText("Your dashboard").assertIsDisplayed()
    }

    @Test fun gearOpensDedicatedSettingsPageAndBackReturnsToTheApp() {
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Your reading space").assertIsDisplayed()
        compose.onNodeWithText("Android theme colors").assertIsDisplayed()
        compose.onNodeWithText("Light", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Dark", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }
}
