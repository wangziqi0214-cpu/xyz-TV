package com.ultrazg.xyztv.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.model.ClapSummary
import com.ultrazg.xyztv.data.model.Episode
import com.ultrazg.xyztv.data.model.TranscriptSentence
import com.ultrazg.xyztv.data.repository.PodcastRepository
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class PlayerViewModel : ViewModel() {

    private val repository = PodcastRepository()
    private var lastReportedProgressSeconds = -1

    var episode by mutableStateOf<Episode?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var isUpdatingFavorite by mutableStateOf(false)
        private set

    var isSyncingPlayback by mutableStateOf(false)
        private set

    var isSubmittingClap by mutableStateOf(false)
        private set

    var playbackProgressSeconds by mutableIntStateOf(0)
        private set

    var clapSummary by mutableStateOf(ClapSummary())
        private set

    var transcriptSentences by mutableStateOf<List<TranscriptSentence>>(emptyList())
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    fun loadEpisode(eid: String) {
        if (isLoading && episode?.eid == eid) return
        AppLogger.info("player", "loadEpisode eid=$eid")
        viewModelScope.launch {
            isLoading = true
            error = null
            statusMessage = null
            episode = null
            playbackProgressSeconds = 0
            clapSummary = ClapSummary()
            transcriptSentences = emptyList()
            lastReportedProgressSeconds = -1
            try {

                repository.getEpisodeDetail(eid)
                    .onSuccess { loaded ->
                        episode = loaded
                        AppLogger.info("player", "loadEpisode success eid=$eid")
                        repository.markEpisodePlayed(eid)

                        val playbackDeferred = async {
                            runCatching { repository.getPlaybackProgress(eid) }.getOrElse { Result.failure(it) }
                        }
                        val clapDeferred = async {
                            runCatching { repository.getClapSummary(eid, loaded.duration.toInt()) }.getOrElse { Result.failure(it) }
                        }
                        val transcriptDeferred = async {
                            runCatching { repository.getEpisodeTranscript(eid) }.getOrElse { Result.failure(it) }
                        }

                        playbackDeferred.await()
                            .onSuccess { playbackProgressSeconds = it?.progress ?: 0 }
                            .onFailure {
                                AppLogger.error("player", "load playback progress failed eid=$eid message=${it.message}")
                            }

                        clapDeferred.await()
                            .onSuccess { clapSummary = it }
                            .onFailure {
                                AppLogger.error("player", "load clap summary failed eid=$eid message=${it.message}")
                            }

                        transcriptDeferred.await()
                            .onSuccess { transcriptSentences = it }
                            .onFailure {
                                AppLogger.info("player", "transcript unavailable eid=$eid message=${it.message}")
                            }
                    }
                    .onFailure {
                        error = it.message
                        AppLogger.error("player", "loadEpisode failure eid=$eid message=${it.message}")
                    }

            } catch (e: Exception) {
                error = e.message
                AppLogger.error("player", "loadEpisode crashed eid=$eid", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleFavorite() {
        val current = episode ?: return
        isUpdatingFavorite = true
        viewModelScope.launch {
            repository.updateFavoriteEpisode(current.eid, !current.isFavorited)
                .onSuccess {
                    episode = current.copy(isFavorited = !current.isFavorited)
                    statusMessage = if (!current.isFavorited) "已收藏单集" else "已取消收藏"
                }
                .onFailure { error = it.message }
            isUpdatingFavorite = false
        }
    }

    fun createClap(positionMs: Long) {
        val current = episode ?: return
        val timestampSeconds = (positionMs / 1000L).toInt().coerceAtLeast(0)
        isSubmittingClap = true
        viewModelScope.launch {
            repository.createClap(
                eid = current.eid,
                timestamp = timestampSeconds.toLong(),
                duration = current.duration
            ).onSuccess {
                statusMessage = "已标记高能点"
                repository.getClapSummary(current.eid, current.duration.toInt())
                    .onSuccess { clapSummary = it }
                    .onFailure { AppLogger.error("player", "refresh clap summary failed eid=${current.eid} message=${it.message}") }
            }.onFailure {
                error = it.message
            }
            isSubmittingClap = false
        }
    }

    fun reportPlaybackSession(startPlayingTimestampMs: Long, endPlayingTimestampMs: Long, progressMs: Long) {
        val current = episode ?: return
        val progressSeconds = (progressMs / 1000L).toInt().coerceAtLeast(0)
        if (progressSeconds <= 0 && endPlayingTimestampMs <= startPlayingTimestampMs) return
        if (progressSeconds == lastReportedProgressSeconds) return
        lastReportedProgressSeconds = progressSeconds

        isSyncingPlayback = true
        viewModelScope.launch {
            val playedAt = Instant.ofEpochMilli(endPlayingTimestampMs).toString()
            val progressResult = repository.updatePlaybackProgress(
                eid = current.eid,
                pid = current.pid,
                progress = progressSeconds,
                playedAt = playedAt
            )
            val mileageResult = repository.updateMileage(
                eid = current.eid,
                pid = current.pid,
                startPlayingTimestamp = startPlayingTimestampMs.toDouble(),
                endPlayingTimestamp = endPlayingTimestampMs.toDouble()
            )

            progressResult.onFailure {
                error = it.message
            }
            mileageResult.onFailure {
                if (error == null) error = it.message
            }

            if (progressResult.isSuccess && mileageResult.isSuccess) {
                playbackProgressSeconds = progressSeconds
                statusMessage = "已同步播放进度"
            }
            isSyncingPlayback = false
        }
    }
}
