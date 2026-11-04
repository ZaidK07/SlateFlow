package com.zaid01.slateflow

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zaid01.slateflow.dashboard.DashboardScreen
import com.zaid01.slateflow.data.*
import com.zaid01.slateflow.explore.ExploreModel
import com.zaid01.slateflow.explore.ExploreScreen
import com.zaid01.slateflow.reader.DocumentReader
import com.zaid01.slateflow.ui.components.*

private enum class HomeTab(val title: String, val icon: AppSymbol) {
    DASHBOARD("Dashboard", AppSymbol.DASHBOARD), EXPLORE("Explore", AppSymbol.FOLDER)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SlateApp(model: SlateModel, settings: ReaderSettings) {
    val document by model.active.collectAsStateWithLifecycle()
    val documents by model.recent.collectAsStateWithLifecycle()
    val loading by model.loading.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val dashboard by model.dashboard.collectAsStateWithLifecycle()
    val explorer: ExploreModel = viewModel()
    val exploreState by explorer.state.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var focused by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(HomeTab.DASHBOARD) }
    LaunchedEffect(document?.uri) { focused = false }
    val width = calculateWindowSizeClass(LocalContext.current as Activity).widthSizeClass
    val configuration = LocalConfiguration.current
    val wide = width != WindowWidthSizeClass.Compact
    val expanded = width == WindowWidthSizeClass.Expanded && configuration.screenWidthDp > configuration.screenHeightDp
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { model.open(it) } }
    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(explorer::addRoot) }
    val openFile = { picker.launch(arrayOf("application/pdf", "text/*", "application/octet-stream")) }
    val addFolder = { treePicker.launch(null) }
    BackHandler(document != null || showSettings || (tab == HomeTab.EXPLORE && exploreState.path.isNotEmpty())) {
        when {
            showSettings -> showSettings = false
            focused -> focused = false
            document != null -> model.home()
            else -> explorer.back()
        }
    }
    if (showSettings) {
        SettingsPage(settings, model::update) { showSettings = false }
    } else {
        Scaffold(
            topBar = {
                AnimatedVisibility(!focused, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(document?.name ?: "SlateFlow", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge)
                                if (document == null) Text(tab.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        navigationIcon = {
                            if (document != null) IconButton(onClick = model::home) { AppIcon(AppSymbol.BACK, "Back to ${tab.title}") }
                        },
                        actions = {
                            if (document != null) TextButton(onClick = { focused = true }) { Text("Focus") }
                            else if (wide) TextButton(onClick = openFile) { AppIcon(AppSymbol.ADD); Spacer(Modifier.width(8.dp)); Text("Open file") }
                            IconButton(onClick = { showSettings = true }) { AppIcon(AppSymbol.SETTINGS, "Settings") }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    )
                }
            },
            bottomBar = {
                if (!wide && document == null) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    HomeTab.entries.forEach { value ->
                        NavigationBarItem(selected = tab == value, onClick = { tab = value }, icon = { AppIcon(value.icon) }, label = { Text(value.title) })
                    }
                }
            },
            floatingActionButton = {
                if (!wide && document == null && tab == HomeTab.DASHBOARD) {
                    FloatingActionButton(onClick = openFile) {
                        AppIcon(AppSymbol.ADD, "Open a file")
                    }
                }
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (wide && document == null) NavigationRail(containerColor = MaterialTheme.colorScheme.background) {
                    Spacer(Modifier.height(16.dp))
                    HomeTab.entries.forEach { value ->
                        NavigationRailItem(selected = tab == value, onClick = { tab = value }, icon = { AppIcon(value.icon) }, label = { Text(value.title) }, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when {
                        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        document != null -> DocumentReader(document!!, settings, expanded, model)
                        tab == HomeTab.DASHBOARD -> DashboardScreen(documents, dashboard, model::customize, { model.open(it.sourceUri.ifBlank { it.uri }.toUri()) }, openFile)
                        else -> ExploreScreen(explorer, addFolder) { entry, folder -> model.open(entry.uri.toUri(), folder.name, folder.uri) }
                    }
                    if (focused && document != null) FilledTonalButton(onClick = { focused = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Text("Show controls") }
                }
            }
        }
    }
    if (error != null) AlertDialog(
        onDismissRequest = { model.error.value = null },
        title = { Text("Something needs attention") }, text = { Text(error!!) },
        confirmButton = { TextButton(onClick = { model.error.value = null }) { Text("Got it") } },
    )
}
