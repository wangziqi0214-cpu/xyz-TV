# XYZ TV SmartTube 风格全局 UI/UX 重设计规格

**日期**：2025-05-03  
**最后修订**：2026-05-03  
**范围**：全量统一（所有页面）  
**参考**：SmartTube Android TV 应用

---

## 1. 设计决策总结

| 决策项 | 选择 | 说明 |
|--------|------|------|
| 全局方向 | SmartTube Classic | 左侧 rail + 内容货架行 |
| 导航结构 | 均衡 7 入口 | 首页/搜索/订阅/分类/历史/我的/设置 |
| 首页布局 | 纯货架行 | 无 Hero，等高横向滚动行 |
| 播放器 | 全屏沉浸 | 居中封面 + 控制栏 |
| 卡片样式 | 双形态 | 播客方封面 + 单集横向宽卡 |
| 焦点高亮 | 缩放 + 阴影 | 聚焦时放大 + 青绿光晕 |
| 主题 | 浅色 | 白底 + 青绿主色 |

### 1.1 当前修订原则

- **先统一结构，再打磨细节：** 每个页面先落到 SmartTube 的「左侧 Rail + 内容区域」结构，再逐步处理动画、焦点记忆和细节状态。
- **功能优先保留：** 页面改版不得删除现有业务入口、跳转和按钮行为。例外：明确属于调试性质或用户已确认不需要的冗余按钮可以移除。
- **登录页范围收敛：** `WebLoginScreen` 只保留网页登录的必要主路径：进入官方登录页 → 自动或手动切到扫码登录 → 显示二维码 → 捕获 token 后回到首页。不要保留 Phone、Try QR、Scan QR 这类面向调试的多按钮面板。
- **验证以当前模拟器前台为准：** 验证截图前必须确认 `com.ultrazg.xyztv/.MainActivity` 是当前前台 Activity，避免浏览器 Intent 或旧截图造成误判。

---

## 2. 色彩系统

### 2.1 色板

| 名称 | 色值 | 用途 |
|------|------|------|
| `background` | `#FFFFFF` | 全局背景 |
| `surface` | `#FAFAFB` | 页面底、输入框背景 |
| `surfaceVariant` | `#EDEEF1` | 卡片背景、货架行卡片 |
| `primary` | `#62BBD2` | 主按钮、进度条、选中态 |
| `primaryLight` | `#78C2D2` | 副 accent、图标、链接 |
| `onBackground` | `#020303` | 主文字、标题 |
| `onSurface` | `#3A3D42` | 副文字、描述 |
| `outline` | `#D8DADE` | 边框、分隔线 |

### 2.2 交互状态系统

所有可交互元素必须定义以下状态：

| 状态 | 视觉表现 | 说明 |
|------|----------|------|
| Default | 原始样式 | 未聚焦、未选中 |
| Focused | scale 1.06 + 青绿阴影环 `0 6px 24px rgba(98,187,210,.22), 0 0 0 2.5px #62BBD2` | D-pad 焦点所在 |
| Pressed | scale 0.97 + 阴影收缩至 `0 2px 8px rgba(98,187,210,.18)` | 遥控器确认键按下瞬间 |
| Selected | `primary` 填充或 2.5dp `primary` 边框 | Tab 选中、当前 Rail 入口 |
| Disabled | opacity 0.38，不可聚焦 | 功能暂不可用 |
| Loading | 骨架屏闪烁或 `surfaceVariant` 占位 | 数据加载中 |
| Error | `#E53935` 文字 + 重试按钮 | 请求失败 |
| Empty | 居中图标 + 说明文字 | 无数据 |

播放按钮额外 Focused：`boxShadow: 0 4px 16px rgba(98,187,210,.35)`

### 2.3 字体层级

使用系统默认无衬线字体（Noto Sans CJK SC / Roboto fallback）。

