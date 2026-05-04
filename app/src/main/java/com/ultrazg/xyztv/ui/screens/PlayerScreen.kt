package com.ultrazg.xyztv.ui.screens

import android.content.Context
import androidx.annotation.OptIn as AndroidXOptIn
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ultrazg.xyztv.data.playback.AppPlaybackController
import com.ultrazg.xyztv.data.model.ClapSummary
import com.ultrazg.xyztv.data.model.CommentItem
import com.ultrazg.xyztv.data.model.Episode
import com.ultrazg.xyztv.data.model.TranscriptSentence
import com.ultrazg.xyztv.ui.viewmodel.CommentViewModel
import com.ultrazg.xyztv.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

private val PlaybackSpeeds = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f)

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    eid: String,
    onBack: () -> Unit,
    onOpenDetail: (eid: String) -> Unit = {},
    onOpenComments: (eid: String) -> Unit = {}
) {
    val viewModel: PlayerViewModel = viewModel()
    val commentViewModel: CommentViewModel = viewModel()
    val episode = viewModel.episode
    val context = LocalContext.current

    LaunchedEffect(eid) {
        viewModel.loadEpisode(eid)
    }

    when {
        viewModel.error != null && episode == null -> CenterMessage("加载失败：${viewModel.error}")
        viewModel.isLoading || episode == null -> CenterMessage("正在加载单集...")
        else -> PlayerContent(
            episode = episode,
            context = context,
            onBack = onBack,
            onOpenDetail = onOpenDetail,
            onToggleFavorite = { viewModel.toggleFavorite() },
            isUpdatingFavorite = viewModel.isUpdatingFavorite,
            initialProgressSeconds = viewModel.playbackProgressSeconds,
            clapSummary = viewModel.clapSummary,
            transcriptSentences = viewModel.transcriptSentences,
            statusMessage = viewModel.statusMessage ?: viewModel.error,
            isSyncingPlayback = viewModel.isSyncingPlayback,
            isSubmittingClap = viewModel.isSubmittingClap,
            commentViewModel = commentViewModel,
            onCreateClap = viewModel::createClap,
            onReportPlayback = viewModel::reportPlaybackSession
        )
    }
}

