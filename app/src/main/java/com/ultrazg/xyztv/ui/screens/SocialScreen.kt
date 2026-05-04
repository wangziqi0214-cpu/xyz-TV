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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.ultrazg.xyztv.data.model.CommentItem
import com.ultrazg.xyztv.data.model.MileageEntry
import com.ultrazg.xyztv.data.model.Podcast
import com.ultrazg.xyztv.data.model.StickerBoardItem
import com.ultrazg.xyztv.data.model.StickerItem
import com.ultrazg.xyztv.data.model.UserLite
import com.ultrazg.xyztv.data.model.UserPreference
import com.ultrazg.xyztv.ui.viewmodel.SocialViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StarSubscriptionScreen(
    onBack: () -> Unit,
    onPodcastClick: (pid: String) -> Unit
) {
    val viewModel: SocialViewModel = viewModel()
    LaunchedEffect(Unit) { viewModel.loadStarSubscriptions() }
    PodcastCollectionScreen(
        title = "星标订阅",
        subtitle = "对应 xyz 的 subscription-star/list。",
        onBack = onBack,
        onPodcastClick = onPodcastClick,
        onToggleStar = { viewModel.toggleStarSubscription(it) },
        viewModel = viewModel
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun NonStarredSubscriptionScreen(
    onBack: () -> Unit,
    onPodcastClick: (pid: String) -> Unit
) {
    val viewModel: SocialViewModel = viewModel()
    LaunchedEffect(Unit) { viewModel.loadNonStarredSubscriptions() }
    PodcastCollectionScreen(
        title = "未加星标订阅",
        subtitle = "对应 xyz 的 subscription/list-non-starred。",
        onBack = onBack,
        onPodcastClick = onPodcastClick,
        onToggleStar = { viewModel.toggleStarSubscription(it) },
        viewModel = viewModel
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FollowingScreen(onBack: () -> Unit) {
    val viewModel: SocialViewModel = viewModel()
    LaunchedEffect(Unit) { viewModel.loadFollowing() }
    UserCollectionScreen(
        title = "我的关注",
        subtitle = "对应 xyz 的 following_list。",
        onBack = onBack,
        users = viewModel.users,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        actionText = { if (it.relation == "FOLLOWING") "取消关注" else "关注" },
        onAction = { user -> viewModel.toggleRelation(user) }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FollowerScreen(onBack: () -> Unit) {
    val viewModel: SocialViewModel = viewModel()
    LaunchedEffect(Unit) { viewModel.loadFollowers() }
    UserCollectionScreen(
        title = "我的粉丝",
        subtitle = "对应 xyz 的 follower_list。",
        onBack = onBack,
        users = viewModel.users,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        actionText = { if (it.relation == "FOLLOWING") "已回关" else "回关" },
        onAction = { user -> viewModel.toggleRelation(user) }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PreferenceScreen(onBack: () -> Unit) {
    val viewModel: SocialViewModel = viewModel()
    val preference = viewModel.preference
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    LaunchedEffect(Unit) { viewModel.loadPreferences() }

    TvLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            FeatureHeader(
                title = "偏好设置",
                subtitle = "对应 xyz 的 user-preference/get 与 update。",
                onBack = onBack
            )
        }

        when {
            isLoading -> item { CenterMessage("加载中...") }
            error != null -> item { FeatureMessageCard("加载失败：$error", Color(0xFFFFB74D)) }
            preference != null -> {
                item { PreferenceToggleRow("隐藏最近收听", preference.isRecentPlayedHidden) { viewModel.togglePreference("isRecentPlayedHidden", preference.isRecentPlayedHidden) } }
                item { PreferenceToggleRow("评论区隐藏收听时长", preference.isListenMileageHiddenInComment) { viewModel.togglePreference("isListenMileageHiddenInComment", preference.isListenMileageHiddenInComment) } }
                item { PreferenceToggleRow("隐藏贴纸库", preference.isStickerLibraryHidden) { viewModel.togglePreference("isStickerLibraryHidden", preference.isStickerLibraryHidden) } }
                item { PreferenceToggleRow("隐藏贴纸板", preference.isStickerBoardHidden) { viewModel.togglePreference("isStickerBoardHidden", preference.isStickerBoardHidden) } }
                item { PreferenceToggleRow("拒绝热门推送", preference.rejectHotPush) { viewModel.togglePreference("rejectHotPush", preference.rejectHotPush) } }
                item { PreferenceToggleRow("拒绝个性化推荐", preference.rejectRecommendation) { viewModel.togglePreference("rejectRecommendation", preference.rejectRecommendation) } }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CollectedCommentScreen(
    onBack: () -> Unit,
    onEpisodeClick: (eid: String) -> Unit
) {
    val viewModel: SocialViewModel = viewModel()
    LaunchedEffect(Unit) { viewModel.loadCollectedComments() }

    CommentCollectionScreen(
        title = "收藏评论",
        subtitle = "对应 xyz 的 comment collect list。",
        onBack = onBack,
        comments = viewModel.collectedComments,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        onEpisodeClick = onEpisodeClick
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun BlockedUserScreen(onBack: () -> Unit) {
    val viewModel: SocialViewModel = viewModel()
    LaunchedEffect(Unit) { viewModel.loadBlockedUsers() }
    UserCollectionScreen(
        title = "黑名单",
        subtitle = "对应 xyz 的 blocked-user/list。",
        onBack = onBack,
        users = viewModel.users,
        isLoading = viewModel.isLoading,
        error = viewModel.error,
        actionText = { "移出黑名单" },
        onAction = { user -> viewModel.toggleBlockedUser(user) }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MileageScreen(
    onBack: () -> Unit,
    onPodcastClick: (pid: String) -> Unit
) {
    val viewModel: SocialViewModel = viewModel()
    LaunchedEffect(Unit) { viewModel.loadMileage(all = false) }

    val overview = viewModel.mileageOverview
    val entries = viewModel.mileageEntries
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
            FeatureHeader(
                title = "收听数据",
                subtitle = "对应 xyz 的 mileage get / list。",
                onBack = onBack
            )
        }

        overview?.let { data ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MileageStatCard("总收听", formatHours(data.totalPlayedSeconds))
                    MileageStatCard("近 7 天", formatHours(data.lastSevenDayPlayedSeconds))
                    MileageStatCard("近 30 天", formatHours(data.lastThirtyDayPlayedSeconds))
                }
            }
            data.tagline?.takeIf { it.isNotBlank() }?.let { tagline ->
                item { FeatureMessageCard(tagline, Color(0xFFDCE5E7)) }
            }
        }

        when {
            isLoading -> item { CenterMessage("加载中...") }
            error != null -> item { FeatureMessageCard("加载失败：$error", Color(0xFFFFB74D)) }
            entries.isEmpty() -> item { FeatureMessageCard("暂时还没有收听排行", Color(0xFF3A3D42)) }
            else -> {
                items(entries.size) { index ->
                    val entry = entries[index]
                    val podcast = entry.podcast ?: return@items
                    Surface(
                        onClick = { onPodcastClick(podcast.pid) },
                        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${index + 1}", color = Color(0xFFFFC36D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            AsyncImage(
                                model = podcast.bestCoverUrl ?: "",
                                contentDescription = podcast.title,
                                modifier = Modifier.size(72.dp)
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = podcast.title, color = Color(0xFF020303), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = podcast.author.orEmpty(), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(text = formatHours(entry.playedSeconds), color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StickerScreen(onBack: () -> Unit) {
    val viewModel: SocialViewModel = viewModel()
    LaunchedEffect(Unit) { viewModel.loadStickers() }

    val stickers = viewModel.stickers
    val stickerBoard = viewModel.stickerBoard
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
            FeatureHeader(
                title = "贴纸与贴纸板",
                subtitle = "对应 xyz 的 sticker/list 与 sticker/get-board。",
                onBack = onBack
            )
        }

        when {
            isLoading -> item { CenterMessage("加载中...") }
            error != null -> item { FeatureMessageCard("加载失败：$error", Color(0xFFFFB74D)) }
            else -> {
                if (stickerBoard.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "贴纸板", color = Color(0xFF020303), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(stickerBoard.size) { index ->
                                    StickerBoardCard(stickerBoard[index])
                                }
                            }
                        }
                    }
                }

                if (stickers.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "贴纸库", color = Color(0xFF020303), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(stickers.size) { index ->
                                    StickerCard(stickers[index])
                                }
                            }
                        }
                    }
                }

                if (stickerBoard.isEmpty() && stickers.isEmpty()) {
                    item { FeatureMessageCard("暂时还没有贴纸内容", Color(0xFF3A3D42)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PodcastCollectionScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onPodcastClick: (pid: String) -> Unit,
    onToggleStar: (Podcast) -> Unit,
    viewModel: SocialViewModel
) {
    val podcasts = viewModel.podcasts
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    TvLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { FeatureHeader(title = title, subtitle = subtitle, onBack = onBack) }
        when {
            isLoading -> item { CenterMessage("加载中...") }
            error != null -> item { FeatureMessageCard("加载失败：$error", Color(0xFFFFB74D)) }
            podcasts.isEmpty() -> item { FeatureMessageCard("暂无内容", Color(0xFF3A3D42)) }
            else -> {
                items(podcasts.size) { index ->
                    val podcast = podcasts[index]
                    Surface(
                        onClick = { onPodcastClick(podcast.pid) },
                        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = podcast.bestCoverUrl ?: "",
                                contentDescription = podcast.title,
                                modifier = Modifier.size(88.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = podcast.title, color = Color(0xFF020303), fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(text = podcast.author.orEmpty(), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Surface(
                                onClick = { onToggleStar(podcast) },
                                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (podcast.subscriptionStar) Color(0xFFFFC36D) else MaterialTheme.colorScheme.surfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = if (podcast.subscriptionStar) "取消星标" else "加入星标",
                                    color = if (podcast.subscriptionStar) Color.Black else Color(0xFF020303),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
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
private fun UserCollectionScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    users: List<UserLite>,
    isLoading: Boolean,
    error: String?,
    actionText: (UserLite) -> String,
    onAction: (UserLite) -> Unit
) {
    TvLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { FeatureHeader(title = title, subtitle = subtitle, onBack = onBack) }
        when {
            isLoading -> item { CenterMessage("加载中...") }
            error != null -> item { FeatureMessageCard("加载失败：$error", Color(0xFFFFB74D)) }
            users.isEmpty() -> item { FeatureMessageCard("暂无内容", Color(0xFF3A3D42)) }
            else -> {
                items(users.size) { index ->
                    val user = users[index]
                    Surface(
                        onClick = { onAction(user) },
                        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = user.avatarUrl ?: "",
                                contentDescription = user.nickname,
                                modifier = Modifier.size(72.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = user.nickname, color = Color(0xFF020303), fontSize = 17.sp)
                                Text(text = user.ipLoc ?: "", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                            Text(
                                text = actionText(user),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
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
private fun PreferenceToggleRow(
    label: String,
    enabled: Boolean,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color(0xFF020303), fontSize = 16.sp)
            Text(
                text = if (enabled) "已开启" else "已关闭",
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CommentCollectionScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    comments: List<CommentItem>,
    isLoading: Boolean,
    error: String?,
    onEpisodeClick: (eid: String) -> Unit
) {
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
            comments.isEmpty() -> item { FeatureMessageCard("暂时还没有内容", Color(0xFF3A3D42)) }
            else -> {
                items(comments.size) { index ->
                    val comment = comments[index]
                    CommentCollectRow(comment = comment, onEpisodeClick = onEpisodeClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CommentCollectRow(
    comment: CommentItem,
    onEpisodeClick: (eid: String) -> Unit
) {
    val episode = comment.episode
    Surface(
        onClick = { episode?.eid?.let(onEpisodeClick) },
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = comment.text, color = Color(0xFF020303), fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = comment.author?.nickname.orEmpty(), color = Color(0xFFFFC36D), fontSize = 12.sp)
            Text(
                text = episode?.title ?: "",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StickerCard(sticker: StickerItem) {
    Surface(
        onClick = {},
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(model = sticker.imageUrl ?: "", contentDescription = sticker.name, modifier = Modifier.size(140.dp))
            Text(text = sticker.name, color = Color(0xFF020303), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = sticker.issuer.orEmpty(), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun StickerBoardCard(item: StickerBoardItem) {
    Surface(
        onClick = {},
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(model = item.sticker.imageUrl ?: "", contentDescription = item.sticker.name, modifier = Modifier.size(140.dp))
            Text(text = item.sticker.name, color = Color(0xFF020303), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "位置 ${item.x}, ${item.y}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MileageStatCard(
    title: String,
    value: String
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
        Text(text = value, color = Color(0xFF020303), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatHours(seconds: Long): String {
    if (seconds <= 0) return "0h"
    val minutes = seconds / 60
    val hours = minutes / 60
    val remainMinutes = minutes % 60
    return if (hours > 0) "${hours}h ${remainMinutes}m" else "${minutes}m"
}
