package com.ultrazg.xyztv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.EmbeddedBackendManager
import com.ultrazg.xyztv.data.HomeLayoutManager
import com.ultrazg.xyztv.data.TokenManager
import com.ultrazg.xyztv.data.playback.AppPlaybackController
import com.ultrazg.xyztv.ui.components.PersonIcon
import com.ultrazg.xyztv.ui.components.SideRail
import com.ultrazg.xyztv.ui.screens.AppSettingsScreen
import com.ultrazg.xyztv.ui.screens.CategoryHubScreen
import com.ultrazg.xyztv.ui.screens.HomeScreen
import com.ultrazg.xyztv.ui.screens.LoginScreen
import com.ultrazg.xyztv.ui.screens.PairingLoginScreen
import com.ultrazg.xyztv.ui.screens.PlayerScreen
import com.ultrazg.xyztv.ui.screens.PodcastDetailScreen
import com.ultrazg.xyztv.ui.screens.CategoryScreen
import com.ultrazg.xyztv.ui.screens.CollectedCommentScreen
import com.ultrazg.xyztv.ui.screens.CommentThreadScreen
import com.ultrazg.xyztv.ui.screens.EpisodeDetailScreen
import com.ultrazg.xyztv.ui.screens.EpisodeCommentScreen
import com.ultrazg.xyztv.ui.screens.EditorPickHistoryScreen
import com.ultrazg.xyztv.ui.screens.FavoriteScreen
import com.ultrazg.xyztv.ui.screens.BlockedUserScreen
import com.ultrazg.xyztv.ui.screens.FollowerScreen
import com.ultrazg.xyztv.ui.screens.FollowingScreen
import com.ultrazg.xyztv.ui.screens.HistoryScreen
import com.ultrazg.xyztv.ui.screens.InboxScreen
import com.ultrazg.xyztv.ui.screens.MileageScreen
import com.ultrazg.xyztv.ui.screens.NonStarredSubscriptionScreen
import com.ultrazg.xyztv.ui.screens.PickHistoryScreen
import com.ultrazg.xyztv.ui.screens.PilotDiscoveryScreen
import com.ultrazg.xyztv.ui.screens.PreferenceScreen
import com.ultrazg.xyztv.ui.screens.ProfileScreen
import com.ultrazg.xyztv.ui.screens.SearchScreen
import com.ultrazg.xyztv.ui.screens.StickerScreen
import com.ultrazg.xyztv.ui.screens.StarSubscriptionScreen
import com.ultrazg.xyztv.ui.screens.SubscriptionScreen
import com.ultrazg.xyztv.ui.screens.TopListScreen
import com.ultrazg.xyztv.ui.screens.WebLoginScreen
import com.ultrazg.xyztv.ui.theme.XyzTvTheme

