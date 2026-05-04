package com.ultrazg.xyztv.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.model.CategoryTab
import com.ultrazg.xyztv.data.model.Podcast
import com.ultrazg.xyztv.data.repository.PodcastRepository
import kotlinx.coroutines.launch

class CategoryViewModel : ViewModel() {

    private val repository = PodcastRepository()

    var tabs by mutableStateOf<List<CategoryTab>>(emptyList())
        private set

    var selectedTab by mutableStateOf<String?>(null)
        private set

    var podcasts by mutableStateOf<List<Podcast>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var currentCategoryId: String? = null

    fun load(categoryId: String) {
        if (currentCategoryId == categoryId && (tabs.isNotEmpty() || isLoading)) return
        currentCategoryId = categoryId
        AppLogger.info("category", "load categoryId=$categoryId")
        viewModelScope.launch {
            isLoading = true
            error = null
            repository.getCategoryTabs(categoryId)
                .onSuccess { loadedTabs ->
                    tabs = loadedTabs
                    selectedTab = loadedTabs.firstOrNull()?.value
                    AppLogger.info("category", "tabs loaded count=${loadedTabs.size}")
                    val firstTab = selectedTab
                    if (!firstTab.isNullOrBlank()) {
                        loadPodcasts(categoryId, firstTab)
                    } else {
                        podcasts = emptyList()
                    }
                }
                .onFailure { e ->
                    error = e.message
                    AppLogger.error("category", "load tabs failure: ${e.message}")
                    isLoading = false
                }
        }
    }

    fun selectTab(categoryId: String, tab: String) {
        if (tab == selectedTab) return
        selectedTab = tab
        loadPodcasts(categoryId, tab)
    }

    private fun loadPodcasts(categoryId: String, tab: String) {
        AppLogger.info("category", "load podcasts categoryId=$categoryId tab=$tab")
        viewModelScope.launch {
            isLoading = true
            repository.getCategoryPodcasts(categoryId, tab)
                .onSuccess { loaded ->
                    podcasts = loaded
                    AppLogger.info("category", "podcasts loaded count=${loaded.size}")
                }
                .onFailure { e ->
                    error = e.message
                    AppLogger.error("category", "load podcasts failure: ${e.message}")
                }
            isLoading = false
        }
    }
}
