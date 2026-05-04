package com.ultrazg.xyztv.data.model

import com.google.gson.annotations.SerializedName

data class Podcast(
    @SerializedName("pid") val pid: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("episodeCount") val episodeCount: Int = 0,
    @SerializedName("subscriptionCount") val subscriptionCount: Int = 0,
    @SerializedName("subscriptionStatus") val subscriptionStatus: String? = null,
    @SerializedName("subscriptionStar") val subscriptionStar: Boolean = false,
    @SerializedName("latestEpisodePubDate") val latestEpisodePubDate: String? = null,
    @SerializedName("image") val image: PodcastImage? = null,
    @SerializedName("smallSquareImage") val smallSquareImage: ImageUrl? = null,
    @SerializedName("mediumSquareImage") val mediumSquareImage: ImageUrl? = null,
    @SerializedName("largeSquareImage") val largeSquareImage: ImageUrl? = null
) {
    val bestCoverUrl: String?
        get() = image?.largePicUrl
            ?: image?.mediumPicUrl
            ?: image?.smallPicUrl
            ?: largeSquareImage?.url
            ?: mediumSquareImage?.url
            ?: smallSquareImage?.url
}

data class PodcastImage(
    @SerializedName("smallPicUrl") val smallPicUrl: String? = null,
    @SerializedName("mediumPicUrl") val mediumPicUrl: String? = null,
    @SerializedName("largePicUrl") val largePicUrl: String? = null
)

data class ImageUrl(
    @SerializedName("url") val url: String? = null
)
