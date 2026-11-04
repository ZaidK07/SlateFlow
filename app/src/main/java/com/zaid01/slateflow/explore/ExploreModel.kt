package com.zaid01.slateflow.explore

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zaid01.slateflow.data.DocumentIdentity
import com.zaid01.slateflow.data.preferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal data class RootFolder(val grantUri: String, val location: FolderLocation?, val unavailable: Boolean = false)
internal data class ExploreState(
    val roots: List<RootFolder> = emptyList(),
    val folders: List<IndexedFolder> = emptyList(),
    val path: List<FolderLocation> = emptyList(),
    val entries: List<FolderEntry> = emptyList(),
    val loading: Boolean = true,
    val scanning: Boolean = false,
    val visited: Int = 0,
    val scannedAt: Long? = null,
    val message: String? = null,
    val scanMessage: String? = null,
)

internal class ExploreModel(application: Application) : AndroidViewModel(application) {
    private val repository = FolderRepository(application.contentResolver)
    private val indexStore = FolderIndexStore(File(application.filesDir, "explore-index.json"))
    private val scanner = FolderScanner({ repository.children(it, sort = false) }, { DocumentIdentity.key(Uri.parse(it)) })
    private val rootsKey = stringSetPreferencesKey("explore_roots")
    private val showPathKey = booleanPreferencesKey("explore_show_paths")
    private val showSizeKey = booleanPreferencesKey("explore_show_sizes")
    private val showCountsKey = booleanPreferencesKey("explore_show_counts")
    private val sortKey = stringPreferencesKey("explore_sort")
    val display = application.preferences.data.map { values ->
        ExploreDisplaySettings(
            showPath = values[showPathKey] ?: true,
            showSize = values[showSizeKey] ?: true,
            showCounts = values[showCountsKey] ?: true,
            sort = ExploreSort.entries.firstOrNull { it.name == values[sortKey] } ?: ExploreSort.PATH,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ExploreDisplaySettings())
    private val mutableState = MutableStateFlow(ExploreState())
    val state = mutableState.asStateFlow()
    private var directoryJob: Job? = null
    private var scanJob: Job? = null
    private var rootRefreshJob: Job? = null
    private var scanGeneration = 0

    init {
        viewModelScope.launch {
            application.preferences.data.map { it[rootsKey].orEmpty() }.distinctUntilChanged().collectLatest { grants ->
                scanJob?.cancel()
                rootRefreshJob?.cancel()
                directoryJob?.cancel()
                mutableState.value = mutableState.value.copy(
                    roots = mutableState.value.roots.filter { it.grantUri in grants },
                    folders = mutableState.value.folders.filter { it.grantUri in grants },
                    path = emptyList(), entries = emptyList(),
                )
                val generation = ++scanGeneration
                val roots = resolveRoots(grants.sorted())
                val available = roots.filterNot { it.unavailable }.map { it.grantUri }.toSet()
                val cached = indexStore.loadAny()
                val plan = folderScanPlan(grants, cached?.roots)
                val visible = (cached?.folders ?: mutableState.value.folders).filter { it.grantUri in available }
                mutableState.value = ExploreState(
                    roots = roots, folders = visible,
                    scannedAt = cached?.scannedAt,
                    loading = false,
                    scanMessage = scanNotice(cached?.skipped ?: 0, cached?.limitReached ?: false, roots.count { it.unavailable }),
                )
                when {
                    plan.reuseIndex -> Unit
                    plan.grantsToScan.isNotEmpty() -> startScan(
                        generation, roots.filter { it.grantUri in plan.grantsToScan },
                        previous = if (plan.retainPrevious) visible else emptyList(),
                        previousSkipped = if (plan.retainPrevious) cached?.skipped ?: 0 else 0,
                        previousLimited = plan.retainPrevious && cached?.limitReached == true,
                    )
                    else -> {
                        mutableState.value = mutableState.value.copy(folders = emptyList(), scannedAt = null, scanMessage = null)
                        if (cached != null) persistIndex(SavedFolderIndex(emptySet(), emptyList(), System.currentTimeMillis()), generation)
                    }
                }
            }
        }
    }

    fun addRoot(uri: Uri) {
        viewModelScope.launch {
            try {
                getApplication<Application>().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                repository.root(uri)
                val grant = uri.toString()
                var alreadyAdded = false
                getApplication<Application>().preferences.edit {
                    alreadyAdded = grant in it[rootsKey].orEmpty()
                    it[rootsKey] = it[rootsKey].orEmpty() + grant
                }
                if (alreadyAdded) { navigate(emptyList()); refresh() }
            } catch (exception: CancellationException) { throw exception }
            catch (exception: Exception) { mutableState.value = mutableState.value.copy(message = "Could not add that location. Select it again and allow access.") }
        }
    }

    fun updateDisplay(settings: ExploreDisplaySettings) {
        viewModelScope.launch {
            getApplication<Application>().preferences.edit {
                it[showPathKey] = settings.showPath
                it[showSizeKey] = settings.showSize
                it[showCountsKey] = settings.showCounts
                it[sortKey] = settings.sort.name
            }
        }
    }

    fun removeRoot(grantUri: String) {
        // Keep provider access for documents already in reading history.
        viewModelScope.launch {
            getApplication<Application>().preferences.edit { it[rootsKey] = it[rootsKey].orEmpty() - grantUri }
        }
    }

    fun enterIndexed(folder: IndexedFolder) = navigate(listOf(FolderLocation(folder.location.uri, folder.displayPath)))
    fun enter(location: FolderLocation) = navigate(mutableState.value.path + location)
    fun back() = navigate(mutableState.value.path.dropLast(1))
    fun breadcrumb(index: Int) = navigate(mutableState.value.path.take(index + 1))

    fun refresh() {
        scanJob?.cancel()
        rootRefreshJob?.cancel()
        val generation = ++scanGeneration
        val grants = mutableState.value.roots.map { it.grantUri }
        mutableState.value = mutableState.value.copy(scanning = grants.isNotEmpty(), visited = 0, scanMessage = null)
        rootRefreshJob = viewModelScope.launch {
            val roots = resolveRoots(grants)
            if (generation != scanGeneration) return@launch
            val available = roots.filterNot { it.unavailable }.map { it.grantUri }.toSet()
            mutableState.value = mutableState.value.copy(roots = roots, folders = mutableState.value.folders.filter { it.grantUri in available })
            startScan(generation)
        }
        if (mutableState.value.path.isNotEmpty()) navigate(mutableState.value.path)
    }

    private suspend fun resolveRoots(grants: List<String>): List<RootFolder> = grants.map { grant ->
        try { RootFolder(grant, repository.root(Uri.parse(grant))) }
        catch (exception: CancellationException) { throw exception }
        catch (exception: Exception) { RootFolder(grant, null, unavailable = true) }
    }

    private fun startScan(
        generation: Int,
        selectedRoots: List<RootFolder> = mutableState.value.roots,
        previous: List<IndexedFolder> = emptyList(),
        previousSkipped: Int = 0,
        previousLimited: Boolean = false,
    ) {
        scanJob?.cancel()
        val roots = mutableState.value.roots
        if (roots.isEmpty()) {
            mutableState.value = mutableState.value.copy(scanning = false, folders = emptyList(), scannedAt = null)
            return
        }
        mutableState.value = mutableState.value.copy(scanning = true, visited = 0, scanMessage = null)
        val hadCachedResults = mutableState.value.folders.isNotEmpty()
        scanJob = viewModelScope.launch {
            try {
                val startedAt = android.os.SystemClock.elapsedRealtime()
                val result = withContext(Dispatchers.IO) {
                    scanner.scan(selectedRoots.mapNotNull { root -> root.location?.let { ScanRoot(root.grantUri, it) } }) { progress ->
                        withContext(Dispatchers.Main.immediate) {
                            if (generation == scanGeneration) {
                                mutableState.value = mutableState.value.copy(
                                    visited = progress.visited,
                                    folders = if (hadCachedResults) mutableState.value.folders else progress.folders,
                                )
                            }
                        }
                    }
                }
                if (generation != scanGeneration) return@launch
                android.util.Log.d("SlateFlowScan", "Scanned ${result.visited} folders; indexed ${result.folders.size}; skipped ${result.skipped}; ${android.os.SystemClock.elapsedRealtime() - startedAt} ms")
                val completedAt = System.currentTimeMillis()
                val folders = withContext(Dispatchers.IO) {
                    mergeIndexedFolders(previous, result.folders) { DocumentIdentity.key(Uri.parse(it)) }
                }
                val skipped = previousSkipped + result.skipped
                val limited = previousLimited || result.limitReached
                mutableState.value = mutableState.value.copy(
                    folders = folders, scanning = false, visited = result.visited,
                    scannedAt = completedAt, scanMessage = scanNotice(skipped, limited, roots.count { it.unavailable }),
                )
                persistIndex(SavedFolderIndex(roots.map { it.grantUri }.toSet(), folders, completedAt, skipped, limited), generation)
            } catch (exception: CancellationException) { throw exception }
            catch (exception: Exception) {
                if (generation == scanGeneration) mutableState.value = mutableState.value.copy(scanning = false, scanMessage = "The scan could not finish. Refresh to try again.")
            }
        }
    }

    private suspend fun persistIndex(index: SavedFolderIndex, generation: Int) {
        try {
            indexStore.save(index)
        } catch (exception: CancellationException) { throw exception }
        catch (exception: Exception) {
            if (generation == scanGeneration) {
                mutableState.value = mutableState.value.copy(scanMessage = listOfNotNull(mutableState.value.scanMessage, "Could not save scan results. Check available storage.").joinToString(" "))
            }
        }
    }

    private fun scanNotice(skipped: Int, limited: Boolean, unavailable: Int): String? {
        val notices = buildList {
            if (skipped > 0) add("The last scan skipped $skipped ${if (skipped == 1) "folder" else "folders"}. Refresh to retry.")
            if (unavailable > 0) add("$unavailable scan ${if (unavailable == 1) "location is" else "locations are"} unavailable. Check access or choose the location again.")
            if (limited) add("The scan reached 20,000 folders. Add smaller locations to include the rest.")
        }
        return notices.takeIf { it.isNotEmpty() }?.joinToString(" ")
    }

    private fun navigate(path: List<FolderLocation>) {
        directoryJob?.cancel()
        mutableState.value = mutableState.value.copy(path = path, entries = emptyList(), loading = path.isNotEmpty(), message = null)
        if (path.isEmpty()) return
        directoryJob = viewModelScope.launch {
            try {
                val entries = repository.children(path.last())
                mutableState.value = mutableState.value.copy(entries = entries, loading = false)
            } catch (exception: CancellationException) { throw exception }
            catch (exception: Exception) {
                mutableState.value = mutableState.value.copy(loading = false, message = exception.message ?: "This folder could not be read.")
            }
        }
    }
}