| 角色 | 大小 | 字重 | 行高 | 用途 |
|------|------|------|------|------|
| display | 36sp | 700 | 1.2 | 播放器标题 |
| headline | 28sp | 700 | 1.25 | 页面大标题（ProfileScreen 昵称） |
| title-lg | 22sp | 700 | 1.3 | 播放器单集标题 |
| title-md | 18sp | 700 | 1.35 | 分类名、区域标题 |
| title-sm | 16sp | 700 | 1.4 | 货架行标题 |
| body-lg | 16sp | 400 | 1.5 | 搜索输入、正文 |
| body-md | 14sp | 400 | 1.5 | 卡片标题、设置行文字 |
| body-sm | 13sp | 700 | 1.4 | 播客卡片标题 |
| label-lg | 14sp | 700 | 1.3 | 按钮文字 |
| label-md | 12sp | 400 | 1.4 | 元信息、时长 |
| label-sm | 11sp | 400 | 1.4 | 副标题、辅助文字 |

### 2.4 圆角体系

| 级别 | 半径 | 用途 |
|------|------|------|
| xs | 8dp | 进度条、小徽章 |
| sm | 14dp | 按钮、图标按钮、单集封面 |
| md | 18dp | 播客封面、搜索栏 |
| lg | 22dp | 分类卡片 |
| xl | 28dp | 评论弹窗、对话框 |
| full | 9999dp | Chip、药丸按钮（如需） |

### 2.5 间距节奏

基准单位 **8dp**，所有间距为 8 的倍数或半数：

| Token | 值 | 典型用途 |
|-------|-----|----------|
| space-xs | 4dp | 图标与文字间隙 |
| space-sm | 8dp | 卡片内边距、行间 |
| space-md | 12dp | 货架卡片 gap |
| space-lg | 16dp | Rail 垂直内边距、区域间 |
| space-xl | 20dp | Content 顶部内边距、货架行间距 |
| space-2xl | 28dp | Content 左右内边距 |
| space-3xl | 48dp | 页面标题与内容间距 |

### 2.6 动效规范

| 属性 | 时长 | 曲线 | 说明 |
|------|------|------|------|
| 焦点缩放 | 200ms | FastOutSlowIn | scale 1.0 → 1.06 |
| 焦点阴影 | 200ms | FastOutSlowIn | 同步于缩放 |
| 按下缩放 | 100ms | FastOutLinearIn | scale → 0.97 |
| 页面切换 | 300ms | FastOutSlowIn | fadeIn + slideInHorizontally |
| 货架滚动 | 250ms | LinearOutSlowIn | animateScrollToItem |
| 内容展开 | 250ms | FastOutSlowIn | animateContentSize |
| 骨架闪烁 | 1200ms | infinite linear | alpha 0.3 → 0.7 循环 |

Reduced-motion：当系统开启「减少动画」时，所有时长降为 0ms，仅保留即时状态切换。

### 2.7 深色模式（未来扩展）

当前版本仅实现浅色主题。深色模式预留 token 映射：

| 浅色 Token | 浅色值 | 深色值（预留） |
|------------|--------|----------------|
| background | `#FFFFFF` | `#121212` |
| surface | `#FAFAFB` | `#1E1E1E` |
| surfaceVariant | `#EDEEF1` | `#2C2C2C` |
| primary | `#62BBD2` | `#62BBD2` |
| onBackground | `#020303` | `#E8E8E8` |
| onSurface | `#3A3D42` | `#B3B3B3` |
| outline | `#D8DADE` | `#3A3A3A` |

深色模式实现优先级低于内容页重构，待浅色主题全量落地后再启动。

---

## 3. 布局架构

### 3.1 全局 Shell

```
┌─────────────────────────────────────────────────────┐
│  [Rail] │ [Content Area]                            │
│  H      │                                           │
│  🔍     │  Shelf 1: 继续收听                        │
│  📥     │  [Card][Card][Card][Card]                 │
│  🏷     │                                           │
│  🕐     │  Shelf 2: 编辑精选                        │
│  👤     │  [Card][Card][Card][Card]                 │
│  ⚙      │                                           │
│         │  Shelf 3: 热门榜                          │
│         │  [Card][Card][Card][Card]                 │
└─────────────────────────────────────────────────────┘
```

