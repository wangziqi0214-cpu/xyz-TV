package com.ultrazg.xyztv.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.model.CategorySummary
import com.ultrazg.xyztv.data.model.DiscoverySection
import com.ultrazg.xyztv.data.model.EditorPickDay
import com.ultrazg.xyztv.data.model.Episode
import com.ultrazg.xyztv.data.repository.PodcastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repository = PodcastRepository()

    var sections by mutableStateOf<List<DiscoverySection>>(emptyList())
        private set

    var categories by mutableStateOf<List<CategorySummary>>(emptyList())
        private set

    var pilotEpisodes by mutableStateOf<List<Episode>>(emptyList())
        private set

    var todayEditorPick by mutableStateOf<EditorPickDay?>(null)
        private set

    var hotTopEpisodes by mutableStateOf<List<Episode>>(emptyList())
        private set

    var rockTopEpisodes by mutableStateOf<List<Episode>>(emptyList())
        private set

    var newTopEpisodes by mutableStateOf<List<Episode>>(emptyList())
        private set

    var profileName by mutableStateOf("")
        private set

    var profileAvatarUrl by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadHome()
    }

    fun loadHome() {
        if (isLoading) return
        AppLogger.info("home", "loadHome requested")
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                var primaryError: String? = null

                val sectionsDeferred = async {
                    runCatching { repository.getHomeSections() }.getOrElse { Result.failure(it) }
                }
                val categoriesDeferred = async {
                    runCatching { repository.getCategories() }.getOrElse { Result.failure(it) }
                }
                val pilotDeferred = async {
                    runCatching { repository.getPilotDiscoveryEpisodes() }.getOrElse { Result.failure(it) }
                }
                val editorDeferred = async {
                    runCatching { repository.getEditorPickHistory() }.getOrElse { Result.failure(it) }
                }
                val hotTopDeferred = async {
                    runCatching { repository.getTopList("HOT") }.getOrElse { Result.failure(it) }
                }
                val rockTopDeferred = async {
                    runCatching { repository.getTopList("ROCK") }.getOrElse { Result.failure(it) }
                }
                val newTopDeferred = async {
                    runCatching { repository.getTopList("NEW") }.getOrElse { Result.failure(it) }
                }
                val profileDeferred = async {
                    runCatching { repository.getProfile() }.getOrElse { Result.failure(it) }
                }

                sectionsDeferred.await()
                    .onSuccess { sections = it }
                    .onFailure {
                        primaryError = it.message
                        AppLogger.error("home", "sections load failed message=${it.message}")
                    }

                categoriesDeferred.await()
                    .onSuccess { categories = it }
                    .onFailure {
                        if (primaryError == null) {
                            primaryError = it.message
                        }
                        AppLogger.error("home", "categories load failed message=${it.message}")
                    }

                pilotDeferred.await()
                    .onSuccess { pilotEpisodes = it }
                    .onFailure { AppLogger.error("home", "pilot load failed message=${it.message}") }

                editorDeferred.await()
                    .onSuccess { todayEditorPick = it.firstOrNull() }
                    .onFailure { AppLogger.error("home", "editor pick load failed message=${it.message}") }

                hotTopDeferred.await()
                    .onSuccess { hotTopEpisodes = it }
                    .onFailure {
                        if (error == null) error = it.message
                        AppLogger.error("home", "hot top load failed message=${it.message}")
                    }

                rockTopDeferred.await()
                    .onSuccess { rockTopEpisodes = it }
                    .onFailure { AppLogger.error("home", "rock top load failed message=${it.message}") }

                newTopDeferred.await()
                    .onSuccess { newTopEpisodes = it }
                    .onFailure { AppLogger.error("home", "new top load failed message=${it.message}") }

                profileDeferred.await()
                    .onSuccess {
                        profileName = it.nickname
                        profileAvatarUrl = it.avatarUrl
                    }
                    .onFailure { AppLogger.error("home", "profile brief load failed message=${it.message}") }

                error = primaryError
            } catch (e: Exception) {
                error = e.message
                AppLogger.error("home", "loadHome crashed", e)
            } finally {
                isLoading = false
            }
        }
    }
}
