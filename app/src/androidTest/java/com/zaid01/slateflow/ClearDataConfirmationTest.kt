package com.zaid01.slateflow

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
}