- Rail 宽度：72dp
- Rail 内边距：16dp 垂直，8dp 水平
- 图标按钮：44dp × 44dp，圆角 14dp
- Content 内边距：20dp 上，28dp 左右

### 3.2 侧边 Rail 入口

| 图标 | 标签 | 路由 | 说明 |
|------|------|------|------|
| 🏠 | 首页 | `home` | 货架行聚合 |
| 🔍 | 搜索 | `search` | 播客+单集搜索 |
| 📥 | 订阅 | `subscriptions` | 订阅更新 + 列表 |
| 🏷 | 分类 | `categories` | 分类 Hub |
| 🕐 | 历史 | `history` | 收听历史 |
| 👤 | 我的 | `profile` | 收藏/喜欢/关注/数据 |
| ⚙️ | 设置 | `app-settings` | 首页布局/模块位置设置 |

---

## 4. 组件规范

### 4.1 卡片组件

#### 播客卡片 (SquareCoverCard)
- 尺寸：146dp × 120dp
- 封面：100dp × 100dp，圆角 18dp
- 标题：13sp，fontWeight 700，onBackground
- 副标题：11sp，onSurface
- 聚焦：scale 1.06 + 青绿阴影环

#### 单集卡片 (EpisodeWideCard)
- 尺寸：全宽自适应，最小 280dp
- 高度：88dp
- 封面：64dp × 64dp，圆角 14dp
- 标题：14sp，fontWeight 700，onBackground
- 元信息：12sp，onSurface
- 进度条：4dp 高，主色填充
- 聚焦：scale 1.02 + 青绿边框阴影

### 4.2 货架行 (ContentShelf)
- 标题区：标题 16sp +「查看全部」链接
- 内容区：横向滚动，gap 12dp
- 行间距：20dp

### 4.3 播放器 (保留所有现有功能键)

**现有功能键必须全部保留，只更新视觉样式**

#### 布局结构
```
┌────────────────────────────────────────────┐
│  ‹ 返回                    [状态消息]       │
│                                            │
│  ┌────┐  播客名 (14sp)                     │
│  │封面│  单集标题 (22sp)                    │
│  │76dp│  高能点 X 段 · 总标记 X · 我的 X   │
│  └────┘                                    │
│                                            │
│          [实时字幕区域]                    │
│                                            │
│  ════════════════════▓══════════ 速度X.x  │
│                                            │
│  [返回详情] [☰评论] [★收藏]  ↶  ⏸  ↷  [◆] [■]
│         0.75x 1x 1.25x 1.5x 1.75x 2x 3x    │
└────────────────────────────────────────────┘
```

#### 功能键完整清单 (必须保留)

| 位置 | 图标/文字 | 功能 | 尺寸 | 新样式 |
|------|----------|------|------|--------|
| 左上 | ‹ | 返回 | 42dp | ControlButton, #EDEEF1底 |
| 左下 | "返回详情" | 打开详情页 | 92×42dp | ControlTextButton |
| 左下 | ☰ | 评论开关 | 38dp | ControlButton |
| 左下 | ☆/★ | 收藏/取消 | 38dp | ControlButton |
| 中央 | ↶ | 后退30秒 | 56dp | ControlButton |
| 中央 | Ⅱ/▶ | 播放/暂停 | 68dp | ControlButton, 主色填充 |
| 中央 | ↷ | 前进30秒 | 56dp | ControlButton |
| 右下 | X.x | 速度选择 | 自适应 | SpeedMenuButton |
| 右下 | ◇/◆ | 鼓掌标记 | 38dp | ControlButton |
| 右下 | □/■ | 停止退出 | 38dp | ControlButton |
| 展开 | 0.75x-3x | 7个速度 | 按钮组 | 选中主色高亮 |

- 封面：76dp × 76dp，圆角 14dp
- 进度条：6dp 高，主色填充，支持点击跳转
- 评论弹窗：右侧 440dp，圆角 28dp，毛玻璃效果

