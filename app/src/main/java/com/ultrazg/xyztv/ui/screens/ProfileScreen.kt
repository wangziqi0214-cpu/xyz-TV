package com.ultrazg.xyztv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ultrazg.xyztv.ui.components.ContentShelf
import com.ultrazg.xyztv.ui.components.PersonIcon
import com.ultrazg.xyztv.ui.components.PodcastCard
import com.ultrazg.xyztv.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onPodcastClick: (pid: String) -> Unit,
    onEpisodeClick: (eid: String) -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToPickHistory: () -> Unit,
    onNavigateToFollowing: () -> Unit,
    onNavigateToFollowers: () -> Unit,
    onNavigateToStarSubscriptions: () -> Unit,
    onNavigateToNonStarredSubscriptions: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    onNavigateToCollectedComments: () -> Unit,
    onNavigateToMileage: () -> Unit,
    onNavigateToStickers: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit
) {
    val viewModel: ProfileViewModel = viewModel()
    val profile = viewModel.profile
    val stats = viewModel.stats
    val unreadCount = viewModel.unreadCount
    val ownedPodcasts = viewModel.ownedPodcasts
    val recentPicks = viewModel.recentPicks
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 20.dp)
    ) {
        ProfileHeader(onBack = onBack)

        Spacer(modifier = Modifier.height(20.dp))

        when {
            isLoading && profile == null -> {
                ProfileLoadingPlaceholder()
            }
            error != null && profile == null -> {
                ProfileErrorCard(message = error!!, onRetry = { viewModel.load() })
            }
            profile != null -> {
                TvLazyColumn(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                PersonIcon(
                                    size = 48.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = profile.nickname.ifBlank { "我的主页" },
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = profile.bio?.ifBlank { "这个账号还没有简介。" } ?: "这个账号还没有简介。",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (unreadCount > 0) {
                                    Text(
                                        text = "未读消息：$unreadCount",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    stats?.let { state ->
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatCard("关注", state.followingCount.toString())
                                StatCard("粉丝", state.followerCount.toString())
                                StatCard("订阅", state.subscriptionCount.toString())
                                StatCard("收听时长", formatListenDuration(state.totalPlayedSeconds))
                            }
                        }
                    }

                    item {
                        ActionShelf(
                            title = "个人行为",
                            actions = listOf(
                                "我的订阅" to onNavigateToSubscriptions,
                                "我的收藏" to onNavigateToFavorites,
                                "收听历史" to onNavigateToHistory,
                                "订阅更新" to onNavigateToInbox
                            )
                        )
                    }

                    item {
                        ActionShelf(
                            title = "账号工具",
                            actions = listOf(
                                "我的关注" to onNavigateToFollowing,
                                "我的粉丝" to onNavigateToFollowers,
                                "星标订阅" to onNavigateToStarSubscriptions,
                                "未加星标" to onNavigateToNonStarredSubscriptions,
                                "收听数据" to onNavigateToMileage,
                                "贴纸" to onNavigateToStickers,
                                "黑名单" to onNavigateToBlockedUsers,
                                "账号偏好" to onNavigateToPreferences,
                                "收藏评论" to onNavigateToCollectedComments
                            )
                        )
                    }

                    if (recentPicks.isNotEmpty()) {
                        item {
                            ContentShelf(
                                title = "我的喜欢",
                                onViewAll = onNavigateToPickHistory
                            ) {
                                TvLazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(recentPicks.size) { index ->
                                        val pick = recentPicks[index]
                                        Box(modifier = Modifier.width(520.dp)) {
                                            PickRow(
                                                pick = pick,
                                                onClick = { pick.episode?.eid?.let(onEpisodeClick) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (ownedPodcasts.isNotEmpty()) {
                        item {
                            ContentShelf(
                                title = "我创建的播客",
                                onViewAll = {}
                            ) {
                                TvLazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(ownedPodcasts.size) { index ->
                                        val podcast = ownedPodcasts[index]
                                        PodcastCard(
                                            podcast = podcast,
                                            onClick = { onPodcastClick(podcast.pid) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (error != null) {
                        item {
                            Text(
                                text = "部分信息加载失败：$error",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileHeader(onBack: () -> Unit) {
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
            text = "我的主页",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionShelf(
    title: String,
    actions: List<Pair<String, () -> Unit>>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(actions.size) { index ->
                val action = actions[index]
                ProfileNavButton(action.first, action.second)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileNavButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StatCard(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
        Text(text = value, color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileLoadingPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.width(160.dp).height(24.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
            Box(modifier = Modifier.width(240.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ProfileErrorCard(
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

private fun formatListenDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0) return "0m"
    val totalMinutes = totalSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
