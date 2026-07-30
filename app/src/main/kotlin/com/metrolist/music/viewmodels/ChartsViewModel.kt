/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.pages.ChartsPage
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChartsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(ContentLoadState<ChartsPage>())
    internal val uiState = _uiState.asStateFlow()

    fun loadCharts() {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.loading()

        viewModelScope.launch {
            try {
                YouTube
                    .getChartsPage()
                    .onSuccess { page ->
                        _uiState.value = _uiState.value.loaded(page)
                    }.onFailure(::handleFailure)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFailure(e)
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading) return
        val currentPage = _uiState.value.content ?: return
        val continuation = currentPage.continuation ?: return
        _uiState.value = _uiState.value.loading()

        viewModelScope.launch {
            try {
                YouTube
                    .getChartsPage(continuation)
                    .onSuccess { newPage ->
                        _uiState.value =
                            _uiState.value.loaded(
                                currentPage.copy(
                                    sections = currentPage.sections + newPage.sections,
                                    continuation = newPage.continuation,
                                ),
                            )
                    }.onFailure(::handleFailure)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFailure(e)
            }
        }
    }

    private fun handleFailure(error: Throwable) {
        if (error is CancellationException) throw error
        _uiState.value = _uiState.value.failed(error)
        reportException(error)
    }
}
