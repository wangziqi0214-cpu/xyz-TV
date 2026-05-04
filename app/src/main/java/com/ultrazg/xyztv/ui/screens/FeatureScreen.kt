package com.ultrazg.xyztv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ultrazg.xyztv.data.model.EditorPickDay
import com.ultrazg.xyztv.data.model.UserPick
import com.ultrazg.xyztv.ui.viewmodel.FeatureFeedViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun InboxScreen(
    onBack: () -> Unit,
    onEpisodeClick: (eid: String) -> Unit
) {
    val viewModel: FeatureFeedViewModel = viewModel()
    LaunchedEffect(Unit) {
        viewModel.loadInbox()
    }

    EpisodeFeedScreen(
        title = "订阅更新",
        subtitle = "对应 xyz 的 inbox 列表，看看你订阅的节目最近更新了哪些单集。",
        onBack = onBack,
        onEpisodeClick = onEpisodeClick,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PilotDiscoveryScreen(
    onBack: () -> Unit,
    onEpisodeClick: (eid: String) -> Unit
) {
    val viewModel: FeatureFeedViewModel = viewModel()
    LaunchedEffect(Unit) {
        viewModel.loadPilotDiscovery()
    }

    EpisodeFeedScreen(
        title = "新节目广场",
        subtitle = "对应 xyz 的 pilot discovery，用来发现新节目的首批内容。",
        onBack = onBack,
        onEpisodeClick = onEpisodeClick,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EditorPickHistoryScreen(
    onBack: () -> Unit,
    onEpisodeClick: (eid: String) -> Unit
) {
    val viewModel: FeatureFeedViewModel = viewModel()
    val days = viewModel.editorPickDays
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    LaunchedEffect(Unit) {
        viewModel.loadEditorPickHistory()
    }

    TvLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            FeatureHeader(
                title = "编辑精选历史",
                subtitle = "对应 xyz 的 editor pick history，按日期查看过往精选和推荐语。",
                onBack = onBack
            )
        }

        when {
            isLoading -> item { CenterMessage("加载中...") }
            error != null -> item { FeatureMessageCard("加载失败：$error", Color(0xFFFFB74D)) }
            days.isEmpty() -> item { FeatureMessageCard("暂时还没有精选历史内容", Color(0xFF3A3D42)) }
            else -> {
                days.forEach { day ->
                    item {
                        EditorPickDaySection(
                            day = day,
                            onEpisodeClick = onEpisodeClick
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PickHistoryScreen(
    onBack: () -> Unit,
    onEpisodeClick: (eid: String) -> Unit
) {
    val viewModel: FeatureFeedViewModel = viewModel()
    val picks = viewModel.picks
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    LaunchedEffect(Unit) {
        viewModel.loadPickHistory()
    }

    TvLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            FeatureHeader(
                title = "我的喜欢",
                subtitle = "对应 xyz 用户主页里的喜欢列表，按单集浏览你标记过的内容。",
                onBack = onBack
            )
        }

        when {
            isLoading -> item { CenterMessage("加载中...") }
            error != null -> item { FeatureMessageCard("加载失败：$error", Color(0xFFFFB74D)) }
            picks.isEmpty() -> item { FeatureMessageCard("暂时还没有喜欢内容", Color(0xFF3A3D42)) }
            else -> {
                items(picks.size) { index ->
                    val pick = picks[index]
                    PickRow(
                        pick = pick,
                        onClick = { pick.episode?.eid?.let(onEpisodeClick) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeFeedScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onEpisodeClick: (eid: String) -> Unit,
    viewModel: FeatureFeedViewModel
) {
    val episodes = viewModel.episodes
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    TvLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            FeatureHeader(title = title, subtitle = subtitle, onBack = onBack)
        }

        when {
            isLoading -> item { CenterMessage("加载中...") }
            error != null -> item { FeatureMessageCard("加载失败：$error", Color(0xFFFFB74D)) }
            episodes.isEmpty() -> item { FeatureMessageCard("暂时还没有内容", Color(0xFF3A3D42)) }
            else -> {
                items(episodes.size) { index ->
                    val episode = episodes[index]
                    EpisodeRow(episode = episode, onClick = { onEpisodeClick(episode.eid) })
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FeatureHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            onClick = onBack,
            shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "返回",
                color = Color(0xFF020303),
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }

        Text(
            text = title,
            color = Color(0xFF020303),
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.headlineLarge
        )
        Text(text = subtitle, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FeatureMessageCard(
    message: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Text(text = message, color = color, fontSize = 16.sp)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EditorPickDaySection(
    day: EditorPickDay,
    onEpisodeClick: (eid: String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (day.date.isBlank()) "某一天的精选" else day.date,
            color = Color(0xFF020303),
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold
        )

        day.entries.forEach { entry ->
            val episode = entry.episode ?: return@forEach
            Surface(
                onClick = { onEpisodeClick(episode.eid) },
                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = episode.title,
                        color = Color(0xFF020303),
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = episode.podcast?.title ?: episode.description.orEmpty(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    entry.commentText?.takeIf { it.isNotBlank() }?.let { comment ->
                        Text(
                            text = comment,
                            color = Color(0xFFDCE5E7),
                            fontSize = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PickRow(
    pick: UserPick,
    onClick: () -> Unit
) {
    val episode = pick.episode ?: return
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = episode.title,
                    color = Color(0xFF020303),
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${pick.likeCount} 赞",
                    color = Color(0xFFFFC36D),
                    fontSize = 12.sp
                )
            }
            Text(
                text = episode.podcast?.title ?: "",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            pick.storyText?.takeIf { it.isNotBlank() }?.let { story ->
                Text(
                    text = story,
                    color = Color(0xFFDCE5E7),
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = pick.pickedAt?.take(10) ?: "",
                color = Color(0xFF7F8B90),
                fontSize = 12.sp
            )
        }
    }
}
