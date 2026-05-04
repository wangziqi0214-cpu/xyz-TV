package com.ultrazg.xyztv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.material3.ClickableSurfaceDefaults
import kotlinx.coroutines.launch
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ultrazg.xyztv.data.model.Episode
import com.ultrazg.xyztv.data.model.Podcast
import com.ultrazg.xyztv.data.model.PodcastHonor
import com.ultrazg.xyztv.data.model.PodcastOwnerInfo
import com.ultrazg.xyztv.ui.viewmodel.PodcastDetailViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PodcastDetailScreen(
    pid: String,
    onBack: () -> Unit,
    onEpisodeClick: (eid: String) -> Unit,
    onPodcastClick: (pid: String) -> Unit = {}
) {
    val viewModel: PodcastDetailViewModel = viewModel()
    val firstEpisodeFocusRequester = remember(pid) { FocusRequester() }

    LaunchedEffect(pid) {
        viewModel.loadPodcast(pid)
    }

    LaunchedEffect(viewModel.episodes.size) {
        if (viewModel.episodes.isNotEmpty()) {
            withFrameNanos { }
            runCatching {
                firstEpisodeFocusRequester.requestFocus()
            }
        }
    }

    val podcast = viewModel.podcast

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            viewModel.error != null && podcast == null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = "加载失败：${viewModel.error}", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                    Surface(
                        onClick = { viewModel.loadPodcast(pid) },
                        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "重试",
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            viewModel.isLoading || podcast == null -> {
                CenterMessage("正在加载节目详情...")
            }

            else -> {
                TvLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Surface(
                            onClick = onBack,
                            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
                            colors = ClickableSurfaceDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.primary
                            ),
                            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f)
                        ) {
                            Text(
                                text = "‹ Back",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }
                    }

                    item {
                        PodcastHeader(
                            podcast = podcast,
                            ownerInfo = viewModel.ownerInfo,
                            honors = viewModel.honors,
                            isUpdatingSubscription = viewModel.isUpdatingSubscription,
                            onToggleSubscription = viewModel::toggleSubscription
                        )
                    }

                    viewModel.bulletin?.let { info ->
                        item {
                            BulletinCard(
                                title = info.title ?: "节目公告",
                                content = info.content ?: ""
                            )
                        }
                    }

                    if (viewModel.popularEpisodes.isNotEmpty()) {
                        item {
                            SectionTitle(
                                title = "最受欢迎",
                                subtitle = "对应 xyz 的 episode/list-by-filter。"
                            )
                        }
                        item {
                            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(viewModel.popularEpisodes.size) { index ->
                                    val episode = viewModel.popularEpisodes[index]
                                    EpisodePosterCard(
                                        episode = episode,
                                        onClick = { onEpisodeClick(episode.eid) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        SectionTitle(
                            title = "单集列表",
                            subtitle = "按时间顺序浏览完整节目内容。"
                        )
                    }

                    items(viewModel.episodes.size) { index ->
                        val episode = viewModel.episodes[index]
                        EpisodeRow(
                            episode = episode,
                            onClick = { onEpisodeClick(episode.eid) },
                            modifier = if (index == 0) Modifier.focusRequester(firstEpisodeFocusRequester) else Modifier
                        )
                    }

                    if (viewModel.relatedPodcasts.isNotEmpty()) {
                        item {
                            SectionTitle(
                                title = "相关节目",
                                subtitle = "对应 xyz 的 related-podcast/list。"
                            )
                        }
                        item {
                            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(viewModel.relatedPodcasts.size) { index ->
                                    val related = viewModel.relatedPodcasts[index]
                                    PodcastCard(
                                        podcast = related,
                                        onClick = { onPodcastClick(related.pid) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PodcastHeader(
    podcast: Podcast,
    ownerInfo: PodcastOwnerInfo?,
    honors: List<PodcastHonor>,
    isUpdatingSubscription: Boolean,
    onToggleSubscription: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = podcast.bestCoverUrl ?: "",
            contentDescription = podcast.title,
            modifier = Modifier.size(164.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = podcast.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = podcast.author.orEmpty(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )
            Text(
                text = podcast.description.orEmpty(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Text(
                text = "${podcast.episodeCount} 集 / ${podcast.subscriptionCount} 订阅",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
            ownerInfo?.let {
                val meta = listOfNotNull(
                    it.subject?.takeIf(String::isNotBlank),
                    it.ipLoc?.takeIf(String::isNotBlank)
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                }
            }
            if (honors.isNotEmpty()) {
                TvLazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(honors.size) { index ->
                        val honor = honors[index]
                        HonorChip(honor)
                    }
                }
            }
            Surface(
                onClick = onToggleSubscription,
                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (podcast.subscriptionStatus == "ON") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isUpdatingSubscription) "处理中..." else if (podcast.subscriptionStatus == "ON") "取消订阅" else "订阅节目",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HonorChip(honor: PodcastHonor) {
    Surface(
        onClick = {},
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            honor.campaignTitle?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
            }
            Text(
                text = honor.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionTitle(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun BulletinCard(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp)
        Text(
            text = content,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodePosterCard(
    episode: Episode,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier
                .size(width = 320.dp, height = 220.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = episode.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = episode.description.orEmpty(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatDuration(episode.duration),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeRow(
    episode: Episode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = episode.description.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatDuration(episode.duration),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val hours = minutes / 60
    val remainMinutes = minutes % 60
    return if (hours > 0) "${hours}h ${remainMinutes}m" else "${minutes}m"
}
