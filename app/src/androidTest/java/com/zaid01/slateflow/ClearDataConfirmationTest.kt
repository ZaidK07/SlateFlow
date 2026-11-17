package com.zaid01.slateflow

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsDisplayed
import com.zaid01.slateflow.data.ReaderSettings
import com.zaid01.slateflow.ui.theme.SlateFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ClearDataConfirmationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun clearingRequiresConfirmationAndCancelPreservesData() {
        var clearRequests = 0
        compose.setContent {
            SlateFlowTheme { SettingsPage(ReaderSettings(), {}, clearAppData = { clearRequests++ }, back = {}) }
        }
        compose.onNodeWithText("Clear app data").performScrollTo().performClick()
        compose.onNodeWithText("Clear all app data?").assertIsDisplayed()
        assertEquals(0, clearRequests)
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(0, clearRequests)
        compose.onNodeWithText("Clear app data").performScrollTo().performClick()
        compose.onNodeWithText("Clear everything").performClick()
        compose.runOnIdle { assertEquals(1, clearRequests) }
    }

    @Test fun markAllReadAndUnreadRequiresConfirmation() {
        var markReadCalls = 0
        var markUnreadCalls = 0
        compose.setContent {
            SlateFlowTheme {
                SettingsPage(
                    settings = ReaderSettings(),
                    update = {},
                    markAllRead = { isRead ->
                        if (isRead) markReadCalls++ else markUnreadCalls++
                    },
                    clearAppData = {},
                    back = {}
                )
            }
        }
        compose.onNodeWithText("Mark all read").performScrollTo().performClick()
        compose.onNodeWithText("Mark all as read?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(0, markReadCalls)

        compose.onNodeWithText("Mark all read").performScrollTo().performClick()
        compose.onNodeWithText("Mark all as read?").assertIsDisplayed()
        compose.onAllNodesWithText("Mark all read").onLast().performClick()
        assertEquals(1, markReadCalls)

        compose.onNodeWithText("Mark all unread").performScrollTo().performClick()
        compose.onNodeWithText("Mark all as unread?").assertIsDisplayed()
        compose.onAllNodesWithText("Mark all unread").onLast().performClick()
        assertEquals(1, markUnreadCalls)
    }
}
