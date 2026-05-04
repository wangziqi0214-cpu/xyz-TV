package com.ultrazg.xyztv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import coil.request.ImageRequest
import com.ultrazg.xyztv.data.HomeFeature
import com.ultrazg.xyztv.data.HomeLayoutManager
import com.ultrazg.xyztv.data.HomePlacement
import com.ultrazg.xyztv.data.model.CategorySummary
import com.ultrazg.xyztv.data.model.DiscoveryItem
import com.ultrazg.xyztv.data.model.DiscoverySection
import com.ultrazg.xyztv.data.model.EditorPickDay
import com.ultrazg.xyztv.data.model.Episode
import com.ultrazg.xyztv.data.model.Podcast
import com.ultrazg.xyztv.ui.components.ContentShelf
import com.ultrazg.xyztv.ui.components.EpisodeWideCard
import com.ultrazg.xyztv.ui.components.PersonIcon
import com.ultrazg.xyztv.ui.viewmodel.HomeViewModel

private const val MaxHomeDiscoverySections = 8
private const val MaxHomeDiscoveryItemsPerRow = 8
private typealias RowFocusTargets = Any

@Suppress("UNUSED_PARAMETER")
private fun Modifier.rowFocusItem(
    isLeadingItem: Boolean,
    targets: Any?,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    upOverride: androidx.compose.ui.focus.FocusRequester? = null,
    downOverride: androidx.compose.ui.focus.FocusRequester? = null
): Modifier = this

@Composable
private fun rememberHomeImageRequest(
    url: String?,
    widthPx: Int,
    heightPx: Int
): ImageRequest {
    val context = LocalContext.current
    return remember(url, widthPx, heightPx) {
        ImageRequest.Builder(context)
            .data(url.orEmpty())
            .size(widthPx, heightPx)
            .crossfade(false)
            .allowHardware(true)
            .build()
    }
}


