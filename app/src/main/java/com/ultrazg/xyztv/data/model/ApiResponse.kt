package com.ultrazg.xyztv.data.model

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    @SerializedName("data") val data: List<SearchItem>? = null,
    @SerializedName("loadMoreKey") val loadMoreKey: Any? = null
)

data class SearchItem(
    @SerializedName("type") val type: String = "",
    @SerializedName("podcast") val podcast: Podcast? = null,
    @SerializedName("episode") val episode: Episode? = null,
    @SerializedName("title") val title: String? = null
)

data class DiscoveryResponse(
    @SerializedName("sections") val sections: List<DiscoverySection>? = null,
    @SerializedName("loadMoreKey") val loadMoreKey: Any? = null
)

data class DiscoverySection(
    @SerializedName("title") val title: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("items") val items: List<DiscoveryItem>? = null
)

data class DiscoveryItem(
    @SerializedName("type") val type: String? = null,
    @SerializedName("podcast") val podcast: Podcast? = null,
    @SerializedName("episode") val episode: Episode? = null
)

data class CategorySummary(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String? = null
)

data class CategoryTab(
    @SerializedName("value") val value: String = "",
    @SerializedName("label") val label: String = ""
)

data class SearchPreset(
    @SerializedName("text") val text: String = ""
)

data class UserProfile(
    @SerializedName("uid") val uid: String = "",
    @SerializedName("nickname") val nickname: String = "",
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null
)

data class UserStats(
    @SerializedName("followingCount") val followingCount: Int = 0,
    @SerializedName("followerCount") val followerCount: Int = 0,
    @SerializedName("subscriptionCount") val subscriptionCount: Int = 0,
    @SerializedName("totalPlayedSeconds") val totalPlayedSeconds: Long = 0
)

data class PodcastBulletin(
    @SerializedName("title") val title: String? = null,
    @SerializedName("content") val content: String? = null
)

data class PodcastOwnerInfo(
    @SerializedName("subject") val subject: String? = null,
    @SerializedName("ipLoc") val ipLoc: String? = null
)

data class PodcastHonor(
    @SerializedName("id") val id: String = "",
    @SerializedName("campaignTitle") val campaignTitle: String? = null,
    @SerializedName("title") val title: String = "",
    @SerializedName("url") val url: String? = null
)

data class UserPick(
    @SerializedName("id") val id: String = "",
    @SerializedName("storyText") val storyText: String? = null,
    @SerializedName("pickedAt") val pickedAt: String? = null,
    @SerializedName("episode") val episode: Episode? = null,
    @SerializedName("likeCount") val likeCount: Int = 0,
    @SerializedName("isLiked") val isLiked: Boolean = false
)

data class EditorPickEntry(
    @SerializedName("episode") val episode: Episode? = null,
    @SerializedName("commentText") val commentText: String? = null
)

data class EditorPickDay(
    @SerializedName("date") val date: String = "",
    @SerializedName("entries") val entries: List<EditorPickEntry> = emptyList()
)

data class UserLite(
    @SerializedName("uid") val uid: String = "",
    @SerializedName("nickname") val nickname: String = "",
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("relation") val relation: String? = null,
    @SerializedName("ipLoc") val ipLoc: String? = null,
    @SerializedName("isBlockedByViewer") val isBlockedByViewer: Boolean = false
)

data class UserPreference(
    @SerializedName("isRecentPlayedHidden") val isRecentPlayedHidden: Boolean = false,
    @SerializedName("isListenMileageHiddenInComment") val isListenMileageHiddenInComment: Boolean = false,
    @SerializedName("isStickerLibraryHidden") val isStickerLibraryHidden: Boolean = false,
    @SerializedName("isStickerBoardHidden") val isStickerBoardHidden: Boolean = false,
    @SerializedName("rejectHotPush") val rejectHotPush: Boolean = false,
    @SerializedName("rejectRecommendation") val rejectRecommendation: Boolean = false
)

data class CommentItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("ownerId") val ownerId: String? = null,
    @SerializedName("text") val text: String = "",
    @SerializedName("author") val author: UserLite? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("likeCount") val likeCount: Int = 0,
    @SerializedName("liked") val liked: Boolean = false,
    @SerializedName("collected") val collected: Boolean = false,
    @SerializedName("pinned") val pinned: Boolean = false,
    @SerializedName("threadReplyCount") val threadReplyCount: Int = 0,
    @SerializedName("episode") val episode: Episode? = null,
    @SerializedName("replies") val replies: List<CommentItem> = emptyList(),
    @SerializedName("replyToAuthorName") val replyToAuthorName: String? = null
)

data class StickerItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("issuer") val issuer: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("ownedAt") val ownedAt: String? = null
)

data class StickerBoardItem(
    @SerializedName("sticker") val sticker: StickerItem,
    @SerializedName("x") val x: Int = 0,
    @SerializedName("y") val y: Int = 0,
    @SerializedName("rotation") val rotation: Float = 0f
)

data class MileageOverview(
    @SerializedName("totalPlayedSeconds") val totalPlayedSeconds: Long = 0,
    @SerializedName("lastSevenDayPlayedSeconds") val lastSevenDayPlayedSeconds: Long = 0,
    @SerializedName("lastThirtyDayPlayedSeconds") val lastThirtyDayPlayedSeconds: Long = 0,
    @SerializedName("tagline") val tagline: String? = null
)

data class MileageEntry(
    @SerializedName("playedSeconds") val playedSeconds: Long = 0,
    @SerializedName("podcast") val podcast: Podcast? = null
)

data class ClapBucket(
    @SerializedName("count") val count: Int = 0
)

data class ClapSummary(
    @SerializedName("episodeClaps") val episodeClaps: List<ClapBucket> = emptyList(),
    @SerializedName("myClaps") val myClaps: List<Int> = emptyList()
)

data class TranscriptSentence(
    @SerializedName("index") val index: Int = 0,
    @SerializedName("text") val text: String = "",
    @SerializedName("startMs") val startMs: Long? = null,
    @SerializedName("endMs") val endMs: Long? = null
)

data class PlaybackProgressInfo(
    @SerializedName("eid") val eid: String = "",
    @SerializedName("pid") val pid: String = "",
    @SerializedName("progress") val progress: Int = 0,
    @SerializedName("playedAt") val playedAt: String? = null
)
