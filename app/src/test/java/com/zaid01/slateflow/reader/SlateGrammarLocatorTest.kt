package com.zaid01.slateflow.reader

import io.noties.prism4j.Prism4j
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlateGrammarLocatorTest {
    @Test
    fun locatorSupportsEssentialLanguages() {
        val locator = SlateGrammarLocator()
        val prism4j = Prism4j(locator)

        val languages = listOf("kotlin", "kt", "java", "python", "py", "javascript", "js", "typescript", "ts", "json", "bash", "sh", "cpp", "c", "xml", "html", "sql", "yaml")
        for (lang in languages) {
            val grammar = locator.grammar(prism4j, lang)
            assertNotNull("Grammar for $lang should be supported", grammar)
        }
        assertTrue(locator.languages().contains("kotlin"))
    }
}
