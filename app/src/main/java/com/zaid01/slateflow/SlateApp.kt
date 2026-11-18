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
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.zaid01.slateflow.dashboard.HistoryScreen
import com.zaid01.slateflow.data.*
import com.zaid01.slateflow.explore.ExploreModel
import com.zaid01.slateflow.explore.ExploreScreen
import com.zaid01.slateflow.reader.DocumentReader
import com.zaid01.slateflow.ui.components.*

private enum class HomeTab(val title: String, val icon: AppSymbol) {
    DASHBOARD("Dashboard", AppSymbol.DASHBOARD),
    EXPLORE("Explore", AppSymbol.FOLDER),
    HISTORY("History", AppSymbol.HISTORY),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun SlateApp(model: SlateModel, settings: ReaderSettings) {
    val document by model.active.collectAsStateWithLifecycle()
    val documents by model.recent.collectAsStateWithLifecycle()
    val loading by model.loading.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val unavailable by model.unavailable.collectAsStateWithLifecycle()
    val dashboard by model.dashboard.collectAsStateWithLifecycle()
    val explorer: ExploreModel = viewModel()
    val exploreState by explorer.state.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var focused by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(HomeTab.DASHBOARD) }

    LaunchedEffect(document?.uri) {
        focused = false
        if (document == null) {
            explorer.resetToRoot()
        }
    }
    LaunchedEffect(tab) {
        if (tab == HomeTab.EXPLORE) {
            explorer.resetToRoot()
        }
    }

    val context = LocalContext.current
    val width = calculateWindowSizeClass(context as Activity).widthSizeClass
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
            document != null -> {
                model.home()
                explorer.resetToRoot()
            }
            else -> explorer.back()
        }
    }

    if (showSettings) {
        SettingsPage(
            settings = settings,
            update = model::update,
            markAllRead = { isRead ->
                val files = explorer.state.value.files.map { it.uri.toUri() to it.name }
                model.markAllRead(isRead, files)
            },
            clearAppData = {
                try {
                    if (!context.getSystemService(android.app.ActivityManager::class.java).clearApplicationUserData()) {
                        showSettings = false
                        model.error.value = "Android could not clear the app data. Try again."
                    }
                } catch (exception: Exception) {
                    showSettings = false
                    model.error.value = "Android could not clear the app data. Try again."
                }
            },
            back = { showSettings = false }
        )
    } else {
        Scaffold(
            topBar = {
                AnimatedVisibility(!focused, enter = fadeIn(tween(180)), exit = fadeOut(tween(180))) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (document == null) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = androidx.compose.ui.graphics.Color(0xFF0A6EBD),
                                        shadowElevation = 2.dp,
                                    ) {
                                        Image(painterResource(R.drawable.ic_slateflow_brand), contentDescription = null, modifier = Modifier.size(44.dp))
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(document?.name ?: "SlateFlow", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                    if (document == null) Text(tab.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        navigationIcon = {
                            if (document != null) IconButton(onClick = { model.home(); explorer.resetToRoot() }) {
                                AppIcon(AppSymbol.BACK, "Back to ${tab.title}")
                            }
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
                        NavigationBarItem(
                            selected = tab == value,
                            onClick = {
                                tab = value
                                if (value == HomeTab.EXPLORE) explorer.resetToRoot()
                            },
                            icon = { AppIcon(value.icon) },
                            label = { Text(value.title) }
                        )
                    }
                }
            },
            floatingActionButton = {
                if (!wide && document == null && (tab == HomeTab.DASHBOARD || tab == HomeTab.HISTORY)) {
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
                        NavigationRailItem(
                            selected = tab == value,
                            onClick = {
                                tab = value
                                if (value == HomeTab.EXPLORE) explorer.resetToRoot()
                            },
                            icon = { AppIcon(value.icon) },
                            label = { Text(value.title) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when {
                        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        document != null -> DocumentReader(document!!, settings, expanded, model)
                        tab == HomeTab.DASHBOARD -> DashboardScreen(documents, dashboard, model::customize, { model.open(it.sourceUri.ifBlank { it.uri }.toUri()) }, openFile)
                        tab == HomeTab.HISTORY -> HistoryScreen(
                            documents = documents,
                            open = { model.open(it.sourceUri.ifBlank { it.uri }.toUri()) },
                            remove = model::removeFromHistory,
                            toggleRead = { uri, isRead -> model.setRead(uri, isRead) },
                            openFile = openFile
                        )
                        else -> ExploreScreen(
                            model = explorer,
                            recentDocuments = documents,
                            addFolder = addFolder,
                            onMarkRead = { uri, isRead, name, kind, folderName, folderUri ->
                                model.setRead(uri, isRead, name, kind, folderName, folderUri)
                            },
                            onMarkFolderRead = { folderUri, isRead, files ->
                                model.setFolderRead(folderUri, isRead, files.map { it.uri.toUri() to it.name })
                            },
                            open = { entry, folder -> model.open(entry.uri.toUri(), folder.name, folder.uri) },
                            openIndexedFile = { file -> model.open(file.uri.toUri(), file.folderName, file.folderUri) }
                        )
                    }
                    if (focused && document != null) FilledTonalButton(onClick = { focused = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { Text("Show controls") }
                }
            }
        }
    }
    if (unavailable != null) MissingFileDialog(unavailable!!.name, { model.unavailable.value = null }, model::removeUnavailable)
    if (error != null) AlertDialog(
        onDismissRequest = { model.error.value = null },
        title = { Text("Something needs attention") }, text = { Text(error!!) },
        confirmButton = { TextButton(onClick = { model.error.value = null }) { Text("Got it") } },
    )
}
