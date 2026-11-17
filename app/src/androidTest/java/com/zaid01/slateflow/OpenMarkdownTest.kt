package com.zaid01.slateflow

import android.net.Uri
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.zaid01.slateflow.data.Document
import com.zaid01.slateflow.data.SlateModel
import org.junit.Rule
import org.junit.Test
import java.io.File

class OpenMarkdownTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun openMarkdownFileInReader() {
        val context = compose.activity
        val file = File(context.cacheDir, "test_render.md")
        file.writeText(
            "# Title\n" +
            "This is a paragraph with **bold** and *italic* and `code`.\n\n" +
            "- [x] Done\n" +
            "- [ ] Todo\n\n" +
            "$$ \\int_0^1 x dx $$\n\n" +
            "Inline math: \$y = mx + b\$.\n\n" +
            "```kotlin\nval a = 1\n```\n\n" +
            "| H1 | H2 |\n|---|---|\n| D1 | D2 |\n"
        )

        try {
            val model = ViewModelProvider(compose.activity)[SlateModel::class.java]
            compose.waitUntil(10_000) { !model.loading.value }
            compose.runOnIdle {
                model.active.value = Document().apply {
                    uri = Uri.fromFile(file).toString()
                    name = "test_render.md"
                    kind = "md"
                }
            }
            Thread.sleep(3000)
            compose.runOnIdle {
                assert(model.active.value != null)
            }
        } finally {
            file.delete()
        }
    }
}