@Composable
private fun LogNavigation(route: String) {
    LaunchedEffect(route) {
        AppLogger.action("navigate", route)
    }
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppLogger.init(this)
        AppLogger.info("main", "MainActivity created")
        TokenManager.init(this)
        HomeLayoutManager.init(this)
        com.ultrazg.xyztv.data.DarkModeManager.init(this)
        AppLogger.info("main", "TokenManager initialized")
        EmbeddedBackendManager.start()
        AppLogger.info("main", "Embedded backend start requested")
        val debugStartRoute = intent.getStringExtra("eid")
            ?.takeIf { it.isNotBlank() }
            ?.let { "player/$it" }
            ?: intent.getStringExtra("route")
            ?.takeIf { it.startsWith("home") || it.startsWith("episode/") || it.startsWith("player/") }

        setContent {
            XyzTvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val navController = rememberNavController()
                    val isLoggedIn by TokenManager.loginState
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route.orEmpty()

                    val showSideRail = isLoggedIn &&
                        currentRoute != "login" &&
                        currentRoute != "web-login" &&
                        currentRoute != "pair-login" &&
                        !currentRoute.startsWith("player/")

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        if (showSideRail) {
                            SideRail(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    AppLogger.action("rail_navigate", route)
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            GlobalTopBar(
                                currentRoute = currentRoute,
                                isLoggedIn = isLoggedIn,
                                onOpenPlayer = { eid -> navController.navigate("player/${android.net.Uri.encode(eid)}") },
                                onOpenSearch = { navController.navigate("search") },
                                onOpenProfile = { navController.navigate("profile") }
                            )

                            NavHost(
                                navController = navController,
                                startDestination = debugStartRoute ?: if (isLoggedIn) "home" else "login",
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                        composable("login") {
                            AppLogger.action("navigate", "login")
                            LoginScreen(
                                onLoginSuccess = {
                                    AppLogger.action("login_success", "navigate to home")
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToWebLogin = {
                                    AppLogger.action("navigate", "web-login")
                                    navController.navigate("web-login")
                                },
                                onNavigateToPairLogin = {
                                    AppLogger.action("navigate", "pair-login")
                                    navController.navigate("pair-login")
                                }
                            )
                        }

                        composable("web-login") {
                            AppLogger.action("navigate", "web-login-screen")
                            WebLoginScreen(
                                onBack = { navController.popBackStack() },
                                onLoginSuccess = {
                                    AppLogger.action("web_login_success", "navigate to home")
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("pair-login") {
                            AppLogger.action("navigate", "pair-login-screen")
                            PairingLoginScreen(
                                onBack = { navController.popBackStack() },
                                onPaired = {
                                    AppLogger.action("pairing_success", "navigate to home")
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            LogNavigation("home")
                            HomeScreen(
                                onNavigateToPodcast = { pid ->
                                    AppLogger.action("open_podcast", pid)
                                    navController.navigate("podcast/$pid")
                                },
                                onNavigateToEpisode = { eid ->
                                    AppLogger.action("open_episode", eid)
                                    navController.navigate("episode/$eid")
                                },
                                onNavigateToCategory = { categoryId, categoryName ->
                                    AppLogger.action("open_category", "$categoryId|$categoryName")
                                    navController.navigate("category/$categoryId/${android.net.Uri.encode(categoryName)}")
                                },
                                onNavigateToCategoryHub = {
                                    AppLogger.action("navigate", "categories")
                                    navController.navigate("categories")
                                },
                                onNavigateToSearch = {
                                    AppLogger.action("navigate", "search")
                                    navController.navigate("search")
                                },
                                onNavigateToSettings = {
                                    AppLogger.action("navigate", "app-settings")
                                    navController.navigate("app-settings")
                                },
                                onNavigateToTopList = { category ->
                                    AppLogger.action("navigate", "top-list/$category")
                                    navController.navigate("top-list/$category")
                                },
                                onNavigateToProfile = {
                                    AppLogger.action("navigate", "profile")
                                    navController.navigate("profile")
                                },
                                onNavigateToPilotDiscovery = {
                                    AppLogger.action("navigate", "pilot-discovery")
                                    navController.navigate("pilot-discovery")
                                },
                                onNavigateToEditorPickHistory = {
                                    AppLogger.action("navigate", "editor-picks")
                                    navController.navigate("editor-picks")
                                },
                                onNavigateToLogin = {
                                    AppLogger.action("logout", "clear token and navigate to login")
                                    TokenManager.clear()
                                    navController.navigate("login") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("app-settings") {
                            AppLogger.action("navigate", "app-settings-screen")
                            AppSettingsScreen(onBack = { navController.popBackStack() })
                        }

                        composable("categories") {
                            AppLogger.action("navigate", "categories-screen")
                            CategoryHubScreen(
                                onBack = { navController.popBackStack() },
                                onCategoryClick = { categoryId, categoryName ->
                                    AppLogger.action("open_category", "$categoryId|$categoryName")
                                    navController.navigate("category/$categoryId/${android.net.Uri.encode(categoryName)}")
                                }
                            )
                        }

                        composable("search") {
                            AppLogger.action("navigate", "search-screen")
                            SearchScreen(
                                onBack = { navController.popBackStack() },
                                onPodcastClick = { pid ->
                                    AppLogger.action("search_open_podcast", pid)
                                    navController.navigate("podcast/$pid")
                                },
                                onEpisodeClick = { eid ->
                                    AppLogger.action("search_open_episode", eid)
                                    navController.navigate("episode/$eid")
                                }
                            )
                        }

                        composable("subscriptions") {
                            AppLogger.action("navigate", "subscriptions-screen")
                            SubscriptionScreen(
                                onBack = { navController.popBackStack() },
                                onPodcastClick = { pid ->
                                    AppLogger.action("subscription_open_podcast", pid)
                                    navController.navigate("podcast/$pid")
                                }
                            )
                        }

                        composable("favorites") {
                            AppLogger.action("navigate", "favorites-screen")
                            FavoriteScreen(
                                onBack = { navController.popBackStack() },
                                onEpisodeClick = { eid ->
                                    AppLogger.action("favorite_open_episode", eid)
                                    navController.navigate("episode/$eid")
                                }
                            )
                        }

                        composable("history") {
                            AppLogger.action("navigate", "history-screen")
                            HistoryScreen(
                                onBack = { navController.popBackStack() },
                                onEpisodeClick = { eid ->
                                    AppLogger.action("history_open_episode", eid)
                                    navController.navigate("episode/$eid")
                                }
                            )
                        }

                        composable("top-list") {
                            AppLogger.action("navigate", "top-list-screen")
                            TopListScreen(
                                onBack = { navController.popBackStack() },
                                onEpisodeClick = { eid ->
                                    AppLogger.action("top_list_open_episode", eid)
                                    navController.navigate("episode/$eid")
                                }
                            )
                        }

                        composable("top-list/{category}") { backStackEntry ->
                            val category = backStackEntry.arguments?.getString("category") ?: "HOT"
                            AppLogger.action("navigate", "top-list-screen/$category")
                            TopListScreen(
                                initialCategory = category,
                                onBack = { navController.popBackStack() },
                                onEpisodeClick = { eid ->
                                    AppLogger.action("top_list_open_episode", eid)
                                    navController.navigate("episode/$eid")
                                }
                            )
                        }

                        composable("profile") {
                            AppLogger.action("navigate", "profile-screen")
                            ProfileScreen(
                                onBack = { navController.popBackStack() },
                                onPodcastClick = { pid ->
                                    AppLogger.action("profile_open_podcast", pid)
                                    navController.navigate("podcast/$pid")
                                },
                                onEpisodeClick = { eid ->
                                    AppLogger.action("profile_open_pick_episode", eid)
                                    navController.navigate("episode/$eid")
                                },
                                onNavigateToSubscriptions = {
                                    AppLogger.action("navigate", "subscriptions")
                                    navController.navigate("subscriptions")
                                },
                                onNavigateToFavorites = {
                                    AppLogger.action("navigate", "favorites")
                                    navController.navigate("favorites")
                                },
                                onNavigateToHistory = {
                                    AppLogger.action("navigate", "history")
                                    navController.navigate("history")
                                },
                                onNavigateToInbox = {
                                    AppLogger.action("navigate", "inbox")
                                    navController.navigate("inbox")
                                },
                                onNavigateToPickHistory = {
                                    AppLogger.action("navigate", "pick-history")
                                    navController.navigate("pick-history")
                                },
                                onNavigateToFollowing = {
                                    AppLogger.action("navigate", "following")
                                    navController.navigate("following")
                                },
                                onNavigateToFollowers = {
                                    AppLogger.action("navigate", "followers")
                                    navController.navigate("followers")
                                },
                                onNavigateToStarSubscriptions = {
                                    AppLogger.action("navigate", "star-subscriptions")
                                    navController.navigate("star-subscriptions")
                                },
                                onNavigateToNonStarredSubscriptions = {
                                    AppLogger.action("navigate", "non-star-subscriptions")
                                    navController.navigate("non-star-subscriptions")
                                },
                                onNavigateToPreferences = {
                                    AppLogger.action("navigate", "preferences")
                                    navController.navigate("preferences")
                                },
                                onNavigateToCollectedComments = {
                                    AppLogger.action("navigate", "collected-comments")
                                    navController.navigate("collected-comments")
                                },
                                onNavigateToMileage = {
                                    AppLogger.action("navigate", "mileage")
                                    navController.navigate("mileage")
                                },
                                onNavigateToStickers = {
                                    AppLogger.action("navigate", "stickers")
                                    navController.navigate("stickers")
                                },
                                onNavigateToBlockedUsers = {
                                    AppLogger.action("navigate", "blocked-users")
                                    navController.navigate("blocked-users")
                                }
                            )
                        }

                        composable("inbox") {
                            AppLogger.action("navigate", "inbox-screen")
                            InboxScreen(
                                onBack = { navController.popBackStack() },
                                onEpisodeClick = { eid ->
                                    AppLogger.action("inbox_open_episode", eid)
                                    navController.navigate("episode/$eid")
                                }
                            )
                        }

                        composable("pilot-discovery") {
                            AppLogger.action("navigate", "pilot-discovery-screen")
                            PilotDiscoveryScreen(
                                onBack = { navController.popBackStack() },
                                onEpisodeClick = { eid ->
                                    AppLogger.action("pilot_discovery_open_episode", eid)
                                    navController.navigate("episode/$eid")
                                }
                            )
                        }

                        composable("editor-picks") {
                            AppLogger.action("navigate", "editor-picks-screen")
                            EditorPickHistoryScreen(
                                onBack = { navController.popBackStack() },
                                onEpisodeClick = { eid ->
                                    AppLogger.action("editor_pick_open_episode", eid)
                                    navController.navigate("episode/$eid")
                                }
                            )
                        }

                        composable("pick-history") {
                            AppLogger.action("navigate", "pick-history-screen")
                            PickHistoryScreen(
                                onBack = { navController.popBackStack() },
                                onEpisodeClick = { eid ->
                                    AppLogger.action("pick_history_open_episode", eid)
                                    navController.navigate("episode/$eid")
                                }
                            )
                        }

                        composable("following") {
                            AppLogger.action("navigate", "following-screen")
                            FollowingScreen(onBack = { navController.popBackStack() })
                        }

                        composable("followers") {
                            AppLogger.action("navigate", "followers-screen")
                            FollowerScreen(onBack = { navController.popBackStack() })
                        }

                        composable("star-subscriptions") {
                            AppLogger.action("navigate", "star-subscriptions-screen")
                            StarSubscriptionScreen(
                                onBack = { navController.popBackStack() },
                                onPodcastClick = { pid ->
                                    AppLogger.action("star_subscription_open_podcast", pid)
                                    navController.navigate("podcast/$pid")
                                }
                            )
                        }

                        composable("non-star-subscriptions") {
                            AppLogger.action("navigate", "non-star-subscriptions-screen")
                            NonStarredSubscriptionScreen(
                                onBack = { navController.popBackStack() },
                                onPodcastClick = { pid ->
                                    AppLogger.action("non_star_subscription_open_podcast", pid)
                                    navController.navigate("podcast/$pid")
                                }
                            )
                        }

                        composable("preferences") {
                            AppLogger.action("navigate", "preferences-screen")
                            PreferenceScreen(onBack = { navController.popBackStack() })
                        }

                        composable("collected-comments") {
                            AppLogger.action("navigate", "collected-comments-screen")
                            CollectedCommentScreen(
                                onBack = { navController.popBackStack() },
                                onEpisodeClick = { eid ->
                                    AppLogger.action("collected_comment_open_episode", eid)
                                    navController.navigate("episode/$eid")
                                }
                            )
                        }

                        composable("mileage") {
                            AppLogger.action("navigate", "mileage-screen")
                            MileageScreen(
                                onBack = { navController.popBackStack() },
                                onPodcastClick = { pid ->
                                    AppLogger.action("mileage_open_podcast", pid)
                                    navController.navigate("podcast/$pid")
                                }
                            )
                        }

                        composable("stickers") {
                            AppLogger.action("navigate", "stickers-screen")
                            StickerScreen(onBack = { navController.popBackStack() })
                        }

                        composable("blocked-users") {
                            AppLogger.action("navigate", "blocked-users-screen")
                            BlockedUserScreen(onBack = { navController.popBackStack() })
                        }

                        composable("episode-comments/{eid}") { backStackEntry ->
                            val eid = backStackEntry.arguments?.getString("eid") ?: ""
                            AppLogger.action("navigate", "episode-comments/$eid")
                            EpisodeCommentScreen(
                                eid = eid,
                                onBack = { navController.popBackStack() },
                                onOpenThread = { commentId, ownerId ->
                                    AppLogger.action("navigate", "comment-thread/$commentId")
                                    navController.navigate("comment-thread/$ownerId/$commentId")
                                }
                            )
                        }

                        composable("comment-thread/{ownerId}/{commentId}") { backStackEntry ->
                            val ownerId = backStackEntry.arguments?.getString("ownerId") ?: ""
                            val commentId = backStackEntry.arguments?.getString("commentId") ?: ""
                            AppLogger.action("navigate", "comment-thread/$commentId")
                            CommentThreadScreen(
                                ownerId = ownerId,
                                primaryCommentId = commentId,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("category/{categoryId}/{categoryName}") { backStackEntry ->
                            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
                            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                            AppLogger.action("navigate", "category/$categoryId")
                            CategoryScreen(
                                categoryId = categoryId,
                                categoryName = categoryName,
                                onBack = { navController.popBackStack() },
                                onPodcastClick = { pid ->
                                    AppLogger.action("category_open_podcast", pid)
                                    navController.navigate("podcast/$pid")
                                }
                            )
                        }

                        composable("podcast/{pid}") { backStackEntry ->
                            val pid = backStackEntry.arguments?.getString("pid") ?: ""
                            AppLogger.action("navigate", "podcast/$pid")
                            PodcastDetailScreen(
                                pid = pid,
                                onBack = { navController.popBackStack() },
                                onEpisodeClick = { eid ->
                                    AppLogger.action("open_episode", eid)
                                    navController.navigate("episode/$eid")
                                },
                                onPodcastClick = { relatedPid ->
                                    AppLogger.action("open_related_podcast", relatedPid)
                                    navController.navigate("podcast/$relatedPid")
                                }
                            )
                        }

                        composable("episode/{eid}") { backStackEntry ->
                            val eid = backStackEntry.arguments?.getString("eid") ?: ""
                            AppLogger.action("navigate", "episode/$eid")
                            if (eid.isBlank()) {
                                com.ultrazg.xyztv.ui.screens.CenterMessage("缺少单集 ID")
                            } else {
                                EpisodeDetailScreen(
                                    eid = eid,
                                    onBack = { navController.popBackStack() },
                                    onPlay = {
                                        AppLogger.action("play_episode", eid)
                                        navController.navigate("player/${android.net.Uri.encode(eid)}")
                                    },
                                    onNavigateToPodcast = { pid ->
                                        AppLogger.action("episode_open_podcast", pid)
                                        navController.navigate("podcast/$pid")
                                    }
                                )
                            }
                        }

                        composable("player/{eid}") { backStackEntry ->
                            val eid = backStackEntry.arguments?.getString("eid") ?: ""
                            AppLogger.action("navigate", "player/$eid")
                            if (eid.isBlank()) {
                                com.ultrazg.xyztv.ui.screens.CenterMessage("缺少单集 ID")
                            } else {
                                PlayerScreen(
                                    eid = eid,
                                    onBack = { navController.popBackStack() },
                                    onOpenDetail = { detailEid ->
                                        AppLogger.action("navigate", "episode/$detailEid")
                                        navController.navigate("episode/${android.net.Uri.encode(detailEid)}") {
                                            launchSingleTop = true
                                        }
                                    },
                                    onOpenComments = { commentEid ->
                                        AppLogger.action("navigate", "episode-comments/$commentEid")
                                        navController.navigate("episode-comments/${android.net.Uri.encode(commentEid)}")
                                    }
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
private fun GlobalTopBar(
    currentRoute: String,
    isLoggedIn: Boolean,
    onOpenPlayer: (eid: String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenProfile: () -> Unit
) {
    if (!isLoggedIn) return
    if (currentRoute.startsWith("player/")) return
    if (currentRoute == "login" || currentRoute == "web-login" || currentRoute == "pair-login") return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 28.dp, end = 34.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaybackOrb(onOpenPlayer = onOpenPlayer)
        Box(modifier = Modifier.weight(1f))
        TopBarButton(text = "搜索", onClick = onOpenSearch)
        TopBarAvatarButton(onClick = onOpenProfile)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PlaybackOrb(
    onOpenPlayer: (eid: String) -> Unit
) {
    val episode = AppPlaybackController.episode
    if (episode == null) {
        Box(modifier = Modifier.size(50.dp))
        return
    }
    val position = AppPlaybackController.currentPositionMs
    val duration = AppPlaybackController.durationMs
    val progress = if (duration > 0L) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    val progressColor = MaterialTheme.colorScheme.primary

    Surface(
        onClick = { onOpenPlayer(episode.eid) },
        modifier = Modifier.size(50.dp),
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()
                drawCircle(
                    color = Color(0xFFD8DADE).copy(alpha = 0.7f),
                    radius = (size.minDimension - strokeWidth) / 2f,
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            AsyncImage(
                model = episode.bestCoverUrl.orEmpty(),
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopBarButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Box(
            modifier = Modifier
                .height(44.dp)
                .width(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopBarAvatarButton(
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            PersonIcon(
                size = 22.dp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
}
