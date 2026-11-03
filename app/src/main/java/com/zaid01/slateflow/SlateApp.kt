package com.zaid01.slateflow

import android.app.Activity
import androidx.core.net.toUri
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zaid01.slateflow.data.*
import com.zaid01.slateflow.reader.DocumentReader
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SlateApp(model: SlateModel, settings: ReaderSettings) {
    val document by model.active.collectAsStateWithLifecycle()
    val recent by model.recent.collectAsStateWithLifecycle()
    val loading by model.loading.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var focused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(document?.uri) { focused = false }
    var filter by rememberSaveable { mutableStateOf("All files") }
    val width = calculateWindowSizeClass(LocalContext.current as Activity).widthSizeClass
    val expanded = width == WindowWidthSizeClass.Expanded
    val compact = width == WindowWidthSizeClass.Compact
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(model::open) }
    val open = { picker.launch(arrayOf("application/pdf", "text/*", "application/octet-stream")) }
    BackHandler(document != null || showSettings) { if (showSettings) showSettings = false else if (focused) focused = false else model.home() }
    if (showSettings) {
        SettingsPage(settings, model::update) { showSettings = false }
        return
    }
    Scaffold(
        topBar = {
            AnimatedVisibility(visible = !focused, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
            TopAppBar(title = {
                Column {
                    Text(document?.name ?: "SlateFlow", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    if (document == null) Text("A little space to get lost in a good read.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }, navigationIcon = { if (document != null) TextButton(onClick = model::home) { Text("Back") } }, actions = {
                if (document != null) TextButton(onClick = { focused = true }) { Text("Focus") }
                IconButton(onClick = { showSettings = true }) {
                    Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings")
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
            }
        }, floatingActionButton = {
            if (document == null) ExtendedFloatingActionButton(onClick = open, shape = RoundedCornerShape(24.dp)) { Text("+  Open a file", fontWeight = FontWeight.SemiBold) }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                document != null -> DocumentReader(document!!, settings, expanded, model)
                else -> Row(Modifier.fillMaxSize()) {
                    if (expanded) {
                        Surface(Modifier.width(264.dp).fillMaxHeight().padding(start = 24.dp, bottom = 24.dp, top = 16.dp), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("YOUR SPACE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text("Library", style = MaterialTheme.typography.headlineSmall)
                                listOf("All files", "PDF", "Markdown").forEach { item -> FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item) }, modifier = Modifier.fillMaxWidth()) }
                                Spacer(Modifier.weight(1f))
                                Text("Just you and the page.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Files stay on your device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = if (compact) 24.dp else 32.dp)) {
                        val last = recent.firstOrNull()
                        if (last != null) {
                            Text("PICK UP WHERE YOU LEFT OFF", Modifier.padding(top = 24.dp, bottom = 16.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Card(onClick = { model.open(last.uri.toUri()) }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(if (last.kind == "pdf") "PDF DOCUMENT" else "MARKDOWN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(last.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    LinearProgressIndicator(progress = { last.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                                    Text("${(last.progress * 100).roundToInt()}% read     ·     Continue reading →", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                        Text("Your library", Modifier.padding(top = 32.dp, bottom = 8.dp), style = MaterialTheme.typography.titleLarge)
                        if (!expanded) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("All files", "PDF", "Markdown").forEach { item -> FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item) }) }
                        }
                        val visible = recent.filter { filter == "All files" || (filter == "PDF" && it.kind == "pdf") || (filter == "Markdown" && it.kind == "md") }
                        if (visible.isEmpty()) {
                            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Text("Aa", Modifier.padding(24.dp), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary) }
                                Text(if (recent.isEmpty()) "Room for your next read" else "No ${filter.lowercase()} files yet", Modifier.padding(top = 24.dp), style = MaterialTheme.typography.titleLarge)
                                Text("Open a PDF or Markdown file.\nWe’ll keep your place for next time.", Modifier.padding(top = 8.dp, bottom = 96.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else if (compact) LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(visible, key = { it.uri }) { doc -> DocumentCard(doc) { model.open(doc.uri.toUri()) } }
                        } else LazyVerticalGrid(columns = GridCells.Adaptive(240.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 112.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(visible, key = { it.uri }) { doc -> DocumentCard(doc) { model.open(doc.uri.toUri()) } }
                        }
                    }
                }
            }
            if (focused && document != null) FilledTonalButton(onClick = { focused = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Text("Show controls") }
        }
    }
    if (error != null) AlertDialog(onDismissRequest = { model.error.value = null }, title = { Text("Couldn’t open file") }, text = { Text(error!!) }, confirmButton = { TextButton(onClick = { model.error.value = null }) { Text("Got it") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPage(settings: ReaderSettings, update: (ReaderSettings) -> Unit, back: () -> Unit) {
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

@Composable
private fun DocumentCard(doc: Document, open: () -> Unit) {
    OutlinedCard(onClick = open, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (doc.kind == "pdf") "PDF" else "MD", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("${(doc.progress * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(doc.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(DateUtils.getRelativeTimeSpanString(doc.openedAt).toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
