package com.ultrazg.xyztv.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.model.CommentItem
import com.ultrazg.xyztv.data.repository.PodcastRepository
import kotlinx.coroutines.launch

class CommentViewModel : ViewModel() {

    private val repository = PodcastRepository()

    var comments by mutableStateOf<List<CommentItem>>(emptyList())
        private set

    var inputText by mutableStateOf("")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isSubmitting by mutableStateOf(false)
        private set

    var viewerUid by mutableStateOf<String?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun onInputChange(value: String) {
        inputText = value
    }

    fun canDelete(comment: CommentItem): Boolean {
        val uid = viewerUid ?: return false
        return comment.author?.uid == uid
    }

    fun loadPrimaryComments(eid: String) {
        AppLogger.info("comment", "loadPrimaryComments eid=$eid")
        viewModelScope.launch {
            isLoading = true
            error = null
            ensureViewerUid()
            repository.getPrimaryComments(eid)
                .onSuccess { comments = it }
                .onFailure { error = userFacingError(it) }
            isLoading = false
        }
    }

    fun loadThread(primaryCommentId: String) {
        AppLogger.info("comment", "loadThread primaryCommentId=$primaryCommentId")
        viewModelScope.launch {
            isLoading = true
            error = null
            ensureViewerUid()
            repository.getCommentThread(primaryCommentId)
                .onSuccess { comments = it }
                .onFailure { error = userFacingError(it) }
            isLoading = false
        }
    }

    fun submitComment(ownerId: String, replyToCommentId: String? = null, onSubmitted: (() -> Unit)? = null) {
        val text = inputText.trim()
        if (text.isBlank()) return
        isSubmitting = true
        viewModelScope.launch {
            repository.createComment(ownerId, text, replyToCommentId)
                .onSuccess {
                    inputText = ""
                    onSubmitted?.invoke()
                }
                .onFailure { error = userFacingError(it) }
            isSubmitting = false
        }
    }

    fun removeComment(comment: CommentItem, onRemoved: (() -> Unit)? = null) {
        if (!canDelete(comment)) return
        viewModelScope.launch {
            repository.removeComment(comment.id)
                .onSuccess {
                    comments = comments.filterNot { it.id == comment.id }
                    onRemoved?.invoke()
                }
                .onFailure { error = userFacingError(it) }
        }
    }

    fun toggleLike(comment: CommentItem) {
        viewModelScope.launch {
            repository.updateCommentLike(comment.id, !comment.liked)
                .onSuccess {
                    comments = comments.map {
                        if (it.id == comment.id) {
                            it.copy(
                                liked = !comment.liked,
                                likeCount = if (!comment.liked) {
                                    comment.likeCount + 1
                                } else {
                                    (comment.likeCount - 1).coerceAtLeast(0)
                                }
                            )
                        } else {
                            it
                        }
                    }
                }
                .onFailure { error = userFacingError(it) }
        }
    }

    fun toggleCollect(comment: CommentItem) {
        viewModelScope.launch {
            repository.updateCommentCollect(comment.id, !comment.collected)
                .onSuccess {
                    comments = comments.map {
                        if (it.id == comment.id) it.copy(collected = !comment.collected) else it
                    }
                }
                .onFailure { error = userFacingError(it) }
        }
    }

    private suspend fun ensureViewerUid() {
        if (!viewerUid.isNullOrBlank()) return
        repository.getProfile()
            .onSuccess { viewerUid = it.uid }
            .onFailure { AppLogger.error("comment", "load viewer uid failed message=${it.message}") }
    }

    private fun userFacingError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            "HTTP 401" in message -> "登录状态已失效，请重新登录后再查看评论"
            "HTTP 400" in message -> "评论暂时无法加载，请稍后再试"
            else -> message.ifBlank { "评论暂时无法加载，请稍后再试" }
        }
    }
}