@AndroidXOptIn(UnstableApi::class)
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerContent(
    episode: Episode,
    context: Context,
    onBack: () -> Unit,
    onOpenDetail: (eid: String) -> Unit,
    onToggleFavorite: () -> Unit,
    isUpdatingFavorite: Boolean,
    initialProgressSeconds: Int,
    clapSummary: ClapSummary,
    transcriptSentences: List<TranscriptSentence>,
    statusMessage: String?,
    isSyncingPlayback: Boolean,
    isSubmittingClap: Boolean,
    commentViewModel: CommentViewModel,
    onCreateClap: (Long) -> Unit,
    onReportPlayback: (Long, Long, Long) -> Unit
) {
    var hasReportedSession by remember { mutableStateOf(false) }
    var sessionStartedAtMs by remember { mutableLongStateOf(0L) }
    var commentsOpen by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var speedOptionsOpen by remember { mutableStateOf(false) }
    val currentPosition = AppPlaybackController.currentPositionMs
    val duration = AppPlaybackController.durationMs
    val isPlaying = AppPlaybackController.isPlaying

    val exoPlayer = remember(episode.eid) {
        AppPlaybackController.playEpisode(
            context = context,
            nextEpisode = episode,
            resumePositionMs = initialProgressSeconds * 1000L
        )
    }

    fun reportSessionIfNeeded() {
        if (hasReportedSession) return
        hasReportedSession = true
        val end = System.currentTimeMillis()
        val started = if (sessionStartedAtMs == 0L) end else sessionStartedAtMs
        val position = maxOf(currentPosition, exoPlayer.currentPosition)
        onReportPlayback(started, end, position)
    }

    fun seekTo(positionMs: Long) {
        AppPlaybackController.seekTo(positionMs)
    }

    DisposableEffect(episode.eid) {
        onDispose {
            reportSessionIfNeeded()
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying && sessionStartedAtMs == 0L) {
            sessionStartedAtMs = System.currentTimeMillis()
        }
    }

    LaunchedEffect(exoPlayer, playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    LaunchedEffect(commentsOpen, episode.eid) {
        if (commentsOpen && commentViewModel.comments.isEmpty()) {
            commentViewModel.loadPrimaryComments(episode.eid)
        }
    }

    val clapPointCount = clapSummary.episodeClaps.count { it.count > 0 }
    val clapCount = clapSummary.episodeClaps.sumOf { it.count }
    val myClapCount = clapSummary.myClaps.size

    BackHandler(enabled = commentsOpen) {
        commentsOpen = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 30.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    ControlButton(icon = "‹") {
                        reportSessionIfNeeded()
                        onBack()
                    }
                    statusMessage?.takeIf { it.isNotBlank() }?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = episode.bestCoverUrl ?: "",
                        contentDescription = episode.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = episode.podcast?.title.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = episode.title,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 22.sp,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "高能点 $clapPointCount 段 · 总标记 $clapCount · 我的标记 $myClapCount",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CurrentTranscriptText(
                    currentPosition = currentPosition,
                    sentences = transcriptSentences
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PlaybackProgressBar(
                    currentPosition = currentPosition,
                    duration = duration,
                    clapSummary = clapSummary,
                    onSeek = ::seekTo
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ControlTextButton(text = "返回详情", width = 92.dp, height = 42.dp) {
                            reportSessionIfNeeded()
                            onOpenDetail(episode.eid)
                        }
                        ControlButton(icon = "☰", size = 38.dp, iconSize = 16.sp) {
                            val opening = !commentsOpen
                            commentsOpen = opening
                            if (opening) speedOptionsOpen = false
                        }
                        ControlButton(
                            icon = if (isUpdatingFavorite) "…" else if (episode.isFavorited) "★" else "☆",
                            size = 38.dp,
                            iconSize = 16.sp
                        ) { onToggleFavorite() }
                    }

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ControlButton(icon = "↶", size = 56.dp, iconSize = 24.sp) {
                            seekTo(AppPlaybackController.currentPositionMs - 30_000L)
                        }
                        ControlButton(icon = if (isPlaying) "Ⅱ" else "▶", size = 68.dp, iconSize = 30.sp) {
                            AppPlaybackController.togglePlayPause()
                        }
                        ControlButton(icon = "↷", size = 56.dp, iconSize = 24.sp) {
                            seekTo(AppPlaybackController.currentPositionMs + 30_000L)
                        }
                    }

                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpeedMenuButton(
                            speed = playbackSpeed,
                            expanded = speedOptionsOpen,
                            onClick = {
                                val opening = !speedOptionsOpen
                                speedOptionsOpen = opening
                                if (opening) commentsOpen = false
                            }
                        )
                        ControlButton(icon = if (isSubmittingClap) "◇" else "◆", size = 38.dp, iconSize = 16.sp) {
                            onCreateClap(maxOf(currentPosition, AppPlaybackController.currentPositionMs))
                        }
                        ControlButton(icon = if (isSyncingPlayback) "□" else "■", size = 38.dp, iconSize = 16.sp) {
                            AppPlaybackController.stopPlayback()
                            reportSessionIfNeeded()
                            onBack()
                        }
                    }
                }

                if (speedOptionsOpen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaybackSpeeds.forEach { speed ->
                            SpeedButton(
                                speed = speed,
                                selected = playbackSpeed == speed,
                                onClick = {
                                    playbackSpeed = speed
                                    speedOptionsOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (commentsOpen) {
            Dialog(
                onDismissRequest = { commentsOpen = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnClickOutside = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.56f))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(440.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.Black.copy(alpha = 0.34f))
                            .padding(8.dp)
                    ) {
                        CommentSidePanel(
                            eid = episode.eid,
                            viewModel = commentViewModel,
                            onClose = { commentsOpen = false }
                        )
                    }
                }
            }
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    visibility = android.view.View.GONE
                }
            },
            onRelease = { playerView ->
                playerView.player = null
            },
            modifier = Modifier.size(1.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaybackProgressBar(
    currentPosition: Long,
    duration: Long,
    clapSummary: ClapSummary,
    onSeek: (Long) -> Unit
) {
    val progress = if (duration > 0L) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    val clapMarkers = clapSummary.episodeClaps.mapIndexedNotNull { index, bucket ->
        if (bucket.count <= 0 || clapSummary.episodeClaps.isEmpty()) null
        else ((index + 0.5f) / clapSummary.episodeClaps.size.toFloat()) to bucket.count
    }
    val myMarkers = clapSummary.myClaps.mapNotNull { index ->
        if (clapSummary.episodeClaps.isEmpty()) null
        else ((index + 0.5f) / clapSummary.episodeClaps.size.toFloat()).coerceIn(0f, 1f)
    }

    Surface(
        onClick = { },
        modifier = Modifier
            .fillMaxWidth()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || duration <= 0L) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onSeek(currentPosition - 10_000L)
                        true
                    }
                    Key.DirectionRight -> {
                        onSeek(currentPosition + 10_000L)
                        true
                    }
                    else -> false
                }
            }
            .focusable(),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(currentPosition), color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp)
                Text(text = formatTime(duration), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            }
            val canvasTrackColor = MaterialTheme.colorScheme.onSurface
            val canvasProgressColor = MaterialTheme.colorScheme.primary
            val canvasMyMarkerColor = MaterialTheme.colorScheme.onBackground
            val canvasThumbColor = MaterialTheme.colorScheme.onBackground
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
            ) {
                val trackHeight = 8.dp.toPx()
                val trackTop = (size.height - trackHeight) / 2f
                val radius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                drawRoundRect(
                    color = canvasTrackColor,
                    topLeft = Offset(0f, trackTop),
                    size = Size(size.width, trackHeight),
                    cornerRadius = radius
                )
                drawRoundRect(
                    color = canvasProgressColor,
                    topLeft = Offset(0f, trackTop),
                    size = Size(size.width * progress, trackHeight),
                    cornerRadius = radius
                )
                clapMarkers.forEach { (position, count) ->
                    val x = size.width * position.coerceIn(0f, 1f)
                    val markerHeight = (12.dp + (count.coerceAtMost(8) * 1).dp).toPx()
                    drawRoundRect(
                        color = canvasProgressColor,
                        topLeft = Offset(x - 2.dp.toPx(), (size.height - markerHeight) / 2f),
                        size = Size(4.dp.toPx(), markerHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
                myMarkers.forEach { position ->
                    drawCircle(
                        color = canvasMyMarkerColor,
                        radius = 4.dp.toPx(),
                        center = Offset(size.width * position, size.height / 2f)
                    )
                }
                drawCircle(
                    color = canvasThumbColor,
                    radius = 7.dp.toPx(),
                    center = Offset(size.width * progress, size.height / 2f)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CurrentTranscriptText(
    currentPosition: Long,
    sentences: List<TranscriptSentence>
) {
    if (sentences.isEmpty()) return
    val active = sentences.lastOrNull { sentence ->
        val start = sentence.startMs ?: return@lastOrNull false
        val end = sentence.endMs ?: Long.MAX_VALUE
        currentPosition >= start && currentPosition < end
    }
    active?.text?.takeIf { it.isNotBlank() }?.let { text ->
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 30.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CommentSidePanel(
    eid: String,
    viewModel: CommentViewModel,
    onClose: () -> Unit
) {
    val commentsScrollState = rememberScrollState()
    val commentsFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(eid) {
        commentsFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .width(420.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(text = "评论", color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(commentsFocusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        if (event.key == Key.Back || event.key == Key.Escape) {
                            onClose()
                            return@onPreviewKeyEvent true
                        }
                        false
                    }
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (event.key) {
                            Key.DirectionDown -> {
                                coroutineScope.launch {
                                    commentsScrollState.animateScrollTo(
                                        (commentsScrollState.value + 180).coerceAtMost(commentsScrollState.maxValue)
                                    )
                                }
                                true
                            }
                            Key.DirectionUp -> {
                                coroutineScope.launch {
                                    commentsScrollState.animateScrollTo(
                                        (commentsScrollState.value - 180).coerceAtLeast(0)
                                    )
                                }
                                true
                            }
                            else -> false
                        }
                    }
                    .focusable()
                    .verticalScroll(commentsScrollState)
                    .padding(end = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    viewModel.isLoading -> CenterMessage("正在加载评论...")
                    viewModel.error != null -> FeatureMessageCard("评论加载失败：${viewModel.error}", MaterialTheme.colorScheme.primary)
                    viewModel.comments.isEmpty() -> FeatureMessageCard("还没有评论", MaterialTheme.colorScheme.onSurface)
                    else -> viewModel.comments.forEach { comment ->
                        PlayerCommentCard(
                            comment = comment,
                            canDelete = viewModel.canDelete(comment),
                            onLike = { viewModel.toggleLike(comment) },
                            onCollect = { viewModel.toggleCollect(comment) },
                            onDelete = { viewModel.removeComment(comment) { viewModel.loadPrimaryComments(eid) } }
                        )
                    }
                }
            }
            PlayerCommentScrollBar(
                scrollState = commentsScrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = viewModel.inputText,
                    onValueChange = viewModel::onInputChange,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth()
                ) { inner ->
                    if (viewModel.inputText.isBlank()) {
                        Text(text = "输入评论...", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    }
                    inner()
                }
            }
            MiniPlayerButton(if (viewModel.isSubmitting) "发送中" else "发送") {
                viewModel.submitComment(eid) { viewModel.loadPrimaryComments(eid) }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerCommentScrollBar(
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.onSurface
    val thumbColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.width(6.dp)) {
        val radius = CornerRadius(size.width / 2f, size.width / 2f)
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = Size(size.width, size.height),
            cornerRadius = radius
        )
        val max = scrollState.maxValue
        val viewportRatio = if (max <= 0) 1f else (size.height / (size.height + max)).coerceIn(0.12f, 1f)
        val thumbHeight = size.height * viewportRatio
        val thumbTop = if (max <= 0) 0f else (scrollState.value.toFloat() / max.toFloat()) * (size.height - thumbHeight)
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(0f, thumbTop),
            size = Size(size.width, thumbHeight),
            cornerRadius = radius
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlayerCommentCard(
    comment: CommentItem,
    canDelete: Boolean,
    onLike: () -> Unit,
    onCollect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = {},
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.author?.nickname.orEmpty(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = comment.createdAt?.take(10).orEmpty(), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
            }
            Text(
                text = comment.text,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniPlayerButton(if (comment.liked) "取消赞 ${comment.likeCount}" else "点赞 ${comment.likeCount}", onLike)
                MiniPlayerButton(if (comment.collected) "取消收藏" else "收藏", onCollect)
                if (canDelete) {
                    MiniPlayerButton("删除", onDelete)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SpeedMenuButton(
    speed: Float,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            focusedContainerColor = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .width(66.dp)
                .height(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${formatSpeed(speed)}x",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SpeedButton(
    speed: Float,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            focusedContainerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = "${formatSpeed(speed)}x",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ControlButton(
    icon: String,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    iconSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Box(
            modifier = Modifier
                .size(size),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = iconSize,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ControlTextButton(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MiniPlayerButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

private fun formatSpeed(speed: Float): String {
    return if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
