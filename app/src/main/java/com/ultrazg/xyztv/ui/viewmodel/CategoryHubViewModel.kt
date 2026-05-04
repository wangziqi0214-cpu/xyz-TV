package com.ultrazg.xyztv.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrazg.xyztv.data.model.CategorySummary
import com.ultrazg.xyztv.data.repository.PodcastRepository
import kotlinx.coroutines.launch

class CategoryHubViewModel : ViewModel() {

    private val repository = PodcastRepository()

    var categories by mutableStateOf<List<CategorySummary>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            error = null
            repository.getCategories()
                .onSuccess { categories = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }
}
