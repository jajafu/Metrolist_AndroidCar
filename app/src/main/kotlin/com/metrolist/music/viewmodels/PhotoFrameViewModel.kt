package com.metrolist.music.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.photo.FrameError
import com.metrolist.music.photo.FrameSettings
import com.metrolist.music.photo.PhotoCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PhotoFrameViewModel @Inject constructor(private val catalog: PhotoCatalog) : ViewModel() {
    val state = catalog.state
    private val _error = MutableStateFlow<FrameError?>(null)
    val error = _error.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _generation = MutableStateFlow(0)
    val generation = _generation.asStateFlow()
    private var operation: Job? = null
    private var operationId = 0L

    fun initialize() {
        if (!state.value.initialized) runOperation { catalog.initialize() }
    }

    fun addPhotos(uris: List<Uri>) = runOperation { catalog.addPhotos(uris); _generation.value++ }
    fun rescan() = runOperation { catalog.rescan(); _generation.value++ }
    fun removeSource(uri: String) = runOperation { catalog.removeSource(uri) }
    fun clear() = runOperation { catalog.clear() }
    fun updateSettings(settings: FrameSettings) = runOperation { catalog.updateSettings(settings) }
    fun cancelOperation() { operation?.cancel() }
    fun dismissError() {
        _error.value = null
        catalog.clearError()
    }

    suspend fun markUnreadable(uri: String) = catalog.markUnreadable(uri)

    fun unavailableUris(): Set<String> {
        val snapshot = state.value
        val unavailable = snapshot.sources.filter { it.needsPermission || it.unavailable }.mapTo(hashSetOf()) { it.uri }
        return if (unavailable.isEmpty()) emptySet()
        else snapshot.photos.filter { it.sourceUri in unavailable }.mapTo(hashSetOf()) { it.uri }
    }

    private fun runOperation(block: suspend () -> Unit) {
        if (operation?.isActive == true) return
        val id = ++operationId
        operation = viewModelScope.launch {
            _busy.value = true
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _error.value = FrameError.STORAGE
            } finally {
                if (operationId == id) _busy.value = false
            }
        }
    }
}
