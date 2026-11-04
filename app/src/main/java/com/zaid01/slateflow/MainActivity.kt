package com.zaid01.slateflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import com.zaid01.slateflow.ui.theme.isReaderDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaid01.slateflow.data.SlateModel
import com.zaid01.slateflow.ui.theme.SlateFlowTheme

class MainActivity : ComponentActivity() {
    private val model: SlateModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            val settings by model.settings.collectAsStateWithLifecycle()
            val dark = isReaderDark(settings.themeMode)
            SideEffect {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                )
            }
            SlateFlowTheme(settings.dynamic, settings.themeMode) { SlateApp(model, settings) }
        }
    }
    override fun onPause() { model.flush(); super.onPause() }
}
