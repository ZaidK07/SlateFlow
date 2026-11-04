package com.zaid01.slateflow

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.zaid01.slateflow.reader.MarkdownScroll
import org.junit.Assert.*
import org.junit.Test

class MarkdownAnchorTest {
    @Test fun firstLayoutAndReflowDoNotOverwriteTheSavedCharacterAnchor() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val source = (1..300).joinToString("\n") { "Paragraph $it. Reading positions must survive native view layout and reflow." }
        val saved = source.indexOf("Paragraph 90.")
        var scroll: MarkdownScroll? = null
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                scroll = MarkdownScroll(activity).apply {
                    anchor = saved
                    lineFraction = 0.35f
                    text.text = source
                    text.textSize = 18f
                    onPosition = { fail("Initial layout must not publish a replacement position") }
                }
                activity.setContentView(scroll)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                val reader = scroll!!
                assertEquals(saved, reader.anchor)
                assertTrue(reader.scrollY > 0)
                val firstLine = reader.text.layout.getLineForOffset(saved)
                assertTrue(kotlin.math.abs(reader.scrollY - reader.text.layout.getLineTop(firstLine)) < reader.text.lineHeight)
                reader.text.setPadding(100, 0, 100, 32)
                reader.text.textSize = 24f
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                val reader = scroll!!
                assertEquals(saved, reader.anchor)
                val line = reader.text.layout.getLineForOffset(saved)
                assertTrue(kotlin.math.abs(reader.scrollY - reader.text.layout.getLineTop(line)) < reader.text.lineHeight)
            }
        }
    }
}
