# SmartTube 风格全局 UI/UX 重设计实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标:** 参考 SmartTube 重新设计 XYZ TV 全局 UI/UX，采用浅色主题 + 青绿主色，保留所有现有功能键。

**架构:** 左侧 Rail 导航 + 内容货架行布局，统一卡片组件和焦点系统，全页面视觉升级但功能保持不变。

**技术栈:** Jetpack Compose TV (`androidx.tv.material3`), Kotlin, Coil, Navigation Compose

---

## 文件结构

| 文件 | 职责 |
|------|------|
| `ui/theme/Color.kt` | 新色彩系统定义 |
| `ui/theme/Theme.kt` | 主题更新，使用新 ColorScheme |
| `ui/components/TvCard.kt` | 播客卡片 + 单集卡片组件 |
| `ui/components/TvButton.kt` | ControlButton, ControlTextButton, 按钮变体 |
| `ui/components/ContentShelf.kt` | 货架行组件 |
| `ui/components/SideRail.kt` | 侧边 Rail 导航组件 |
| `ui/components/FocusModifiers.kt` | 焦点缩放 + 阴影修饰符 |
| `MainActivity.kt` | 重构为 Rail + Content 布局 |
| `HomeScreen.kt` | 重写为货架行布局 |
| `PlayerScreen.kt` | 视觉更新，保留所有功能键 |
| `SearchScreen.kt` | 视觉更新，保留搜索功能 |
| `SubscriptionsScreen.kt` | 视觉更新 |
| `CategoryHubScreen.kt` | 视觉更新 |
| `HistoryScreen.kt` | 视觉更新 |
| `ProfileScreen.kt` | 视觉更新，保留所有入口 |
| `SettingsScreen.kt` | 视觉更新 |
| `LoginScreen.kt` | 视觉更新，保留所有登录方式 |
| `WebLoginScreen.kt` | 视觉更新，保留所有控制按钮 |

---

## 阶段 1: 设计系统

### 任务 1: 创建新色彩系统

**文件：**
- 创建：`ui/theme/Color.kt`
- 修改：`ui/theme/Theme.kt`

**步骤：**

- [ ] **步骤 1: 创建 Color.kt 定义新色板**

```kotlin
package com.ultrazg.xyztv.ui.theme

import androidx.compose.ui.graphics.Color

// 浅色主题色板 (用户自定义)
val BackgroundLight = Color(0xFFFFFFFF)      // #FFFFFF 背景
val SurfaceLight = Color(0xFFFAFAFB)         // #FAFAFB Surface
val SurfaceVariantLight = Color(0xFFEDEEF1)  // #EDEEF1 卡片底
val PrimaryLight = Color(0xFF62BBD2)         // #62BBD2 主色
val PrimaryLightLight = Color(0xFF78C2D2)    // #78C2D2 主色亮
val OnBackgroundLight = Color(0xFF020303)     // #020303 主文字
val OnSurfaceLight = Color(0xFF3A3D42)      // #3A3D42 副文字
val OutlineLight = Color(0xFFD8DADE)        // #D8DADE 边框

// 暗色主题保留兼容
val BackgroundDark = Color(0xFF020303)
val SurfaceDark = Color(0xFF0E1214)
val SurfaceVariantDark = Color(0xFF1A1F22)
val PrimaryDark = Color(0xFF62BBD2)
val OnBackgroundDark = Color(0xFFFFFFFF)
val OnSurfaceDark = Color(0xFFFAFAFB)
```

- [ ] **步骤 2: Commit 颜色定义**

```bash
git add app/src/main/java/com/ultrazg/xyztv/ui/theme/Color.kt
git commit -m "feat: add light theme color palette"
```

- [ ] **步骤 3: 更新 Theme.kt 使用新 ColorScheme**

```kotlin
// 在 Theme.kt 中
@OptIn(ExperimentalTvMaterial3Api::class)
private val LightColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    secondary = PrimaryLightLight,
    tertiary = PrimaryLightLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = OnBackgroundLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceLight,
    outline = OutlineLight
)
```

