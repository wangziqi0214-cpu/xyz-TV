package com.ultrazg.xyztv.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.model.Episode
import com.ultrazg.xyztv.data.model.Podcast
import com.ultrazg.xyztv.data.repository.PodcastRepository
import kotlinx.coroutines.launch

class LibraryViewModel : ViewModel() {

    private val repository = PodcastRepository()

    var podcasts by mutableStateOf<List<Podcast>>(emptyList())
        private set

    var episodes by mutableStateOf<List<Episode>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun loadSubscriptions() {
        AppLogger.info("library", "loadSubscriptions")
        viewModelScope.launch {
            isLoading = true
            error = null
            podcasts = emptyList()
            episodes = emptyList()
            repository.getSubscriptions()
                .onSuccess { podcasts = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadFavorites() {
        AppLogger.info("library", "loadFavorites")
        viewModelScope.launch {
            isLoading = true
            error = null
            podcasts = emptyList()
            episodes = emptyList()
            repository.getFavoriteEpisodes()
                .onSuccess { episodes = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadHistory() {
        AppLogger.info("library", "loadHistory")
        viewModelScope.launch {
            isLoading = true
            error = null
            podcasts = emptyList()
            episodes = emptyList()
            repository.getPlayedHistory()
                .onSuccess { episodes = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }
}
