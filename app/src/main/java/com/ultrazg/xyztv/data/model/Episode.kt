package com.ultrazg.xyztv.data.model

import com.google.gson.annotations.SerializedName

data class Episode(
    @SerializedName("eid") val eid: String = "",
    @SerializedName("pid") val pid: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("shownotes") val shownotes: String? = null,
    @SerializedName("pubDate") val pubDate: String? = null,
    @SerializedName("duration") val duration: Long = 0,
    @SerializedName("isFinished") val isFinished: Boolean = false,
    @SerializedName("enclosure") val enclosure: Enclosure? = null,
    @SerializedName("media") val media: Media? = null,
    @SerializedName("image") val image: PodcastImage? = null,
    @SerializedName("podcast") val podcast: Podcast? = null,
    @SerializedName("isPicked") val isPicked: Boolean = false,
    @SerializedName("isFavorited") val isFavorited: Boolean = false,
    @SerializedName("transcript") val transcript: EpisodeTranscriptMeta? = null,
    @SerializedName("transcriptMediaId") val transcriptMediaId: String? = null
) {
    val audioUrl: String?
        get() = enclosure?.url
            ?: media?.source?.url

    val bestCoverUrl: String?
        get() = image?.largePicUrl
            ?: image?.mediumPicUrl
            ?: image?.smallPicUrl
            ?: podcast?.bestCoverUrl
}

data class EpisodeTranscriptMeta(
    @SerializedName("mediaId") val mediaId: String? = null
)

data class Enclosure(
    @SerializedName("url") val url: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("length") val length: Long = 0
)

data class Media(
    @SerializedName("id") val id: String? = null,
    @SerializedName("source") val source: MediaSource? = null,
    @SerializedName("mimeType") val mimeType: String? = null
)

data class MediaSource(
    @SerializedName("url") val url: String? = null,
    @SerializedName("size") val size: Long = 0
)
