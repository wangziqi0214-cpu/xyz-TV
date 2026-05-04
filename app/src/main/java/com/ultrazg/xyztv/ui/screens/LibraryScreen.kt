package com.ultrazg.xyztv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.ultrazg.xyztv.ui.components.EpisodeWideCard
import com.ultrazg.xyztv.ui.components.PodcastCard
import com.ultrazg.xyztv.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    onPodcastClick: (pid: String) -> Unit
) {
    val viewModel: LibraryViewModel = viewModel()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadSubscriptions()
    }

    val podcasts = viewModel.podcasts
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 20.dp)
    ) {
        LibraryHeader(title = "我的订阅", onBack = onBack)

        Spacer(modifier = Modifier.height(20.dp))

        when {
            isLoading -> {
                LibraryLoadingPlaceholder()
            }
            error != null -> {
                LibraryErrorCard(message = error, onRetry = { viewModel.loadSubscriptions() })
            }
            podcasts.isEmpty() -> {
                LibraryEmptyState(message = "还没有订阅任何播客")
            }
            else -> {
                TvLazyVerticalGrid(
                    columns = TvGridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(podcasts.size) { index ->
                        val podcast = podcasts[index]
                        PodcastCard(
                            podcast = podcast,
                            onClick = { onPodcastClick(podcast.pid) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FavoriteScreen(
    onBack: () -> Unit,
    onEpisodeClick: (eid: String) -> Unit
) {
    val viewModel: LibraryViewModel = viewModel()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadFavorites()
    }

    val episodes = viewModel.episodes
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 20.dp)
    ) {
        LibraryHeader(title = "我的收藏", onBack = onBack)

        Spacer(modifier = Modifier.height(20.dp))

        when {
            isLoading -> LibraryLoadingPlaceholder()
            error != null -> LibraryErrorCard(message = error, onRetry = { viewModel.loadFavorites() })
            episodes.isEmpty() -> LibraryEmptyState(message = "还没有收藏任何单集")
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(episodes) { episode ->
                        EpisodeWideCard(
                            episode = episode,
                            onClick = { onEpisodeClick(episode.eid) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onEpisodeClick: (eid: String) -> Unit
) {
    val viewModel: LibraryViewModel = viewModel()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    val episodes = viewModel.episodes
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 20.dp)
    ) {
        LibraryHeader(title = "收听历史", onBack = onBack)

        Spacer(modifier = Modifier.height(20.dp))

        when {
            isLoading -> LibraryLoadingPlaceholder()
            error != null -> LibraryErrorCard(message = error, onRetry = { viewModel.loadHistory() })
            episodes.isEmpty() -> LibraryEmptyState(message = "还没有收听记录")
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(episodes) { episode ->
                        EpisodeWideCard(
                            episode = episode,
                            onClick = { onEpisodeClick(episode.eid) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibraryHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibraryLoadingPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .width(146.dp)
                    .height(120.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibraryErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "⚠ 加载失败",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                onClick = onRetry,
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f)
            ) {
                Text(
                    text = "重试",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibraryEmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp
        )
    }
}