### 4.4 按钮

| 类型 | 背景 | 文字 | 圆角 | 内边距 |
|------|------|------|------|--------|
| Primary | `#62BBD2` | `#FFFFFF` | 14dp | 14dp 20dp |
| Secondary | `#EDEEF1` | `#020303` | 14dp | 12dp 18dp |
| Icon | `#EDEEF1` | `#3A3D42` | 14dp | 12dp |

### 4.5 TV 焦点最小尺寸

所有可聚焦元素的最小尺寸为 **48dp × 48dp**（含内边距的可触摸/可聚焦区域）。
播放器核心控件（播放/暂停、快退、快进）使用 56dp–68dp 以增强 10-foot 可识别性。
Rail 图标按钮已设为 44dp × 44dp + 8dp 水平内边距 = 有效 60dp 宽。

### 4.6 统一状态组件

#### LoadingPlaceholder
- 使用 `surfaceVariant` 背景的骨架矩形，按实际卡片尺寸占位。
- alpha 在 0.3–0.7 间循环闪烁（1200ms 周期）。
- 货架行加载时显示 3–5 个骨架卡片。

#### ErrorCard
- 居中布局：⚠ 图标 + 错误描述（body-md，`onSurface`）+ 重试按钮（Secondary）。
- 宽度自适应，最大 400dp。
- 重试按钮获得首焦点。

#### EmptyState
- 居中布局：说明文字（body-lg，`onSurface`）。
- 不展示空白大区域；可选加一个灰色图标增强视觉。
- 可聚焦元素（如搜索按钮）获得首焦点。

---

## 5. 页面规格

### 5.1 首页 (HomeScreen)

货架行顺序：
1. **继续收听** — 带播放进度条
2. **编辑精选** — 编辑推荐的单集
3. **推荐发现** — 个性化推荐
4. **热门榜** — 热门榜单
5. **新锐榜** — 新锐榜单
6. **飞跃榜** — 飞跃榜单
7. **试听发现** — 试听课单

每行：最多 8 个卡片，横向滚动

### 5.2 搜索页 (SearchScreen)

**保留功能键**：Back 返回，Search 搜索按钮，播客/单集切换

- **页面结构：** 顶部搜索栏 + 类型切换 + 结果区。
- **搜索栏：** 使用 `surface` 背景，圆角 18dp；输入框 16sp，光标使用 `primary`；左侧保留 Back，右侧保留 Search 按钮。
- **类型切换：** 播客 / 单集双按钮组，当前类型使用 `primary` 填充，未选中使用 `surfaceVariant`。
- **结果区：**
  - 播客结果使用方形播客卡片网格，优先 5 列。
  - 单集结果使用 `EpisodeWideCard`，按 2 列或纵向宽卡布局。
  - 预设关键词使用横向 chip 行，保留点击搜索能力。
- **空状态：** 居中显示「输入关键词开始搜索」，不展示空白大区域。
- **加载/错误：** 使用统一信息卡片，不弹窗，不阻断 D-pad 焦点。

### 5.3 订阅页 (SubscriptionScreen)

**保留功能键**：Back 返回，点击播客进入详情

- **实际实现：** 当前路由为 `subscriptions`，对应 `SubscriptionScreen`。
- **页面结构：** 标题区 + 订阅播客网格。
- **卡片形态：** 使用播客方卡，保留进入播客详情的点击行为。
- **布局：** 1080p 下优先 5 列；当数据不足时左对齐，不居中漂浮。
- **后续扩展：** 如果恢复「订阅更新」数据源，再增加顶部 Tab；当前不为不存在的数据源新增空 Tab。

### 5.4 分类页 (CategoryHubScreen)

**保留功能键**：Back 返回

- **页面结构：** 标题区 + 分类网格。
- **分类卡片：** 使用 `surfaceVariant` 背景，圆角 22dp；分类名 18sp 加粗，描述 13sp。
- **布局：** 1080p 下 5 列，卡片高度 84dp；长分类名最多 1 行截断。
- **交互：** 点击分类进入 `category/{categoryId}/{categoryName}`；Back 返回上级。
- **状态：** 加载、错误、空状态统一使用信息卡片。

