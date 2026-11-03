package com.zaid01.slateflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zaid01.slateflow.data.ReaderSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(settings: ReaderSettings, update: (ReaderSettings) -> Unit, back: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Settings", fontWeight = FontWeight.SemiBold) }, navigationIcon = {
            IconButton(onClick = back) { Icon(painterResource(R.drawable.ic_back), contentDescription = "Back") }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 760.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your reading space", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text("A few small adjustments. A more comfortable read.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SettingsGroup("Appearance", "Quiet surfaces and a palette that feels like you.") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Android theme colors", style = MaterialTheme.typography.titleMedium)
                            Text("Use the accent colors from your system wallpaper.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = settings.dynamic, onCheckedChange = { update(settings.copy(dynamic = it)) })
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(if (settings.dynamic) "System palette is active" else "Signature Blue · SlateFlow’s cool, soothing palette", Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                SettingsGroup("Markdown", "Set the spacing and typography for your documents.") {
                    SettingSlider("Side margins", "dp", settings.margin, 16f..160f) { update(settings.copy(margin = it)) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingSlider("Text size", "sp", settings.font, 14f..28f) { update(settings.copy(font = it)) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingSlider("Line spacing", "×", settings.spacing, 1.15f..1.8f) { update(settings.copy(spacing = it)) }
                    Text("Margins adapt to smaller windows so the text stays readable. Your place follows the text when these settings change.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { update(settings.copy(margin = 32f, font = 18f, spacing = 1.45f)) }) { Text("Reset reading defaults") }
                }
                SettingsGroup("Reading memory", "Pick up where you left off.") {
                    Text("Your position is saved while you scroll, when you stop, and when you leave the reader.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Files stay on your device. SlateFlow uses the access you grant in the Android file picker.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("SlateFlow  ·  Made for a quieter read", Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}

@Composable
private fun SettingSlider(label: String, unit: String, value: Float, range: ClosedFloatingPointRange<Float>, change: (Float) -> Unit) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(if (unit == "×") "%.2f×".format(sliderValue) else "${sliderValue.roundToInt()} $unit", color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = sliderValue, onValueChange = { sliderValue = it }, onValueChangeFinished = { change(sliderValue) }, valueRange = range)
    }
}

