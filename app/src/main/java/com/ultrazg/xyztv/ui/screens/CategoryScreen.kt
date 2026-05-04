package com.ultrazg.xyztv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ultrazg.xyztv.ui.viewmodel.CategoryViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoryScreen(
    categoryId: String,
    categoryName: String,
    onBack: () -> Unit,
    onPodcastClick: (pid: String) -> Unit
) {
    val viewModel: CategoryViewModel = viewModel()
    val firstPodcastFocusRequester = remember { FocusRequester() }

    LaunchedEffect(categoryId) {
        viewModel.load(categoryId)
    }

    LaunchedEffect(viewModel.podcasts.size, viewModel.selectedTab) {
        if (viewModel.podcasts.isNotEmpty()) {
            withFrameNanos { }
            runCatching {
                firstPodcastFocusRequester.requestFocus()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = categoryName,
                    color = Color(0xFF020303),
                    fontSize = 34.sp,
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "Explore podcasts by category tag",
                    color = Color(0xFFB0BEC5),
                    fontSize = 14.sp
                )
            }
            Surface(
                onClick = onBack,
                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Back",
                    color = Color(0xFF020303),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (viewModel.tabs.isNotEmpty()) {
            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.tabs.size) { index ->
                    val tab = viewModel.tabs[index]
                    val selected = tab.value == viewModel.selectedTab
                    Surface(
                        onClick = { viewModel.selectTab(categoryId, tab.value) },
                        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.focusProperties {
                            down = firstPodcastFocusRequester
                        }
                    ) {
                        Text(
                            text = tab.label,
                            color = if (selected) Color.Black else Color(0xFF020303),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        when {
            viewModel.isLoading && viewModel.podcasts.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Loading category...", color = Color(0xFF020303), fontSize = 22.sp)
                }
            }

            viewModel.error != null && viewModel.podcasts.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Failed to load category: ${viewModel.error}", color = Color.Red, fontSize = 18.sp)
                }
            }

            else -> {
                TvLazyVerticalGrid(
                    columns = TvGridCells.Fixed(5),
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.podcasts.size) { index ->
                        val podcast = viewModel.podcasts[index]
                        var focused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .then(
                                    if (index == 0) Modifier.focusRequester(firstPodcastFocusRequester) else Modifier
                                )
                                .onFocusChanged { focused = it.isFocused || it.hasFocus }
                                .onKeyEvent { event ->
                                    if (
                                        event.type == KeyEventType.KeyDown &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter)
                                    ) {
                                        onPodcastClick(podcast.pid)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .focusable()
                                .border(
                                    width = if (focused) 3.dp else 0.dp,
                                    color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(28.dp)
                                )
                                .padding(3.dp)
                        ) {
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
}