- [ ] **步骤 4: 修改 XyzTvTheme 支持浅色主题**

```kotlin
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun XyzTvTheme(
    darkTheme: Boolean = false, // 默认浅色
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

- [ ] **步骤 5: Commit 主题更新**

```bash
git add app/src/main/java/com/ultrazg/xyztv/ui/theme/Theme.kt
git commit -m "feat: update theme to use light color scheme by default"
```

---

### 任务 2: 创建焦点修饰符

**文件：**
- 创建：`ui/components/FocusModifiers.kt`

- [ ] **步骤 1: 创建 FocusModifiers.kt**

```kotlin
package com.ultrazg.xyztv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme

@OptIn(ExperimentalTvMaterial3Api::class)
fun Modifier.tvFocusable(
    scale: Float = 1.06f,
    enabled: Boolean = true
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val scaleAnimated by animateFloatAsState(
        targetValue = if (isFocused && enabled) scale else 1f,
        label = "focus_scale"
    )
    
    this
        .scale(scaleAnimated)
        .focusable(
            interactionSource = interactionSource,
            enabled = enabled
        )
}

@OptIn(ExperimentalTvMaterial3Api::class)
fun Modifier.tvFocusableWithShadow(
    scale: Float = 1.06f,
    shadowAlpha: Float = 0.22f,
    enabled: Boolean = true
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val scaleAnimated by animateFloatAsState(
        targetValue = if (isFocused && enabled) scale else 1f,
        label = "focus_scale"
    )
    
    val shadowColor = MaterialTheme.colorScheme.primary
    
    this
        .graphicsLayer {
            this.scaleX = scaleAnimated
            this.scaleY = scaleAnimated
            if (isFocused) {
                shadowElevation = 12f
            }
        }
        .focusable(
            interactionSource = interactionSource,
            enabled = enabled
        )
}
```

- [ ] **步骤 2: Commit 焦点修饰符**

```bash
git add app/src/main/java/com/ultrazg/xyztv/ui/components/FocusModifiers.kt
git commit -m "feat: add tv focus modifiers with scale animation"
```

---

### 任务 3: 创建按钮组件

**文件：**
- 创建：`ui/components/TvButton.kt`

- [ ] **步骤 1: 创建 TvButton.kt 基础结构**

```kotlin
package com.ultrazg.xyztv.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvControlButton(
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: TextUnit = 18.sp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = iconSize,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvControlTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 92.dp,
    height: Dp = 42.dp
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(width, height),
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
```

- [ ] **步骤 2: Commit 按钮组件**

```bash
git add app/src/main/java/com/ultrazg/xyztv/ui/components/TvButton.kt
git commit -m "feat: add TvControlButton and TvControlTextButton components"
```

---

### 任务 4: 创建卡片组件

**文件：**
- 创建：`ui/components/TvCard.kt`

- [ ] **步骤 1: 创建播客卡片组件**

```kotlin
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PodcastCard(
    podcast: Podcast,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        label = "podcast_scale"
    )
    
    Column(
        modifier = modifier
            .width(146.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusable(interactionSource = interactionSource),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = podcast.bestCoverUrl,
            contentDescription = podcast.title,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = podcast.title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${podcast.subscriptionCount} 订阅",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
```

- [ ] **步骤 2: 创建单集卡片组件**

```kotlin
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeWideCard(
    episode: Episode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        label = "episode_scale"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .focusable(interactionSource = interactionSource)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = episode.bestCoverUrl,
            contentDescription = episode.title,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = episode.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Text(
                text = "${episode.podcast?.title} · ${episode.duration} · ${episode.pubDate}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            // 进度条
            LinearProgressIndicator(
                progress = episode.progress,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth(0.6f)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
```

- [ ] **步骤 3: Commit 卡片组件**

```bash
git add app/src/main/java/com/ultrazg/xyztv/ui/components/TvCard.kt
git commit -m "feat: add PodcastCard and EpisodeWideCard components"
```

---

### 任务 5: 创建货架行组件

**文件：**
- 创建：`ui/components/ContentShelf.kt`

- [ ] **步骤 1: 创建 ContentShelf 组件**

```kotlin
package com.ultrazg.xyztv.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ContentShelf(
    title: String,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                onClick = onViewAll,
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "查看全部 →",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        content()
    }
}
```

- [ ] **步骤 2: Commit 货架行组件**

```bash
git commit -m "feat: add ContentShelf component"
```

---

## 阶段 2: Shell 重构

### 任务 6: 重构 MainActivity 布局

**文件：**
- 修改：`MainActivity.kt`

- [ ] **步骤 1: 在 MainActivity 顶部添加 Rail 入口定义**

```kotlin
// 在 MainActivity.kt 中添加
private data class RailItem(
    val icon: String,
    val label: String,
    val route: String
)

private val railItems = listOf(
    RailItem("🏠", "首页", "home"),
    RailItem("🔍", "搜索", "search"),
    RailItem("📥", "订阅", "subscriptions"),
    RailItem("🏷", "分类", "categories"),
    RailItem("🕐", "历史", "history"),
    RailItem("👤", "我的", "profile"),
    RailItem("⚙️", "设置", "settings")
)
```

- [ ] **步骤 2: 创建 SideRail 可组合函数**

```kotlin
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SideRail(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
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
        railItems.forEach { item ->
            val selected = currentRoute == item.route
            Surface(
                onClick = { onNavigate(item.route) },
                modifier = Modifier.size(44.dp),
                shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.medium),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = if (selected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.icon,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}
```

- [ ] **步骤 3: 修改 MainActivity setContent 为 Rail + Content 布局**

```kotlin
setContent {
    XyzTvTheme {
        val navController = rememberNavController()
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route.orEmpty()
        
        Row(modifier = Modifier.fillMaxSize()) {
            // Side Rail
            if (currentRoute != "login" && currentRoute != "web-login" && currentRoute != "pair-login") {
                SideRail(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                )
            }
            
            // Content Area
            NavHost(
                navController = navController,
                startDestination = if (isLoggedIn) "home" else "login",
                modifier = Modifier.weight(1f)
            ) {
                // ... existing composables
            }
        }
    }
}
```

- [ ] **步骤 4: Commit Shell 重构**

```bash
git add app/src/main/java/com/ultrazg/xyztv/MainActivity.kt
git commit -m "feat: refactor MainActivity to Rail + Content layout"
```

---

## 阶段 3: 页面重设计

### 任务 7: 重写 HomeScreen

**文件：**
- 修改：`HomeScreen.kt`

- [ ] **步骤 1: 简化 HomeScreen 结构，使用货架行**

```kotlin
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPodcast: (String) -> Unit,
    onNavigateToEpisode: (String) -> Unit,
    // ... other callbacks
) {
    val viewModel: HomeViewModel = viewModel()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp, 28.dp)
    ) {
        // 继续收听
        ContentShelf(
            title = "继续收听",
            onViewAll = { /* TODO */ }
        ) {
            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.continueListening) { episode ->
                    EpisodeWideCard(
                        episode = episode,
                        onClick = { onNavigateToEpisode(episode.eid) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 编辑精选
        ContentShelf(
            title = "编辑精选",
            onViewAll = { /* TODO */ }
        ) {
            TvLazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(viewModel.editorPicks) { episode ->
                    EpisodeWideCard(
                        episode = episode,
                        onClick = { onNavigateToEpisode(episode.eid) }
                    )
                }
            }
        }
        
        // ... 其他货架行
    }
}
```

- [ ] **步骤 2: Commit HomeScreen 重写**

```bash
git commit -m "refactor: rewrite HomeScreen with ContentShelf layout"
```

---

### 任务 8: 更新 PlayerScreen 视觉

**文件：**
- 修改：`PlayerScreen.kt`

- [ ] **步骤 1: 更新 PlayerScreen 颜色引用**

```kotlin
// 替换旧颜色定义为使用 MaterialTheme
private val PlayerBackground @Composable get() = MaterialTheme.colorScheme.background
private val PlayerSurface @Composable get() = MaterialTheme.colorScheme.surface
private val PlayerAccent @Composable get() = MaterialTheme.colorScheme.primary
```

- [ ] **步骤 2: 确保所有 ControlButton 调用使用新组件**

```kotlin
// 将现有的 ControlButton 调用替换为 TvControlButton
// 保持所有 onClick 逻辑不变
```

- [ ] **步骤 3: Commit PlayerScreen 更新**

```bash
git commit -m "style: update PlayerScreen with new theme colors, keep all controls"
```

---

### 任务 9: 更新 SearchScreen

**文件：**
- 修改：`SearchScreen.kt`

- [ ] **步骤 1: 更新 SearchScreen 使用新主题**

```kotlin
// 更新背景色
Column(
    modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(28.dp)
)
```

- [ ] **步骤 2: 更新搜索栏样式**

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
        .padding(horizontal = 18.dp, vertical = 12.dp)
)
```

- [ ] **步骤 3: Commit SearchScreen 更新**

```bash
git commit -m "style: update SearchScreen with new theme"
```

---

### 任务 10-15: 更新其他页面

为每个页面重复类似步骤（Subscriptions, CategoryHub, History, Profile, Settings, Login）:

- [ ] **任务 10: 更新 SubscriptionsScreen**
- [ ] **任务 11: 更新 CategoryHubScreen**
- [ ] **任务 12: 更新 HistoryScreen**
- [ ] **任务 13: 更新 ProfileScreen**
- [ ] **任务 14: 更新 SettingsScreen**
- [ ] **任务 15: 更新 LoginScreen**
- [ ] **任务 16: 更新 WebLoginScreen**

每个任务的步骤：
1. 更新背景色为 `MaterialTheme.colorScheme.background`
2. 更新卡片/按钮使用新组件
3. 确保所有功能键逻辑不变
4. Commit

---

## 阶段 4: 测试与验证

### 任务 17: 编译测试

- [ ] **步骤 1: 运行编译检查**

```bash
./gradlew :app:compileDebugKotlin
```

- [ ] **步骤 2: 修复编译错误**

---

### 任务 18: 焦点系统测试

- [ ] **步骤 1: 在模拟器上测试 D-pad 导航**
- [ ] **步骤 2: 验证 Rail 和 Content 之间焦点切换**
- [ ] **步骤 3: 验证货架行内卡片焦点移动**

---

### 任务 19: 功能键验证

- [ ] **步骤 1: 验证播放器所有 10 个功能键工作**
- [ ] **步骤 2: 验证登录页所有登录方式可用**
- [ ] **步骤 3: 验证 Web 登录页所有控制按钮工作**

---

## 自检清单

### 规格覆盖度
- [x] 新色彩系统 → 任务 1
- [x] 浅色主题 → 任务 1
- [x] 焦点缩放高亮 → 任务 2
- [x] 播客卡片 → 任务 4
- [x] 单集卡片 → 任务 4
- [x] 货架行 → 任务 5
- [x] Rail 导航 7 入口 → 任务 6
- [x] 播放器功能键保留 → 任务 8
- [x] 首页货架行 → 任务 7
- [x] 所有页面视觉更新 → 任务 9-16

### 占位符扫描
- [ ] 无 "TODO" / "待定" / "后续"
- [ ] 所有代码步骤包含实际代码
- [ ] 所有类型名称一致

---

## 执行选项

**计划已保存到** `docs/superpowers/plans/2025-05-03-smarttube-redesign.md`

**两种执行方式：**

1. **子代理驱动（推荐）** - 使用 `superpowers:subagent-driven-development`，每个任务一个子代理，任务间审查
2. **内联执行** - 使用 `superpowers:executing-plans`，批量执行并设检查点

**选哪种方式？**
