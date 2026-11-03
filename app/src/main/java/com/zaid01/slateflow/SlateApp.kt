package com.zaid01.slateflow

import android.app.Activity
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
                TextButton(onClick = { showSettings = true }) { Text("Settings") }
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
                            Card(onClick = { model.open(android.net.Uri.parse(last.uri)) }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
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
                            items(visible, key = { it.uri }) { doc -> DocumentCard(doc) { model.open(android.net.Uri.parse(doc.uri)) } }
                        } else LazyVerticalGrid(columns = GridCells.Adaptive(240.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 112.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(visible, key = { it.uri }) { doc -> DocumentCard(doc) { model.open(android.net.Uri.parse(doc.uri)) } }
                        }
                    }
                }
            }
            if (focused && document != null) FilledTonalButton(onClick = { focused = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Text("Show controls") }
        }
    }
    if (showSettings) ModalBottomSheet(onDismissRequest = { showSettings = false }, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Make it yours", style = MaterialTheme.typography.headlineSmall)
            Text("Quiet colors. Comfortable reading.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Use Android theme colors"); Text(if (settings.dynamic) "Your system palette" else "SlateFlow Signature Blue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Switch(checked = settings.dynamic, onCheckedChange = { model.update(settings.copy(dynamic = it)) })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingSlider("Markdown side margins", "${settings.margin.roundToInt()} dp", settings.margin, 16f..160f) { model.update(settings.copy(margin = it)) }
            SettingSlider("Text size", "${settings.font.roundToInt()} sp", settings.font, 14f..28f) { model.update(settings.copy(font = it)) }
            SettingSlider("Line spacing", "%.2f×".format(settings.spacing), settings.spacing, 1.15f..1.8f) { model.update(settings.copy(spacing = it)) }
            Text("Reading positions are saved automatically as you scroll.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (error != null) AlertDialog(onDismissRequest = { model.error.value = null }, title = { Text("Couldn’t open file") }, text = { Text(error!!) }, confirmButton = { TextButton(onClick = { model.error.value = null }) { Text("Got it") } })
}

@Composable
private fun SettingSlider(label: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, change: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(valueLabel, color = MaterialTheme.colorScheme.primary) }
        Slider(value = value, onValueChange = change, valueRange = range)
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
