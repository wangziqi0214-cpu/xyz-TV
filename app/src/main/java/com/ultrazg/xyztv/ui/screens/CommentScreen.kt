package com.ultrazg.xyztv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.ultrazg.xyztv.data.model.CommentItem
import com.ultrazg.xyztv.ui.viewmodel.CommentViewModel

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeCommentScreen(
    eid: String,
    onBack: () -> Unit,
    onOpenThread: (commentId: String, ownerId: String) -> Unit
) {
    val viewModel: CommentViewModel = viewModel()

    LaunchedEffect(eid) {
        viewModel.loadPrimaryComments(eid)
    }

    CommentScaffold(
        title = "单集评论",
        subtitle = "对应 xyz 的评论主列表与创建评论能力。",
        onBack = onBack,
        inputText = viewModel.inputText,
        onInputChange = viewModel::onInputChange,
        submitLabel = if (viewModel.isSubmitting) "发送中..." else "发表评论",
        onSubmit = { viewModel.submitComment(eid) { viewModel.loadPrimaryComments(eid) } },
        isLoading = viewModel.isLoading,
        error = viewModel.error
    ) {
        items(viewModel.comments.size) { index ->
            val comment = viewModel.comments[index]
            CommentCard(
                comment = comment,
                onLike = { viewModel.toggleLike(comment) },
                onCollect = { viewModel.toggleCollect(comment) },
                onReply = { onOpenThread(comment.id, eid) },
                onDelete = { viewModel.removeComment(comment) { viewModel.loadPrimaryComments(eid) } },
                showReplyAction = true,
                canDelete = viewModel.canDelete(comment)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CommentThreadScreen(
    ownerId: String,
    primaryCommentId: String,
    onBack: () -> Unit
) {
    val viewModel: CommentViewModel = viewModel()

    LaunchedEffect(primaryCommentId) {
        viewModel.loadThread(primaryCommentId)
    }

    CommentScaffold(
        title = "评论回复",
        subtitle = "对应 xyz 的评论回复线程与回复创建能力。",
        onBack = onBack,
        inputText = viewModel.inputText,
        onInputChange = viewModel::onInputChange,
        submitLabel = if (viewModel.isSubmitting) "发送中..." else "回复这条评论",
        onSubmit = {
            viewModel.submitComment(ownerId, primaryCommentId) {
                viewModel.loadThread(primaryCommentId)
            }
        },
        isLoading = viewModel.isLoading,
        error = viewModel.error
    ) {
        items(viewModel.comments.size) { index ->
            val comment = viewModel.comments[index]
            CommentCard(
                comment = comment,
                onLike = { viewModel.toggleLike(comment) },
                onCollect = { viewModel.toggleCollect(comment) },
                onReply = {},
                onDelete = { viewModel.removeComment(comment) { viewModel.loadThread(primaryCommentId) } },
                showReplyAction = false,
                canDelete = viewModel.canDelete(comment)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CommentScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    inputText: String,
    onInputChange: (String) -> Unit,
    submitLabel: String,
    onSubmit: () -> Unit,
    isLoading: Boolean,
    error: String?,
    content: androidx.tv.foundation.lazy.list.TvLazyListScope.() -> Unit
) {
    TvLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = {
            item {
                FeatureHeader(title = title, subtitle = subtitle, onBack = onBack)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            textStyle = TextStyle(color = Color(0xFF020303), fontSize = 16.sp),
                            cursorBrush = SolidColor(Color(0xFF020303)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (inputText.isBlank()) {
                                Text(text = "输入评论内容...", color = Color(0xFF3A3D42), fontSize = 16.sp)
                            }
                            it()
                        }
                    }
                    Surface(
                        onClick = onSubmit,
                        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = submitLabel,
                            color = Color.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            when {
                isLoading -> item { CenterMessage("正在加载评论...") }
                error != null -> item { FeatureMessageCard("加载失败：$error", Color(0xFFFFB74D)) }
                else -> content()
            }
        }
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CommentCard(
    comment: CommentItem,
    onLike: () -> Unit,
    onCollect: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    showReplyAction: Boolean,
    canDelete: Boolean
) {
    Surface(
        onClick = onReply,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.author?.nickname.orEmpty(),
                    color = Color(0xFFFFC36D),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = comment.createdAt?.take(10).orEmpty(),
                    color = Color(0xFF7F8B90),
                    fontSize = 12.sp
                )
            }

            comment.replyToAuthorName?.takeIf { it.isNotBlank() }?.let {
                Text(text = "回复 @$it", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            }

            Text(
                text = comment.text,
                color = Color(0xFF020303),
                fontSize = 16.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (comment.replies.isNotEmpty()) {
                comment.replies.take(2).forEach { reply ->
                    Text(
                        text = "${reply.author?.nickname.orEmpty()}：${reply.text}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniActionButton(
                    text = if (comment.liked) "取消赞 ${comment.likeCount}" else "点赞 ${comment.likeCount}",
                    onClick = onLike
                )
                MiniActionButton(
                    text = if (comment.collected) "取消收藏" else "收藏",
                    onClick = onCollect
                )
                if (showReplyAction) {
                    MiniActionButton(
                        text = if (comment.threadReplyCount > 0) "查看回复 ${comment.threadReplyCount}" else "回复",
                        onClick = onReply
                    )
                }
                if (canDelete) {
                    MiniActionButton(
                        text = "删除",
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MiniActionButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = text,
            color = Color(0xFF020303),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
