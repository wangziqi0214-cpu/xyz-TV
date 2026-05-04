package com.ultrazg.xyztv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface

data class SideRailItem(
    val label: String,
    val route: String,
    val icon: @Composable (color: Color) -> Unit
)

val defaultSideRailItems = listOf(
    SideRailItem("首页", "home") { HomeIcon(color = it) },
    SideRailItem("搜索", "search") { SearchIcon(color = it) },
    SideRailItem("订阅", "subscriptions") { SubscribeIcon(color = it) },
    SideRailItem("分类", "categories") { CategoryIcon(color = it) },
    SideRailItem("历史", "history") { HistoryIcon(color = it) },
    SideRailItem("我的", "profile") { PersonIcon(color = it) },
    SideRailItem("设置", "app-settings") { SettingsIcon(color = it) }
)

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SideRail(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    items: List<SideRailItem> = defaultSideRailItems
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route || currentRoute.startsWith(item.route)
            val iconColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Surface(
                onClick = { onNavigate(item.route) },
                modifier = Modifier.size(44.dp),
                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    item.icon(iconColor)
                }
            }
        }
    }
}
