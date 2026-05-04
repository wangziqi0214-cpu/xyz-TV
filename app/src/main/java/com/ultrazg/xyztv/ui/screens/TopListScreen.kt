package com.ultrazg.xyztv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ultrazg.xyztv.ui.viewmodel.TopListViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopListScreen(
    initialCategory: String = "HOT",
    onBack: () -> Unit,
    onEpisodeClick: (eid: String) -> Unit
) {
    val viewModel: TopListViewModel = viewModel()
    val category = viewModel.category
    val episodes = viewModel.episodes
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    LaunchedEffect(initialCategory) {
        viewModel.load(initialCategory)
    }

    TvLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
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
                    text = "首页榜单",
                    color = Color(0xFF020303),
                    fontSize = 32.sp,
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "这里集中展示最热榜、锋芒榜和星星榜。",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TopCategoryButton("最热榜", category == "HOT") { viewModel.load("HOT") }
                    TopCategoryButton("锋芒榜", category == "ROCK") { viewModel.load("ROCK") }
                    TopCategoryButton("星星榜", category == "NEW") { viewModel.load("NEW") }
                }
            }
        }

        when {
            isLoading -> item { CenterMessage("正在加载榜单...") }
            error != null -> item { Text(text = "加载失败：$error", color = Color(0xFFFFB74D), fontSize = 16.sp) }
            episodes.isEmpty() -> item { Text(text = "暂时没有榜单内容", color = Color(0xFF3A3D42), fontSize = 16.sp) }
            else -> {
                items(episodes.size) { index ->
                    EpisodeRow(episode = episodes[index], onClick = { onEpisodeClick(episodes[index].eid) })
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopCategoryButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else Color(0xFF020303),
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}
