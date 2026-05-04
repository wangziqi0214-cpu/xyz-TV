package com.ultrazg.xyztv.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.model.Episode
import com.ultrazg.xyztv.data.model.Podcast
import com.ultrazg.xyztv.data.model.PodcastBulletin
import com.ultrazg.xyztv.data.model.PodcastHonor
import com.ultrazg.xyztv.data.model.PodcastOwnerInfo
import com.ultrazg.xyztv.data.repository.PodcastRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class PodcastDetailViewModel : ViewModel() {

    private val repository = PodcastRepository()

    var podcast by mutableStateOf<Podcast?>(null)
        private set

    var episodes by mutableStateOf<List<Episode>>(emptyList())
        private set

    var popularEpisodes by mutableStateOf<List<Episode>>(emptyList())
        private set

    var relatedPodcasts by mutableStateOf<List<Podcast>>(emptyList())
        private set

    var bulletin by mutableStateOf<PodcastBulletin?>(null)
        private set

    var ownerInfo by mutableStateOf<PodcastOwnerInfo?>(null)
        private set

    var honors by mutableStateOf<List<PodcastHonor>>(emptyList())
        private set

    var isUpdatingSubscription by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun loadPodcast(pid: String) {
        if (isLoading) return
        AppLogger.info("podcast", "loadPodcast pid=$pid")
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                podcast = null
                episodes = emptyList()
                popularEpisodes = emptyList()
                relatedPodcasts = emptyList()
                bulletin = null
                ownerInfo = null
                honors = emptyList()

                repository.getPodcastDetail(pid)
                    .onSuccess {
                        podcast = it
                        AppLogger.info("podcast", "podcast detail success pid=$pid")
                    }
                    .onFailure {
                        error = it.message
                        AppLogger.error("podcast", "podcast detail failure pid=$pid message=${it.message}")
                    }

                val episodesDeferred = async {
                    runCatching { repository.getEpisodeList(pid) }.getOrElse { Result.failure(it) }
                }
                val popularDeferred = async {
                    runCatching { repository.getPopularEpisodes(pid) }.getOrElse { Result.failure(it) }
                }
                val relatedDeferred = async {
                    runCatching { repository.getRelatedPodcasts(pid) }.getOrElse { Result.failure(it) }
                }
                val bulletinDeferred = async {
                    runCatching { repository.getPodcastBulletin(pid) }.getOrElse { Result.failure(it) }
                }
                val infoDeferred = async {
                    runCatching { repository.getPodcastInfo(pid) }.getOrElse { Result.failure(it) }
                }
                val honorDeferred = async {
                    runCatching { repository.getPodcastHonorList(pid) }.getOrElse { Result.failure(it) }
                }

                episodesDeferred.await().onSuccess {
                    episodes = it
                    AppLogger.info("podcast", "episode list success pid=$pid count=${it.size}")
                }.onFailure {
                    if (error == null) error = it.message
                    AppLogger.error("podcast", "episode list failure pid=$pid message=${it.message}")
                }

                popularDeferred.await().onSuccess {
                    popularEpisodes = it
                }.onFailure {
                    AppLogger.info("podcast", "popular episodes unavailable pid=$pid message=${it.message}")
                }

                relatedDeferred.await().onSuccess {
                    relatedPodcasts = it
                }.onFailure {
                    if (error == null) error = it.message
                }

                bulletinDeferred.await().onSuccess {
                    bulletin = it
                }.onFailure {
                    if (error == null) error = it.message
                }

                infoDeferred.await().onSuccess {
                    ownerInfo = it
                }.onFailure {
                    AppLogger.error("podcast", "podcast info failure pid=$pid message=${it.message}")
                }

                honorDeferred.await().onSuccess {
                    honors = it
                }.onFailure {
                    AppLogger.error("podcast", "podcast honor failure pid=$pid message=${it.message}")
                }
            } catch (e: Exception) {
                error = e.message
                AppLogger.error("podcast", "loadPodcast crashed pid=$pid", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleSubscription() {
        val current = podcast ?: return
        val newMode = if (current.subscriptionStatus == "ON") "OFF" else "ON"
        isUpdatingSubscription = true
        viewModelScope.launch {
            repository.updateSubscription(current.pid, newMode)
                .onSuccess {
                    podcast = current.copy(subscriptionStatus = newMode)
                }
                .onFailure {
                    error = it.message
                }
            isUpdatingSubscription = false
        }
    }
}
