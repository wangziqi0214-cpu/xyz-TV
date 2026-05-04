package com.ultrazg.xyztv.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.model.Episode
import com.ultrazg.xyztv.data.repository.PodcastRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TopListViewModel : ViewModel() {

    private val repository = PodcastRepository()
    private var loadJob: Job? = null

    var category by mutableStateOf("HOT")
        private set

    var episodes by mutableStateOf<List<Episode>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun load(category: String) {
        if (isLoading && this.category == category) return
        if (!isLoading && this.category == category && episodes.isNotEmpty() && error == null) return
        this.category = category
        AppLogger.info("toplist", "load category=$category")
        val requestedCategory = category
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            error = null
            repository.getTopList(requestedCategory)
                .onSuccess {
                    if (this@TopListViewModel.category == requestedCategory) {
                        episodes = it
                    }
                }
                .onFailure {
                    if (this@TopListViewModel.category == requestedCategory) {
                        error = it.message
                    }
                }
            if (this@TopListViewModel.category == requestedCategory) {
                isLoading = false
            }
        }
    }
}
