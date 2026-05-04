package com.ultrazg.xyztv.data.playback

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ultrazg.xyztv.data.model.Episode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AppPlaybackController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerStarted = false

    var player by mutableStateOf<ExoPlayer?>(null)
        private set

    var episode by mutableStateOf<Episode?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var currentPositionMs by mutableLongStateOf(0L)
        private set

    var durationMs by mutableLongStateOf(0L)
        private set

    fun playEpisode(context: Context, nextEpisode: Episode, resumePositionMs: Long): ExoPlayer {
        val appContext = context.applicationContext
        val existing = player
        if (existing != null && episode?.eid == nextEpisode.eid) {
            if (resumePositionMs > 0L && currentPositionMs <= 0L) {
                existing.seekTo(resumePositionMs)
            }
            existing.playWhenReady = true
            ensureTicker()
            return existing
        }

        existing?.release()
        episode = nextEpisode
        currentPositionMs = resumePositionMs.coerceAtLeast(0L)
        durationMs = 0L

        return ExoPlayer.Builder(appContext).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            nextEpisode.audioUrl?.let { url ->
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                if (resumePositionMs > 0L) seekTo(resumePositionMs)
                playWhenReady = true
            }
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    refreshFromPlayer(this@apply)
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    this@AppPlaybackController.isPlaying = playing
                }
            })
            this@AppPlaybackController.player = this
            ensureTicker()
        }
    }

    fun togglePlayPause() {
        player?.let { it.playWhenReady = !it.playWhenReady }
    }

    fun seekTo(positionMs: Long) {
        val active = player ?: return
        val target = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
        active.seekTo(target)
        currentPositionMs = target
    }

    fun stopPlayback() {
        player?.stop()
        isPlaying = false
        refresh()
    }

    fun release() {
        player?.release()
        player = null
        episode = null
        isPlaying = false
        currentPositionMs = 0L
        durationMs = 0L
    }

    fun refresh() {
        player?.let(::refreshFromPlayer)
    }

    private fun ensureTicker() {
        if (tickerStarted) return
        tickerStarted = true
        scope.launch {
            while (true) {
                refresh()
                delay(1_000)
            }
        }
    }

    private fun refreshFromPlayer(active: ExoPlayer) {
        currentPositionMs = active.currentPosition.coerceAtLeast(0L)
        val nextDuration = active.duration
        if (nextDuration != C.TIME_UNSET) {
            durationMs = nextDuration.coerceAtLeast(0L)
        }
        isPlaying = active.isPlaying
    }
}