### 5.5 历史页 (HistoryScreen)

**保留功能键**：Back 返回，点击单集进入详情

- **实际实现：** 当前 `HistoryScreen` 位于 `LibraryScreen.kt`，路由为 `history`。
- **页面结构：** 标题区 + 纵向单集列表。
- **卡片形态：** 使用 `EpisodeWideCard` 或等价宽卡，展示封面、标题、播客名、时长、播放进度。
- **布局：** 1080p 下优先 2 列宽卡；如果焦点稳定性不足，先采用单列宽卡。
- **删除能力：** 只有代码中已有删除/清空 API 时才展示删除按钮；不得凭空新增不可用按钮。

### 5.6 我的页 (ProfileScreen)

**保留功能键**：Back 返回，我的订阅/收藏/历史/收听数据/关注/粉丝/星标订阅/未加星标/贴纸/黑名单/偏好/收藏评论等所有入口

- **页面结构：** 用户信息区 + 快捷入口网格 + 内容货架。
- **用户信息区：** 头像 88dp，昵称 28sp，简介/统计使用 `onSurface`。
- **快捷入口：** 保留所有现有入口，按功能分组：
  1. 内容：订阅、收藏、历史、编辑精选历史。
  2. 社交：关注、粉丝、收藏评论、黑名单。
  3. 订阅管理：星标订阅、未加星标订阅。
  4. 个人资产：收听数据、贴纸、偏好。
- **入口卡片：** 统一 180dp × 76dp，图标 + 标题 + 简短描述。
- **内容货架：** 如果已有最近喜欢或收藏数据，使用横向货架；否则不新增伪数据。

### 5.7 设置页 (AppSettingsScreen)

**保留功能键**：Back 返回，首页模块位置切换，上移，下移

- **实际实现：** 当前设置页是 `AppSettingsScreen`，路由为 `app-settings`，主要控制首页模块布局。
- **页面结构：** 标题区 + 当前配置版本卡片 + 首页模块列表。
- **设置行：** 每一行显示模块名称、当前位置（顶部/货架）、上移/下移、位置切换。
- **交互规则：** 保留 `HomeLayoutManager` 的所有现有能力；按钮只改视觉，不改排序逻辑。
- **布局：** 左侧为模块说明，右侧为操作按钮组；按钮间距 10dp。
- **底部信息：** 展示配置版本与「返回首页立即生效」提示。

### 5.8 登录页 (LoginScreen)

**保留功能键**：Phone Number 输入，Send SMS Code，Verification Code 输入，Login，Web Login 入口，Pair Login 入口

- **页面结构：** 左侧登录表单 + 右侧诊断状态卡片。
- **主入口优先级：**
  1. Web Login：推荐入口，放在表单顶部，使用 `primary` 按钮。
  2. Pair Login：备用入口，使用 Secondary 按钮。
  3. SMS 登录：保留现有手机号、验证码、发送、登录流程。
- **文案：** 面向 TV 用户，避免调试口吻；诊断日志可以保留在右侧状态卡片中。
- **焦点：** 首焦点落到 Web Login，遥控器向下进入 Pair Login 和 SMS 表单。

### 5.9 Web 登录页 (WebLoginScreen)

**保留功能**：Reload，Back，WebView 官方登录页，扫码登录切换，二维码检测，token 捕获

- **主目标：** 用户点开 Web Login 后，页面应自动加载官方网页登录，并尽快进入扫码登录状态，显示可扫码二维码。
- **不需要的按钮：** 不展示 Phone、Try QR、Scan QR 这类调试按钮。
- **保留按钮：**
  - Reload：页面加载失败或二维码过期时重新加载。
  - Back：返回 `LoginScreen`。