@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPodcast: (pid: String) -> Unit = {},
    onNavigateToEpisode: (eid: String) -> Unit = {},
    onNavigateToCategory: (categoryId: String, categoryName: String) -> Unit = { _, _ -> },
    onNavigateToCategoryHub: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTopList: (category: String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToPilotDiscovery: () -> Unit = {},
    onNavigateToEditorPickHistory: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val viewModel: HomeViewModel = viewModel()
    val sections = viewModel.sections
    val categories = viewModel.categories
    val pilotEpisodes = viewModel.pilotEpisodes
    val todayEditorPick = viewModel.todayEditorPick
    val hotTopEpisodes = viewModel.hotTopEpisodes
    val rockTopEpisodes = viewModel.rockTopEpisodes
    val newTopEpisodes = viewModel.newTopEpisodes
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    val layoutVersion = HomeLayoutManager.version
    val orderedFeatures = HomeLayoutManager.orderedFeatures()
    val topFeatures = orderedFeatures.filter { HomeLayoutManager.placementOf(it) == HomePlacement.TOP }
    val shelfFeatures = orderedFeatures.filter { HomeLayoutManager.placementOf(it) == HomePlacement.SHELF }
    val visibleDiscoverySections = remember(sections) {
        sections
            .filter { it.items.orEmpty().isNotEmpty() }
            .take(MaxHomeDiscoverySections)
            .map { section ->
                section.copy(items = section.items.orEmpty().take(MaxHomeDiscoveryItemsPerRow))
            }
    }
    @Suppress("UNUSED_PARAMETER")
    fun focusTargets(index: Int): RowFocusTargets? = null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            isLoading && sections.isEmpty() && categories.isEmpty() -> {
                CenterMessage("正在加载首页内容...")
            }

            error != null && sections.isEmpty() && categories.isEmpty() -> {
                CenterError(
                    message = "首页加载失败：$error",
                    buttonText = "重新登录",
                    onClick = onNavigateToLogin
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 28.dp, end = 28.dp, top = 20.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (topFeatures.isNotEmpty()) {
                        TopFeatureChipRow(
                            features = topFeatures,
                            onOpenCategoryHub = onNavigateToCategoryHub,
                            onOpenPilotDiscovery = onNavigateToPilotDiscovery,
                            onOpenEditorPicks = onNavigateToEditorPickHistory,
                            onOpenTopLists = { onNavigateToTopList("HOT") }
                        )
                    }

                    shelfFeatures.forEach { feature ->
                        when (feature) {
                            HomeFeature.CATEGORIES -> if (categories.isNotEmpty()) {
                                CategoryChipShelf(
                                    categories = categories,
                                    onCategoryClick = onNavigateToCategory
                                )
                            }

                            HomeFeature.PILOT_DISCOVERY -> if (pilotEpisodes.isNotEmpty()) {
                                ContentShelf(
                                    title = "新节目广场",
                                    onViewAll = onNavigateToPilotDiscovery
                                ) {
                                    EpisodeCardRow(
                                        episodes = pilotEpisodes.take(8),
                                        onEpisodeClick = onNavigateToEpisode
                                    )
                                }
                            }

                            HomeFeature.EDITOR_PICKS -> if (todayEditorPick != null && todayEditorPick.entries.isNotEmpty()) {
                                ContentShelf(
                                    title = "编辑精选",
                                    onViewAll = onNavigateToEditorPickHistory
                                ) {
                                    EpisodeCardRow(
                                        episodes = todayEditorPick.entries.mapNotNull { it.episode },
                                        onEpisodeClick = onNavigateToEpisode
                                    )
                                }
                            }

                            HomeFeature.TOP_LISTS -> if (
                                hotTopEpisodes.isNotEmpty() || rockTopEpisodes.isNotEmpty() || newTopEpisodes.isNotEmpty()
                            ) {
                                TopListsSection(
                                    hotTopEpisodes = hotTopEpisodes,
                                    rockTopEpisodes = rockTopEpisodes,
                                    newTopEpisodes = newTopEpisodes,
                                    onPodcastClick = onNavigateToPodcast,
                                    onEpisodeClick = onNavigateToEpisode,
                                    onOpenTopList = onNavigateToTopList
                                )
                            }
                        }
                    }

                    if (sections.isEmpty() && categories.isEmpty()) {
                        HomeSectionsEmptyState()
                    }

                    visibleDiscoverySections.forEach { section ->
                        ContentShelf(
                            title = section.title ?: "推荐内容"
                        ) {
                            DiscoveryItemRow(
                                section = section,
                                onPodcastClick = onNavigateToPodcast,
                                onEpisodeClick = onNavigateToEpisode
                            )
                        }
                    }

                    if (error != null) {
                        Text(
                            text = "部分模块加载失败：$error",
                            color = Color(0xFFFFC36D),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/* ========== NEW SHELF COMPONENTS ========== */

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeCardRow(
    episodes: List<Episode>,
    onEpisodeClick: (eid: String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        episodes.forEach { episode ->
            EpisodeWideCard(
                episode = episode,
                onClick = { onEpisodeClick(episode.eid) }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopFeatureChipRow(
    features: List<HomeFeature>,
    onOpenCategoryHub: () -> Unit,
    onOpenPilotDiscovery: () -> Unit,
    onOpenEditorPicks: () -> Unit,
    onOpenTopLists: () -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        features.forEach { feature ->
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            Surface(
                onClick = {
                    when (feature) {
                        HomeFeature.CATEGORIES -> onOpenCategoryHub()
                        HomeFeature.PILOT_DISCOVERY -> onOpenPilotDiscovery()
                        HomeFeature.EDITOR_PICKS -> onOpenEditorPicks()
                        HomeFeature.TOP_LISTS -> onOpenTopLists()
                    }
                },
                interactionSource = interactionSource,
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f)
            ) {
                Text(
                    text = feature.title,
                    color = if (isFocused) Color.White else MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CategoryChipShelf(
    categories: List<CategorySummary>,
    onCategoryClick: (categoryId: String, categoryName: String) -> Unit
) {
    ContentShelf(title = "分类探索") {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categories.forEach { category ->
                Surface(
                    onClick = { onCategoryClick(category.id, category.name) },
                    modifier = Modifier.width(180.dp),
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(18.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.primary
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = category.name,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = category.description ?: "浏览该分类",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
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
private fun DiscoveryItemRow(
    section: DiscoverySection,
    onPodcastClick: (pid: String) -> Unit,
    onEpisodeClick: (eid: String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        section.items.orEmpty().forEach { item ->
            val episode = item.episode
            val podcast = item.podcast ?: episode?.podcast ?: return@forEach
            val title = episode?.title ?: podcast.title
            val coverUrl = episode?.bestCoverUrl ?: podcast.bestCoverUrl.orEmpty()
            val clickAction = {
                if (episode != null) onEpisodeClick(episode.eid) else onPodcastClick(podcast.pid)
            }

            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()

            Surface(
                onClick = clickAction,
                modifier = Modifier.width(140.dp),
                interactionSource = interactionSource,
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(18.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = rememberHomeImageRequest(coverUrl, 280, 280),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = podcast.title,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopListsSection(
    hotTopEpisodes: List<Episode>,
    rockTopEpisodes: List<Episode>,
    newTopEpisodes: List<Episode>,
    onPodcastClick: (pid: String) -> Unit,
    onEpisodeClick: (eid: String) -> Unit,
    onOpenTopList: (category: String) -> Unit
) {
    ContentShelf(title = "榜单", onViewAll = { onOpenTopList("HOT") }) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopListBoardCard(
                title = "最热榜",
                category = "HOT",
                episodes = hotTopEpisodes,
                onPodcastClick = onPodcastClick,
                onEpisodeClick = onEpisodeClick,
                onOpenAll = { onOpenTopList("HOT") }
            )
            TopListBoardCard(
                title = "飙升榜",
                category = "ROCK",
                episodes = rockTopEpisodes,
                onPodcastClick = onPodcastClick,
                onEpisodeClick = onEpisodeClick,
                onOpenAll = { onOpenTopList("ROCK") }
            )
            TopListBoardCard(
                title = "新星榜",
                category = "NEW",
                episodes = newTopEpisodes,
                onPodcastClick = onPodcastClick,
                onEpisodeClick = onEpisodeClick,
                onOpenAll = { onOpenTopList("NEW") }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopListBoardCard(
    title: String,
    category: String,
    episodes: List<Episode>,
    onPodcastClick: (pid: String) -> Unit,
    onEpisodeClick: (eid: String) -> Unit,
    onOpenAll: () -> Unit
) {
    Surface(
        onClick = onOpenAll,
        modifier = Modifier.width(340.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(22.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.03f)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "查看全部",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (episodes.isEmpty()) {
                Text(
                    text = "暂无数据",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
            } else {
                episodes.take(5).forEachIndexed { index, episode ->
                    TopListEntryRow(
                        rank = index + 1,
                        category = category,
                        episode = episode,
                        onClick = {
                            val pid = episode.podcast?.pid.orEmpty()
                            if (pid.isNotBlank()) onPodcastClick(pid) else onEpisodeClick(episode.eid)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopListEntryRow(
    rank: Int,
    category: String,
    episode: Episode,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
            focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$rank",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp)
            )
            AsyncImage(
                model = episode.bestCoverUrl.orEmpty(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.podcast?.title ?: episode.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = episode.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/* ========== OLD COMPONENTS (to be cleaned up) ========== */

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FocusAwareHomeHeader(
    profileName: String,
    profileAvatarUrl: String?,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    rowFocusTargets: RowFocusTargets?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant)
                )
            )
            .padding(horizontal = 30.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "听播客，上小宇宙",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "首页只保留公共发现能力，搜索和个人入口已经收进顶部横条。",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        Text(
            text = "你可以在设置里决定分类探索、新节目广场、编辑精选和首页榜单，是放在 Top 栏还是横滑栏目。",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FocusAwareTopFeatureRow(
    features: List<HomeFeature>,
    onOpenCategoryHub: () -> Unit,
    onOpenPilotDiscovery: () -> Unit,
    onOpenEditorPicks: () -> Unit,
    onOpenTopLists: () -> Unit,
    rowFocusTargets: RowFocusTargets?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading(
            title = "顶部功能区",
            subtitle = "这里放的是你设定为 Top 栏的公共模块入口。"
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            features.forEachIndexed { index, feature ->
                Surface(
                    onClick = {
                        when (feature) {
                            HomeFeature.CATEGORIES -> onOpenCategoryHub()
                            HomeFeature.PILOT_DISCOVERY -> onOpenPilotDiscovery()
                            HomeFeature.EDITOR_PICKS -> onOpenEditorPicks()
                            HomeFeature.TOP_LISTS -> onOpenTopLists()
                        }
                    },
                    modifier = Modifier.rowFocusItem(isLeadingItem = index == 0, targets = rowFocusTargets),
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = feature.title, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = feature.summary,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
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
private fun FocusAwareCategoryShelf(
    categories: List<CategorySummary>,
    onCategoryClick: (categoryId: String, categoryName: String) -> Unit,
    rowFocusTargets: RowFocusTargets?
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading(
            title = "分类探索",
            subtitle = "先扫一遍分类，再决定要深入哪条内容线。"
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            categories.forEachIndexed { index, category ->
                Surface(
                    onClick = { onCategoryClick(category.id, category.name) },
                    modifier = Modifier
                        .width(244.dp)
                        .rowFocusItem(
                            isLeadingItem = index == 0,
                            targets = rowFocusTargets
                        ),
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(22.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.primary
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = "分类", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                        }
                        Text(text = category.name, color = MaterialTheme.colorScheme.onBackground, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = category.description ?: "进入这一类继续挑节目",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
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
private fun FocusAwareEpisodePreviewShelf(
    title: String,
    subtitle: String,
    episodes: List<Episode>,
    onEpisodeClick: (eid: String) -> Unit,
    onOpenAll: () -> Unit,
    rowFocusTargets: RowFocusTargets?
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShelfHeader(
            title = title,
            subtitle = subtitle,
            onOpenAll = onOpenAll,
            actionModifier = Modifier.rowFocusItem(
                isLeadingItem = true,
                targets = rowFocusTargets
            )
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            episodes.forEach { episode ->
                HomeEpisodeCard(
                    episode = episode,
                    modifier = Modifier.rowFocusItem(
                        isLeadingItem = false,
                        targets = rowFocusTargets
                    ),
                    onClick = { onEpisodeClick(episode.eid) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FocusAwareEditorPickShelf(
    day: EditorPickDay,
    onEpisodeClick: (eid: String) -> Unit,
    onOpenAll: () -> Unit,
    rowFocusTargets: RowFocusTargets?
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShelfHeader(
            title = "编辑精选",
            subtitle = if (day.date.isBlank()) "横滑栏模式下只显示当天精选。" else "当前展示 ${day.date} 这一天的精选。",
            onOpenAll = onOpenAll,
            actionModifier = Modifier.rowFocusItem(
                isLeadingItem = true,
                targets = rowFocusTargets
            )
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            day.entries.forEach { entry ->
                val episode = entry.episode ?: return@forEach
                HomeEpisodeCard(
                    episode = episode,
                    note = entry.commentText,
                    modifier = Modifier.rowFocusItem(
                        isLeadingItem = false,
                        targets = rowFocusTargets
                    ),
                    onClick = { onEpisodeClick(episode.eid) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FocusAwareTopListShowcase(
    hotTopEpisodes: List<Episode>,
    rockTopEpisodes: List<Episode>,
    newTopEpisodes: List<Episode>,
    onPodcastClick: (pid: String) -> Unit,
    onEpisodeClick: (eid: String) -> Unit,
    onOpenTopList: (category: String) -> Unit,
    rowFocusTargets: RowFocusTargets?
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionHeading(
            title = "首页榜单",
            subtitle = "三个榜单放在同一个横滑模块里，按下时会直接去下一排的最左边。"
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            FocusAwareTopListBoardCard(
                title = "最热榜",
                subtitle = "24 小时内热度最高的节目。",
                category = "HOT",
                episodes = hotTopEpisodes,
                onPodcastClick = onPodcastClick,
                onEpisodeClick = onEpisodeClick,
                onOpenAll = { onOpenTopList("HOT") },
                rowFocusTargets = rowFocusTargets,
                isLeadingBoard = true
            )
            FocusAwareTopListBoardCard(
                title = "飙升榜",
                subtitle = "最近增长最快、上升最猛的节目。",
                category = "ROCK",
                episodes = rockTopEpisodes,
                onPodcastClick = onPodcastClick,
                onEpisodeClick = onEpisodeClick,
                onOpenAll = { onOpenTopList("ROCK") },
                rowFocusTargets = rowFocusTargets,
                isLeadingBoard = false
            )
            FocusAwareTopListBoardCard(
                title = "新星榜",
                subtitle = "最近冒头的新节目和新面孔。",
                category = "NEW",
                episodes = newTopEpisodes,
                onPodcastClick = onPodcastClick,
                onEpisodeClick = onEpisodeClick,
                onOpenAll = { onOpenTopList("NEW") },
                rowFocusTargets = rowFocusTargets,
                isLeadingBoard = false
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FocusAwareTopListBoardCard(
    title: String,
    subtitle: String,
    category: String,
    episodes: List<Episode>,
    onPodcastClick: (pid: String) -> Unit,
    onEpisodeClick: (eid: String) -> Unit,
    onOpenAll: () -> Unit,
    rowFocusTargets: RowFocusTargets?,
    isLeadingBoard: Boolean
) {
    val visibleEpisodes = remember(episodes) { episodes.take(5) }
    Column(
        modifier = Modifier
            .width(420.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = title, color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                onClick = onOpenAll,
                modifier = Modifier.rowFocusItem(
                    isLeadingItem = isLeadingBoard,
                    targets = rowFocusTargets
                ),
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "查看全部",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
        if (episodes.isEmpty()) {
            TopListShowcaseEmptyCard(title = title)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                visibleEpisodes.forEachIndexed { index, episode ->
                    TopListEntryRow(
                        rank = index + 1,
                        category = category,
                        episode = episode,
                        modifier = Modifier.rowFocusItem(
                            isLeadingItem = false,
                            targets = rowFocusTargets
                        ),
                        onClick = {
                            val podcastId = episode.podcast?.pid.orEmpty()
                            if (podcastId.isNotBlank()) onPodcastClick(podcastId) else onEpisodeClick(episode.eid)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun FocusAwareDiscoverySectionRow(
    section: DiscoverySection,
    onPodcastClick: (pid: String) -> Unit,
    onEpisodeClick: (eid: String) -> Unit,
    rowFocusTargets: RowFocusTargets?
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading(
            title = section.title ?: "推荐内容",
            subtitle = sectionSubtitle(section.title)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            section.items.orEmpty().forEachIndexed { index, item ->
                DiscoveryMediaCard(
                    sectionTitle = section.title.orEmpty(),
                    item = item,
                    index = index,
                    onPodcastClick = onPodcastClick,
                    onEpisodeClick = onEpisodeClick,
                    modifier = Modifier.rowFocusItem(
                        isLeadingItem = index == 0,
                        targets = rowFocusTargets
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HomeHeader(
    profileName: String,
    profileAvatarUrl: String?,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceVariant)
                )
            )
            .padding(horizontal = 30.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "听播客，上小宇宙",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "首页只保留公共发现能力；搜索和个人入口已经收进顶部横条。",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        Text(
            text = "你可以在设置里决定分类探索、新节目广场、编辑精选和首页榜单，是显示在首页顶部按钮区，还是显示成横向浏览模块。",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HeaderActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun AvatarActionButton(
    profileName: String,
    profileAvatarUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            PersonIcon(
                size = 24.dp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HomeTopFeatureRow(
    features: List<HomeFeature>,
    onOpenCategoryHub: () -> Unit,
    onOpenPilotDiscovery: () -> Unit,
    onOpenEditorPicks: () -> Unit,
    onOpenTopLists: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading(
            title = "顶部功能区",
            subtitle = "这里放的是你设定为 Top 栏的公共模块入口。"
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            features.forEach { feature ->
                Surface(
                    onClick = {
                        when (feature) {
                            HomeFeature.CATEGORIES -> onOpenCategoryHub()
                            HomeFeature.PILOT_DISCOVERY -> onOpenPilotDiscovery()
                            HomeFeature.EDITOR_PICKS -> onOpenEditorPicks()
                            HomeFeature.TOP_LISTS -> onOpenTopLists()
                        }
                    },
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = feature.title, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = feature.summary,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
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
fun CategoryShelf(
    categories: List<CategorySummary>,
    onCategoryClick: (categoryId: String, categoryName: String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading(
            title = "分类探索",
            subtitle = "先扫一遍分类，再决定要深入哪条内容线。"
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            categories.forEach { category ->
                Surface(
                    onClick = { onCategoryClick(category.id, category.name) },
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(22.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.primary
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
                    modifier = Modifier.width(244.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "分类",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = category.name,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = category.description ?: "进入这一类继续挑节目",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
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
private fun EpisodePreviewShelf(
    title: String,
    subtitle: String,
    episodes: List<Episode>,
    onEpisodeClick: (eid: String) -> Unit,
    onOpenAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShelfHeader(title = title, subtitle = subtitle, onOpenAll = onOpenAll)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            episodes.forEach { episode ->
                HomeEpisodeCard(
                    episode = episode,
                    onClick = { onEpisodeClick(episode.eid) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EditorPickShelf(
    day: EditorPickDay,
    onEpisodeClick: (eid: String) -> Unit,
    onOpenAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShelfHeader(
            title = "编辑精选",
            subtitle = if (day.date.isBlank()) "横滑栏模式下只显示当天精选。" else "当前展示 ${day.date} 这一天的精选。",
            onOpenAll = onOpenAll
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            day.entries.forEach { entry ->
                val episode = entry.episode ?: return@forEach
                HomeEpisodeCard(
                    episode = episode,
                    note = entry.commentText,
                    onClick = { onEpisodeClick(episode.eid) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopListShelf(
    hotTopEpisodes: List<Episode>,
    rockTopEpisodes: List<Episode>,
    newTopEpisodes: List<Episode>,
    onOpenTopList: (category: String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ShelfHeader(
            title = "首页榜单",
            subtitle = "最热榜、锋芒榜和星星榜都会从单独榜单接口加载。",
            onOpenAll = { onOpenTopList("HOT") }
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopListPreviewCard(
                title = "最热榜",
                description = hotTopEpisodes.firstOrNull()?.title ?: "正在等榜单内容回来",
                count = hotTopEpisodes.size,
                onClick = { onOpenTopList("HOT") }
            )
            TopListPreviewCard(
                title = "锋芒榜",
                description = rockTopEpisodes.firstOrNull()?.title ?: "正在等榜单内容回来",
                count = rockTopEpisodes.size,
                onClick = { onOpenTopList("ROCK") }
            )
            TopListPreviewCard(
                title = "星星榜",
                description = newTopEpisodes.firstOrNull()?.title ?: "正在等榜单内容回来",
                count = newTopEpisodes.size,
                onClick = { onOpenTopList("NEW") }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ShelfHeader(
    title: String,
    subtitle: String,
    onOpenAll: (() -> Unit)? = null,
    actionModifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeading(title = title, subtitle = subtitle)
        if (onOpenAll != null) {
            Surface(
                onClick = onOpenAll,
                modifier = actionModifier,
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "查看全部",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopListPreviewCard(
    title: String,
    description: String,
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.width(264.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = title, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = "当前已拿到 $count 条", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopListShowcase(
    hotTopEpisodes: List<Episode>,
    rockTopEpisodes: List<Episode>,
    newTopEpisodes: List<Episode>,
    onPodcastClick: (pid: String) -> Unit,
    onEpisodeClick: (eid: String) -> Unit,
    onOpenTopList: (category: String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SectionHeading(
            title = "首页榜单",
            subtitle = "三个榜单都放进同一个横滑模块里，每个榜单卡片内部直接列出前几名内容。"
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            TopListBoardCard(
                title = "最热榜",
                subtitle = "24 小时内热度最高的节目。",
                category = "HOT",
                episodes = hotTopEpisodes,
                onPodcastClick = onPodcastClick,
                onEpisodeClick = onEpisodeClick,
                onOpenAll = { onOpenTopList("HOT") }
            )
            TopListBoardCard(
                title = "飙升榜",
                subtitle = "最近增长最快、上升最猛的节目。",
                category = "ROCK",
                episodes = rockTopEpisodes,
                onPodcastClick = onPodcastClick,
                onEpisodeClick = onEpisodeClick,
                onOpenAll = { onOpenTopList("ROCK") }
            )
            TopListBoardCard(
                title = "新星榜",
                subtitle = "最近冒头的新节目和新面孔。",
                category = "NEW",
                episodes = newTopEpisodes,
                onPodcastClick = onPodcastClick,
                onEpisodeClick = onEpisodeClick,
                onOpenAll = { onOpenTopList("NEW") }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopListBoardCard(
    title: String,
    subtitle: String,
    category: String,
    episodes: List<Episode>,
    onPodcastClick: (pid: String) -> Unit,
    onEpisodeClick: (eid: String) -> Unit,
    onOpenAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(396.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                onClick = onOpenAll,
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "查看全部",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }
        if (episodes.isEmpty()) {
            TopListShowcaseEmptyCard(title = title)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                episodes.take(5).forEachIndexed { index, episode ->
                    TopListEntryRow(
                        rank = index + 1,
                        category = category,
                        episode = episode,
                        onClick = {
                            val podcastId = episode.podcast?.pid.orEmpty()
                            if (podcastId.isNotBlank()) {
                                onPodcastClick(podcastId)
                            } else {
                                onEpisodeClick(episode.eid)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopListShowcaseEmptyCard(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(
            text = "这个榜单暂时还没有加载到内容。",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopListEntryRow(
    rank: Int,
    category: String,
    episode: Episode,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "#$rank",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (category) {
                        "HOT" -> "最热榜"
                        "ROCK" -> "飙升榜"
                        else -> "新星榜"
                    },
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = episode.podcast?.title ?: episode.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = episode.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "进入",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HomeEpisodeCard(
    episode: Episode,
    note: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.width(284.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = rememberHomeImageRequest(episode.bestCoverUrl, 360, 280),
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Text(
                text = episode.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = episode.podcast?.title ?: episode.description.orEmpty(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            note?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HomeSectionsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "首页推荐暂时还没加载出来",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "分类、榜单和可配置模块已经先接上了。如果这里还空着，说明 discovery 的某些结构还要继续兼容。",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DiscoverySectionRow(
    section: DiscoverySection,
    onPodcastClick: (pid: String) -> Unit,
    onEpisodeClick: (eid: String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeading(
            title = section.title ?: "推荐内容",
            subtitle = sectionSubtitle(section.title)
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            section.items.orEmpty().forEachIndexed { index, item ->
                DiscoveryMediaCard(
                    sectionTitle = section.title.orEmpty(),
                    item = item,
                    index = index,
                    onPodcastClick = onPodcastClick,
                    onEpisodeClick = onEpisodeClick
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SectionHeading(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun DiscoveryMediaCard(
    sectionTitle: String,
    item: DiscoveryItem,
    index: Int,
    onPodcastClick: (pid: String) -> Unit,
    onEpisodeClick: (eid: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val episode = item.episode
    val podcast = item.podcast ?: episode?.podcast ?: return
    val title = episode?.title ?: podcast.title
    val subtitle = episode?.podcast?.title ?: podcast.author.orEmpty()
    val coverUrl = episode?.bestCoverUrl ?: podcast.bestCoverUrl.orEmpty()
    val badge = if (episode != null) "单集" else "节目"
    val showRank = sectionTitle.contains("榜") || sectionTitle.contains("排行")
    val clickAction = {
        if (episode != null) onEpisodeClick(episode.eid) else onPodcastClick(podcast.pid)
    }

    Surface(
        onClick = clickAction,
        modifier = modifier.width(236.dp),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(24.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box {
                AsyncImage(
                    model = rememberHomeImageRequest(coverUrl, 280, 280),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                if (showRank) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.56f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (episode != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = badge,
                        color = if (episode != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle.ifBlank { "打开查看详情" },
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PodcastCard(
    podcast: Podcast,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(24.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        modifier = modifier.width(236.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = rememberHomeImageRequest(podcast.bestCoverUrl, 280, 280),
                contentDescription = podcast.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = podcast.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = podcast.author ?: "",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun sectionSubtitle(title: String?): String {
    val text = title.orEmpty()
    return when {
        text.contains("编辑精选") -> "把编辑挑过的内容单独排出来，更适合在电视上按排浏览。"
        text.contains("为你精选") -> "偏个性化推荐，适合先扫封面，再决定要不要点开。"
        text.contains("榜") -> "榜单模块改成横向轨道，左右切换时更容易形成记忆。"
        text.contains("大家都在听") -> "先看一眼最近大家都在听什么，再决定要不要深入。"
        else -> "保持一眼能扫完的电视浏览节奏，先看封面，再看标题。"
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CenterMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CenterError(
    message: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
            Surface(
                onClick = onClick,
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(18.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = buttonText,
                    color = Color.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)
                )
            }
        }
    }
}
