package com.zaid01.slateflow.data

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.preferences.core.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class ReadingPosition(val anchor: Int = 0, val offset: Int = 0, val fraction: Float = 0f, val progress: Float = 0f)
private data class PositionWrite(val uri: String, val position: ReadingPosition, val barrier: CompletableDeferred<Unit>? = null)

class SlateModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(application, SlateDatabase::class.java, "slateflow.db").addMigrations(SlateDatabase.MIGRATION_1_2).build()
    private val dao = db.documents()
    private val dynamicKey = booleanPreferencesKey("dynamic_color")
    private val marginKey = floatPreferencesKey("markdown_margin")
    private val fontKey = floatPreferencesKey("markdown_font")
    private val spacingKey = floatPreferencesKey("markdown_spacing")
    private val activeKey = stringPreferencesKey("active_document")
    private val themeKey = stringPreferencesKey("theme_mode")
    private val groupKey = stringPreferencesKey("history_group")
    private val sortKey = stringPreferencesKey("history_sort")
    private val layoutKey = stringPreferencesKey("history_layout")
    private val filterKey = stringPreferencesKey("history_filter")
    private val welcomeKey = booleanPreferencesKey("show_welcome")
    private val continueKey = booleanPreferencesKey("show_continue")
    val settings = application.preferences.data.map {
        ReaderSettings(
            dynamic = it[dynamicKey] ?: false,
            margin = it[marginKey] ?: 32f,
            font = it[fontKey] ?: 18f,
            spacing = it[spacingKey] ?: 1.45f,
            themeMode = preferenceEnum(it[themeKey], ThemeMode.SYSTEM),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ReaderSettings())
    val dashboard = application.preferences.data.map {
        DashboardPreferences(
            grouping = preferenceEnum(it[groupKey], HistoryGrouping.DATE),
            sort = preferenceEnum(it[sortKey], HistorySort.RECENT),
            layout = preferenceEnum(it[layoutKey], HistoryLayout.GRID),
            filter = preferenceEnum(it[filterKey], FileFilter.ALL),
            showWelcome = it[welcomeKey] ?: true,
            showContinue = it[continueKey] ?: true,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DashboardPreferences())
    val recent = MutableStateFlow<List<Document>>(emptyList())
    val active = MutableStateFlow<Document?>(null)
    val loading = MutableStateFlow(true)
    val error = MutableStateFlow<String?>(null)
    private val writes = Channel<PositionWrite>(Channel.UNLIMITED)
    private var latest: PositionWrite? = null
    private var lastQueued: PositionWrite? = null
    private var lastWriteTime = 0L
    private var idleSave: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            for (write in writes) {
                if (write.barrier != null) { write.barrier.complete(Unit); continue }
                val p = write.position
                try { dao.savePosition(write.uri, p.anchor, p.offset, p.fraction, p.progress) }
                catch (e: Exception) { error.value = "Your reading position could not be saved. Check available storage." }
            }
        }
        viewModelScope.launch {
            try {
                refresh()
                val uri = application.preferences.data.first()[activeKey]
                if (uri != null) active.value = withContext(Dispatchers.IO) { dao.find(uri) }
            } catch (e: Exception) { error.value = "Could not restore your library: ${e.message}" }
            finally { loading.value = false }
        }
    }

    suspend fun refresh() { recent.value = withContext(Dispatchers.IO) { dao.recent() } }
    fun open(uri: Uri, folderName: String? = null, folderUri: String? = null) {
        flush()
        viewModelScope.launch {
            loading.value = true
            try {
                val barrier = CompletableDeferred<Unit>()
                writes.send(PositionWrite("", ReadingPosition(), barrier))
                barrier.await()
                val doc = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    if (!android.provider.DocumentsContract.isTreeUri(uri) &&
                        resolver.persistedUriPermissions.none { it.uri == uri && it.isReadPermission }) {
                        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: uri.lastPathSegment.orEmpty()
                    val type = resolver.getType(uri)
                    val kind = when {
                        type == "application/pdf" || name.endsWith(".pdf", true) -> "pdf"
                        name.endsWith(".md", true) || name.endsWith(".markdown", true) || type == "text/markdown" || type == "text/plain" -> "md"
                        else -> error("Choose a PDF or Markdown file.")
                    }
                    val identity = DocumentIdentity.key(uri)
                    (dao.findByKey(identity) ?: dao.find(uri.toString()) ?: Document().apply { this.uri = uri.toString() }).apply {
                        this.name = name
                        this.kind = kind
                        this.documentKey = identity
                        this.sourceUri = uri.toString()
                        if (folderName != null) this.folderName = folderName
                        if (folderUri != null) this.folderUri = folderUri
                        openedAt = System.currentTimeMillis()
                        dao.put(this)
                    }
                }
                latest = null; lastQueued = null
                active.value = doc
                getApplication<Application>().preferences.edit { it[activeKey] = doc.uri }
                refresh()
            } catch (e: Exception) { error.value = e.message ?: "This file could not be opened. Try selecting it again." }
            finally { loading.value = false }
        }
    }
    fun home() {
        flush()
        active.value = null
        viewModelScope.launch {
            val barrier = CompletableDeferred<Unit>()
            writes.send(PositionWrite("", ReadingPosition(), barrier)); barrier.await()
            getApplication<Application>().preferences.edit { it.remove(activeKey) }; refresh()
        }
    }
    fun position(uri: String, p: ReadingPosition) {
        val write = PositionWrite(uri, p)
        if (latest == write) return
        latest = write
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastWriteTime >= 250) flush()
        idleSave?.cancel()
        idleSave = viewModelScope.launch { delay(150); flush() }
    }
    fun flush() {
        latest?.let { if (it != lastQueued) { writes.trySend(it); lastQueued = it; lastWriteTime = android.os.SystemClock.elapsedRealtime() } }
    }
    fun customize(preferences: DashboardPreferences) {
        viewModelScope.launch {
            getApplication<Application>().preferences.edit {
                it[groupKey] = preferences.grouping.name
                it[sortKey] = preferences.sort.name
                it[layoutKey] = preferences.layout.name
                it[filterKey] = preferences.filter.name
                it[welcomeKey] = preferences.showWelcome
                it[continueKey] = preferences.showContinue
            }
        }
    }
    fun update(settings: ReaderSettings) { viewModelScope.launch {
        getApplication<Application>().preferences.edit {
            it[dynamicKey] = settings.dynamic; it[marginKey] = settings.margin
            it[fontKey] = settings.font; it[spacingKey] = settings.spacing
            it[themeKey] = settings.themeMode.name
        }
    } }
}
