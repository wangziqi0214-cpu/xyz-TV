package com.ultrazg.xyztv.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.model.MileageEntry
import com.ultrazg.xyztv.data.model.MileageOverview
import com.ultrazg.xyztv.data.model.CommentItem
import com.ultrazg.xyztv.data.model.Podcast
import com.ultrazg.xyztv.data.model.StickerBoardItem
import com.ultrazg.xyztv.data.model.StickerItem
import com.ultrazg.xyztv.data.model.UserLite
import com.ultrazg.xyztv.data.model.UserPreference
import com.ultrazg.xyztv.data.repository.PodcastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class SocialViewModel : ViewModel() {

    private val repository = PodcastRepository()

    var podcasts by mutableStateOf<List<Podcast>>(emptyList())
        private set

    var users by mutableStateOf<List<UserLite>>(emptyList())
        private set

    var collectedComments by mutableStateOf<List<CommentItem>>(emptyList())
        private set

    var stickers by mutableStateOf<List<StickerItem>>(emptyList())
        private set

    var stickerBoard by mutableStateOf<List<StickerBoardItem>>(emptyList())
        private set

    var mileageOverview by mutableStateOf<MileageOverview?>(null)
        private set

    var mileageEntries by mutableStateOf<List<MileageEntry>>(emptyList())
        private set

    var preference by mutableStateOf<UserPreference?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun loadStarSubscriptions() {
        AppLogger.info("social", "loadStarSubscriptions")
        viewModelScope.launch {
            reset()
            repository.getStarSubscriptions()
                .onSuccess { podcasts = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadNonStarredSubscriptions() {
        AppLogger.info("social", "loadNonStarredSubscriptions")
        viewModelScope.launch {
            reset()
            repository.getNonStarredSubscriptions()
                .onSuccess { podcasts = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadFollowing() {
        AppLogger.info("social", "loadFollowing")
        viewModelScope.launch {
            reset()
            val uid = repository.getProfile().getOrNull()?.uid.orEmpty()
            if (uid.isBlank()) {
                error = "Missing profile uid"
                isLoading = false
                return@launch
            }
            repository.getFollowing(uid)
                .onSuccess { users = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadFollowers() {
        AppLogger.info("social", "loadFollowers")
        viewModelScope.launch {
            reset()
            val uid = repository.getProfile().getOrNull()?.uid.orEmpty()
            if (uid.isBlank()) {
                error = "Missing profile uid"
                isLoading = false
                return@launch
            }
            repository.getFollowers(uid)
                .onSuccess { users = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadPreferences() {
        AppLogger.info("social", "loadPreferences")
        viewModelScope.launch {
            reset()
            repository.getUserPreference()
                .onSuccess { preference = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadCollectedComments() {
        AppLogger.info("social", "loadCollectedComments")
        viewModelScope.launch {
            reset()
            repository.getCollectedComments()
                .onSuccess { collectedComments = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadBlockedUsers() {
        AppLogger.info("social", "loadBlockedUsers")
        viewModelScope.launch {
            reset()
            repository.getBlockedUsers()
                .onSuccess { users = it }
                .onFailure { error = it.message }
            isLoading = false
        }
    }

    fun loadMileage(all: Boolean) {
        AppLogger.info("social", "loadMileage all=$all")
        viewModelScope.launch {
            reset()
            val overviewDeferred = async { repository.getMileageOverview() }
            val listDeferred = async { repository.getMileageList(all) }
            overviewDeferred.await().onSuccess { mileageOverview = it }.onFailure { error = it.message }
            listDeferred.await().onSuccess { mileageEntries = it }.onFailure { if (error == null) error = it.message }
            isLoading = false
        }
    }

    fun loadStickers() {
        AppLogger.info("social", "loadStickers")
        viewModelScope.launch {
            reset()
            val uid = repository.getProfile().getOrNull()?.uid.orEmpty()
            if (uid.isBlank()) {
                error = "Missing profile uid"
                isLoading = false
                return@launch
            }
            val stickerDeferred = async { repository.getStickers(uid) }
            val boardDeferred = async { repository.getStickerBoard(uid) }
            stickerDeferred.await().onSuccess { stickers = it }.onFailure { error = it.message }
            boardDeferred.await().onSuccess { stickerBoard = it }.onFailure { if (error == null) error = it.message }
            isLoading = false
        }
    }

    fun toggleRelation(user: UserLite) {
        val newRelation = if (user.relation == "FOLLOWING") "STRANGE" else "FOLLOWING"
        viewModelScope.launch {
            repository.updateRelation(user.uid, newRelation)
                .onSuccess {
                    users = users.map {
                        if (it.uid == user.uid) it.copy(relation = if (user.relation == "FOLLOWING") "STRANGE" else "FOLLOWING")
                        else it
                    }
                }
                .onFailure { error = it.message }
        }
    }

    fun togglePreference(key: String, current: Boolean) {
        viewModelScope.launch {
            repository.updateUserPreference(key, !current)
                .onSuccess {
                    repository.getUserPreference().onSuccess { preference = it }
                }
                .onFailure { error = it.message }
        }
    }

    fun toggleStarSubscription(podcast: Podcast) {
        viewModelScope.launch {
            val withStar = !podcast.subscriptionStar
            repository.updateStarSubscription(podcast.pid, withStar)
                .onSuccess {
                    podcasts = podcasts.map {
                        if (it.pid == podcast.pid) it.copy(subscriptionStar = withStar) else it
                    }
                }
                .onFailure { error = it.message }
        }
    }

    fun toggleBlockedUser(user: UserLite) {
        val newBlocked = !user.isBlockedByViewer
        viewModelScope.launch {
            repository.updateBlockedUser(user.uid, newBlocked)
                .onSuccess {
                    users = users.filterNot { it.uid == user.uid }
                }
                .onFailure { error = it.message }
        }
    }

    private fun reset() {
        isLoading = true
        error = null
        podcasts = emptyList()
        users = emptyList()
        collectedComments = emptyList()
        stickers = emptyList()
        stickerBoard = emptyList()
        mileageOverview = null
        mileageEntries = emptyList()
        preference = null
    }
}