- **自动流程：**
  1. WebView 打开官方登录页。
  2. JavaScript 桥接检测登录组件是否渲染。
  3. 如果默认是手机号页签，自动点击「扫码登录」页签或按钮。
  4. 检测二维码图片或二维码链接。
  5. 将二维码同步到右侧/顶部醒目卡片，方便手机扫码。
  6. 扫码成功后从 Cookie 捕获 `x-jike-access-token` / `x-jike-refresh-token`，写入 `TokenManager` 并返回首页。
- **布局：** 顶部状态栏高度不超过 140dp；WebView 占据主要区域；二维码卡片浮在右侧，不遮挡核心登录控件。
- **错误处理：** 加载失败时展示状态文本 + Reload；不得自动跳浏览器，除非用户明确点击外部打开入口。

---

## 6. 焦点系统

### 6.1 焦点导航

- D-pad 上下：货架行间移动
- D-pad 左右：行内卡片移动
- D-pad 左：从内容区返回 Rail
- D-pad 右：从 Rail 进入内容
- Back：返回上级，或退出到首页

### 6.2 焦点记忆

- 离开页面时记住焦点位置
- 返回时恢复焦点
- 货架行切换时，优先落到同行首个

### 6.3 焦点可视反馈

所有可聚焦元素必须有：
- 缩放动画 (scale 1.0 → 1.06)
- 阴影光晕 (青绿半透明)
- 可选：边框高亮叠加

---

## 7. 迁移计划

### 阶段 1：已完成基建校准

1. 创建浅色主题与基础色板。
2. 创建 Rail、按钮、卡片、焦点修饰符等基础组件。
3. 将 `MainActivity` 改为 Rail + Content Shell。
4. 将 `HomeScreen` 重构为纯货架布局。
5. 修复 `TokenManager.loginState`，让重新登录和 token 清理能触发 UI 状态更新。

### 阶段 2：登录闭环修复

1. `LoginScreen` 首焦点落到 Web Login。
2. `WebLoginScreen` 删除调试按钮，只保留 Reload 和 Back。
3. WebView 自动进入扫码登录路径并显示二维码。
4. token 捕获成功后写入 `TokenManager` 并回到首页。
5. 验证前台 Activity 和截图来源，避免浏览器 Intent 干扰判断。

### 阶段 3：核心内容页

1. 重构 `SearchScreen`：搜索栏、类型切换、预设关键词、结果网格。
2. 重构 `SubscriptionScreen`：订阅播客网格。
3. 重构 `CategoryHubScreen`：分类网格。
4. 重构 `HistoryScreen`：单集宽卡列表。

### 阶段 4：个人与设置页

1. 重构 `ProfileScreen`：用户信息、快捷入口分组、内容货架。
2. 重构 `AppSettingsScreen`：模块设置列表和操作按钮组。
3. 梳理 `FavoriteScreen`、`TopListScreen`、`CategoryScreen`，复用统一卡片组件。

### 阶段 5：详情页与播放器打磨

1. 重构 `PodcastDetailScreen`，保持订阅、打开单集等现有功能。
2. 重构 `EpisodeDetailScreen`，保持播放、收藏、评论等现有入口。
3. 打磨 `PlayerScreen`，只改视觉，不删任何控制键。
4. 全量验证 D-pad 焦点、Back 栈、截图和 1080p 适配。

---

## 8. 技术约束

### 8.1 框架与依赖

- 使用 `androidx.tv.material3` 组件
- 保持 FocusRequester 管理焦点
- 使用 `bringIntoViewOnFocus` 滚动
- 动画使用 `animateContentSize` 和 `AnimatedVisibility`
- 图片使用 Coil AsyncImage，带占位
- 尽量复用 `ContentShelf`、`EpisodeWideCard`、`PodcastCard`、`TvControlButton`、`TvControlTextButton`
- 页面重构优先使用现有 ViewModel 和导航回调，不新增平行状态源

### 8.2 Token 引用规则

