package com.ultrazg.xyztv.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.model.SearchPreset
import com.ultrazg.xyztv.data.model.SearchItem
import com.ultrazg.xyztv.data.repository.PodcastRepository
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {

    private val repository = PodcastRepository()

    var query by mutableStateOf("")
        private set

    var searchType by mutableStateOf("PODCAST")
        private set

    var results by mutableStateOf<List<SearchItem>>(emptyList())
        private set

    var presets by mutableStateOf<List<SearchPreset>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadPresets()
    }

    fun onQueryChange(newQuery: String) {
        query = newQuery
        AppLogger.action("search_query", "length=${query.length}")
    }

    fun selectSearchType(type: String) {
        if (type == searchType) return
        searchType = type
        AppLogger.action("search_type", type)
    }

    fun search() {
        if (query.isBlank()) {
            AppLogger.warn("search", "search ignored because query is blank")
            return
        }
        AppLogger.info("search", "search requested query=${query.take(40)} type=$searchType")
        viewModelScope.launch {
            isLoading = true
            error = null
            val result = repository.search(query, searchType)
            result.onSuccess { response ->
                results = response.data ?: emptyList()
                AppLogger.info("search", "search success results=${results.size} type=$searchType")
            }.onFailure { e ->
                error = e.message
                AppLogger.error("search", "search failure: ${e.message}")
            }
            isLoading = false
        }
    }

    private fun loadPresets() {
        viewModelScope.launch {
            repository.getSearchPresets()
                .onSuccess { presets = it }
                .onFailure { AppLogger.warn("search", "preset load failed: ${it.message}") }
        }
    }
}
