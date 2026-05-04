package com.ultrazg.xyztv.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.model.EditorPickDay
import com.ultrazg.xyztv.data.model.Episode
import com.ultrazg.xyztv.data.model.UserPick
import com.ultrazg.xyztv.data.repository.PodcastRepository
import kotlinx.coroutines.launch

class FeatureFeedViewModel : ViewModel() {

    private val repository = PodcastRepository()

    var episodes by mutableStateOf<List<Episode>>(emptyList())
        private set

    var editorPickDays by mutableStateOf<List<EditorPickDay>>(emptyList())
        private set

    var picks by mutableStateOf<List<UserPick>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun loadInbox() {
        AppLogger.info("feature_feed", "loadInbox")
        viewModelScope.launch {
            reset()
            repository.getInboxEpisodes()
                .onSuccess { episodes = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadPilotDiscovery() {
        AppLogger.info("feature_feed", "loadPilotDiscovery")
        viewModelScope.launch {
            reset()
            repository.getPilotDiscoveryEpisodes()
                .onSuccess { episodes = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadEditorPickHistory() {
        AppLogger.info("feature_feed", "loadEditorPickHistory")
        viewModelScope.launch {
            reset()
            repository.getEditorPickHistory()
                .onSuccess { editorPickDays = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadPickHistory() {
        AppLogger.info("feature_feed", "loadPickHistory")
        viewModelScope.launch {
            reset()
            val profileResult = repository.getProfile()
            val uid = profileResult.getOrNull()?.uid.orEmpty()
            if (uid.isBlank()) {
                error = profileResult.exceptionOrNull()?.message ?: "Missing profile uid"
                isLoading = false
                return@launch
            }

            repository.getPickHistory(uid)
                .onSuccess { picks = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    private fun reset() {
        isLoading = true
        error = null
        episodes = emptyList()
        editorPickDays = emptyList()
        picks = emptyList()
    }
}
