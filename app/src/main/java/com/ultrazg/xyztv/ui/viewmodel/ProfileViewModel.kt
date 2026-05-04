package com.ultrazg.xyztv.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.model.Podcast
import com.ultrazg.xyztv.data.model.UserPick
import com.ultrazg.xyztv.data.model.UserProfile
import com.ultrazg.xyztv.data.model.UserStats
import com.ultrazg.xyztv.data.repository.PodcastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = PodcastRepository()

    var profile by mutableStateOf<UserProfile?>(null)
        private set

    var stats by mutableStateOf<UserStats?>(null)
        private set

    var unreadCount by mutableStateOf(0)
        private set

    var ownedPodcasts by mutableStateOf<List<Podcast>>(emptyList())
        private set

    var recentPicks by mutableStateOf<List<UserPick>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun load() {
        AppLogger.info("profile", "load profile")
        viewModelScope.launch {
            isLoading = true
            error = null

            val profileResult = repository.getProfile()
            profileResult.onSuccess { profile = it }
                .onFailure { error = it.message }

            val uid = profile?.uid.orEmpty()
            if (uid.isNotBlank()) {
                val statsDeferred = async { repository.getUserStats(uid) }
                val ownedDeferred = async { repository.getOwnedPodcasts(uid) }
                val picksDeferred = async { repository.getRecentPicks(uid) }
                statsDeferred.await().onSuccess { stats = it }.onFailure { if (error == null) error = it.message }
                ownedDeferred.await().onSuccess { ownedPodcasts = it }.onFailure { if (error == null) error = it.message }
                picksDeferred.await().onSuccess { recentPicks = it }.onFailure { if (error == null) error = it.message }
            }

            repository.getUnreadCount()
                .onSuccess { unreadCount = it }
                .onFailure { if (error == null) error = it.message }

            isLoading = false
        }
    }
}