- 颜色使用 §2.1 色板 token 名（`background`、`surface`、`primary` 等），不硬编码色值。
- 字号使用 §2.3 字体层级角色名（`title-sm`、`body-md` 等），不散写裸 sp。
- 圆角使用 §2.4 圆角体系级别名（`sm`、`md`、`lg` 等）。
- 间距使用 §2.5 间距 token（`space-sm`、`space-md` 等），新增间距必须是 8dp 倍数。
- 动效时长和曲线使用 §2.6 规范值，不自行定义。

### 8.3 兼容性与验证

- 保持向后兼容：业务功能不变；调试按钮不属于必须保留的业务功能
- 模拟器验证必须先确认当前前台 Activity 是 `com.ultrazg.xyztv/.MainActivity`
- 所有可聚焦元素最小尺寸 48dp × 48dp（§4.5）
- 每个页面必须处理 Loading / Error / Empty 三种状态（§4.6）
- 横屏 only，不出现竖屏布局或透明背景

---

## 9. 品牌参考

| 参考 | 取用要素 | 不取用要素 |
|------|----------|------------|
| **SmartTube**（主方向） | 左侧 Rail + 内容货架行结构、焦点缩放 + 光晕、横向滚动货架、全屏播放器 | 深色主题（我们用浅色） |
| **PlayStation DESIGN.md** | flat-on-canvas 卡片（无 resting shadow）、pill CTA 形态参考、section-band 节奏 | 品牌蓝/橙色、PS 字体 |
| **Spotify DESIGN.md** | sidebar + content area 结构、密集内容扫描布局、8dp spacing rhythm、heavy shadow on elevated | 深色背景、Spotify Green、uppercase 按钮 |
| **Android TV 官方指南** | 10-foot 可读性、D-pad 五向导航、焦点系统、横屏 only、launcher 资源 | Leanback 库（我们用 Compose TV） |

所有参考仅提取结构和交互模式，不复制品牌视觉标识。

---

## 10. 验收标准

### 10.1 视觉一致性

- [ ] 所有页面使用 §2.1 色彩系统，无硬编码色值
- [ ] 所有文字使用 §2.3 字体层级，无裸 sp
- [ ] 所有圆角使用 §2.4 体系，无随意半径
- [ ] 所有间距符合 §2.5 的 8dp 节奏
- [ ] 卡片样式符合 §4.1 规格
- [ ] 所有文字对比度 ≥ 4.5:1

### 10.2 交互与焦点

- [ ] 所有可交互元素有 §2.2 定义的焦点反馈（Focused + Pressed）
- [ ] D-pad 导航流畅，无焦点丢失
- [ ] Rail 导航 7 入口工作正常
- [ ] 首页货架行滚动流畅
- [ ] 所有可聚焦元素 ≥ 48dp × 48dp
- [ ] Back 键行为可预测：页面内返回上级 → Rail → 退出确认

### 10.3 功能保全

- [ ] **播放器保留所有功能键**：返回/详情/评论/收藏/快退/播放暂停/快进/速度/鼓掌/停止
- [ ] **登录页保留所有功能**：手机号输入/验证码/发送/登录/Web 登录/Pair 登录
- [ ] **Web 登录页主路径可用**：打开 Web Login → 自动或手动进入扫码登录 → 显示二维码 → 扫码后捕获 token → 回到首页
- [ ] **Web 登录页只保留必要控制**：Reload/Back 可用，无 Phone/Try QR/Scan QR 调试按钮
- [ ] **其他页面保留所有现有功能键**

### 10.4 状态覆盖

- [ ] 每个数据页面实现 Loading 骨架屏（§4.6 LoadingPlaceholder）
- [ ] 每个数据页面实现 Error 重试（§4.6 ErrorCard）
- [ ] 每个数据页面实现 Empty 空状态（§4.6 EmptyState）

### 10.5 适配与验证

- [ ] 适配 TV 各种分辨率（720p / 1080p / 4K）
- [ ] 横屏 only，无竖屏布局泄漏
- [ ] 验证截图必须来自当前前台 `MainActivity`，不能使用旧截图或外部浏览器页面
- [ ] Reduced-motion 模式下动画降级正常
