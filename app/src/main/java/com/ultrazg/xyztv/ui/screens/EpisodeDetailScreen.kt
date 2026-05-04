package com.ultrazg.xyztv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ultrazg.xyztv.data.model.CommentItem
import com.ultrazg.xyztv.ui.viewmodel.CommentViewModel
import com.ultrazg.xyztv.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    eid: String,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onNavigateToPodcast: (pid: String) -> Unit = {}
) {
    val playerViewModel: PlayerViewModel = viewModel()
    val commentViewModel: CommentViewModel = viewModel()
    val episode = playerViewModel.episode
    val pageScrollState = rememberScrollState()
    val descriptionScrollState = rememberScrollState()
    val commentsScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(eid) {
        playerViewModel.loadEpisode(eid)
        commentViewModel.loadPrimaryComments(eid)
    }

    when {
        playerViewModel.error != null && episode == null -> CenterMessage("加载失败：${playerViewModel.error}")
        playerViewModel.isLoading || episode == null -> CenterMessage("正在加载单集...")
        else -> BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val screenHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(pageScrollState)
                    .padding(horizontal = 46.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight - 72.dp),
                    horizontalArrangement = Arrangement.spacedBy(34.dp)
                ) {
                    Column(
                        modifier = Modifier.width(270.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = episode.bestCoverUrl ?: "",
                            contentDescription = episode.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(136.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        BigPlayButton(onClick = onPlay)
                        if (!episode.podcast?.pid.isNullOrBlank()) {
                            DetailSmallButton("前往播客") {
                                onNavigateToPodcast(episode.podcast!!.pid)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DetailSmallButton("返回", onBack)
                            DetailSmallButton("评论 ${commentViewModel.comments.size}") {
                                coroutineScope.launch {
                                    pageScrollState.animateScrollTo(pageScrollState.maxValue)
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .focusable()
                            .onKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                                when (event.key) {
                                    Key.DirectionUp -> {
                                        coroutineScope.launch {
                                            descriptionScrollState.scroll {
                                                scrollBy(-80f)
                                            }
                                        }
                                        true
                                    }
                                    Key.DirectionDown -> {
                                        coroutineScope.launch {
                                            descriptionScrollState.scroll {
                                                scrollBy(80f)
                                            }
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            }
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(descriptionScrollState)
                                .padding(end = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Text(
                                text = episode.podcast?.title.orEmpty(),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = episode.title,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 36.sp
                            )
                            Text(
                                text = "时长 ${formatDurationSec(episode.duration)}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Text(
                                text = episode.description?.takeIf { it.isNotBlank() } ?: "这个单集暂时没有简介。",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                lineHeight = 25.sp
                            )
                        }
                        DetailScrollBar(
                            scrollState = descriptionScrollState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight - 72.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(30.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(commentsScrollState)
                            .padding(end = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(text = "评论 ${commentViewModel.comments.size}", color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                        when {
                            commentViewModel.isLoading -> FeatureMessageCard("正在加载评论...", MaterialTheme.colorScheme.onSurface)
                            commentViewModel.error != null -> FeatureMessageCard("评论加载失败：${commentViewModel.error}", MaterialTheme.colorScheme.primary)
                            commentViewModel.comments.isEmpty() -> FeatureMessageCard("还没有评论", MaterialTheme.colorScheme.onSurface)
                            else -> commentViewModel.comments.forEach { comment ->
                                EpisodeDetailCommentCard(comment)
                            }
                        }
                    }
                    DetailScrollBar(
                        scrollState = commentsScrollState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DetailScrollBar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.border.copy(alpha = 0.10f)
    val thumbColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.width(6.dp)) {
        val radius = CornerRadius(size.width / 2f, size.width / 2f)
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, 0f),
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
private fun BigPlayButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(28.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "▶", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = "播放", color = MaterialTheme.colorScheme.onBackground, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DetailSmallButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeDetailCommentCard(comment: CommentItem) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.author?.nickname.orEmpty(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = comment.createdAt?.take(10).orEmpty(), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            }
            Text(
                text = comment.text,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDurationSec(seconds: Long): String {
    val minutes = (seconds / 60L).coerceAtLeast(0L)
    val hours = minutes / 60L
    val remainMinutes = minutes % 60L
    return if (hours > 0) "${hours}h ${remainMinutes}m" else "${minutes}m"
}
