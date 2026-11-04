package com.zaid01.slateflow.explore

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zaid01.slateflow.data.preferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal data class RootFolder(val grantUri: String, val location: FolderLocation?, val unavailable: Boolean = false)
internal data class ExploreState(
    val roots: List<RootFolder> = emptyList(),
    val path: List<FolderLocation> = emptyList(),
    val entries: List<FolderEntry> = emptyList(),
    val loading: Boolean = true,
    val message: String? = null,
    val revision: Int = 0,
)

internal class ExploreModel(application: Application) : AndroidViewModel(application) {
    private val repository = FolderRepository(application.contentResolver)
    private val rootsKey = stringSetPreferencesKey("explore_roots")
    private val mutableState = MutableStateFlow(ExploreState())
    val state = mutableState.asStateFlow()
    private val mutableSummaries = MutableStateFlow<Map<String, String>>(emptyMap())
    val summaries = mutableSummaries.asStateFlow()
    private val summarySlots = Semaphore(3)
    private var directoryJob: Job? = null
    private var generation = 0

    init {
        viewModelScope.launch {
            application.preferences.data.map { it[rootsKey].orEmpty() }.distinctUntilChanged().collect { grants ->
                val roots = grants.sorted().map { grant ->
                    try { RootFolder(grant, repository.root(Uri.parse(grant))) }
                    catch (exception: CancellationException) { throw exception }
                    catch (exception: Exception) { RootFolder(grant, null, unavailable = true) }
                }
                mutableState.value = mutableState.value.copy(roots = roots, loading = if (mutableState.value.path.isEmpty()) false else mutableState.value.loading)
            }
        }
    }

    fun addRoot(uri: Uri) {
        viewModelScope.launch {
            try {
                getApplication<Application>().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                repository.root(uri)
                getApplication<Application>().preferences.edit { it[rootsKey] = it[rootsKey].orEmpty() + uri.toString() }
                navigate(emptyList())
            } catch (exception: CancellationException) { throw exception }
            catch (exception: Exception) { mutableState.value = mutableState.value.copy(message = "Could not add that folder. Select it again and allow access.") }
        }
    }

    fun removeRoot(grantUri: String) {
        // Keep the provider grant for documents already in reading history.
        viewModelScope.launch {
            getApplication<Application>().preferences.edit { it[rootsKey] = it[rootsKey].orEmpty() - grantUri }
        }
    }

    fun enter(location: FolderLocation) = navigate(mutableState.value.path + location)
    fun back() = navigate(mutableState.value.path.dropLast(1))
    fun breadcrumb(index: Int) = navigate(mutableState.value.path.take(index + 1))
    fun refresh() {
        generation++
        mutableState.value = mutableState.value.copy(revision = generation)
        mutableSummaries.value = emptyMap()
        navigate(mutableState.value.path)
    }
    fun dismissMessage() { mutableState.value = mutableState.value.copy(message = null) }

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

    suspend fun loadSummary(location: FolderLocation) {
        if (mutableSummaries.value.containsKey(location.uri)) return
        val requestedGeneration = generation
        val label = try { summarySlots.withPermit { repository.summary(location).label } }
        catch (exception: CancellationException) { throw exception }
        catch (exception: Exception) { "Unavailable · Check folder access" }
        if (requestedGeneration != generation) return
        mutableSummaries.value = (mutableSummaries.value + (location.uri to label)).entries.toList().takeLast(128).associate { it.toPair() }
    }
}
