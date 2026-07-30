/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.filterExplicit
import com.metrolist.innertube.pages.ExplorePage
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContentLoadState<ExplorePage>())
    internal val uiState = _uiState.asStateFlow()

    private suspend fun load() {
        try {
            YouTube
                .explore()
                .onSuccess { page ->
                    val artists: MutableMap<Int, String> = mutableMapOf()
                    val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                    database.allArtistsByPlayTime().first().let { list ->
                        var favIndex = 0
                        for ((artistsIndex, artist) in list.withIndex()) {
                            artists[artistsIndex] = artist.id
                            if (artist.artist.bookmarkedAt != null) {
                                favouriteArtists[favIndex] = artist.id
                                favIndex++
                            }
                        }
                    }
                    _uiState.value =
                        _uiState.value.loaded(
                            page.copy(
                                newReleaseAlbums =
                                    page.newReleaseAlbums
                                        .sortedBy { album ->
                                            val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                                            val firstArtistKey =
                                                artistIds.firstNotNullOfOrNull { artistId ->
                                                    if (artistId in favouriteArtists.values) {
                                                        favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                                    } else {
                                                        artists.entries.firstOrNull { it.value == artistId }?.key
                                                    }
                                                } ?: Int.MAX_VALUE
                                            firstArtistKey
                                        }.filterExplicit(context.dataStore.get(HideExplicitKey, false)),
                            ),
                        )
                }.onFailure(::handleFailure)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFailure(e)
        }
    }

    fun retry() {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.loading()

        viewModelScope.launch(Dispatchers.IO) {
            load()
        }
    }

    private fun handleFailure(error: Throwable) {
        if (error is CancellationException) throw error
        _uiState.value = _uiState.value.failed(error)
        reportException(error)
    }

    init {
        retry()
    }
}
