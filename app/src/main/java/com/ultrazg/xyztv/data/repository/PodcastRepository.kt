package com.ultrazg.xyztv.data.repository

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.ultrazg.xyztv.api.RetrofitClient
import com.ultrazg.xyztv.data.AppLogger
import com.ultrazg.xyztv.data.TokenManager
import com.ultrazg.xyztv.data.model.CategorySummary
import com.ultrazg.xyztv.data.model.CategoryTab
import com.ultrazg.xyztv.data.model.ClapBucket
import com.ultrazg.xyztv.data.model.ClapSummary
import com.ultrazg.xyztv.data.model.CommentItem
import com.ultrazg.xyztv.data.model.DiscoveryItem
import com.ultrazg.xyztv.data.model.DiscoveryResponse
import com.ultrazg.xyztv.data.model.DiscoverySection
import com.ultrazg.xyztv.data.model.EditorPickDay
import com.ultrazg.xyztv.data.model.EditorPickEntry
import com.ultrazg.xyztv.data.model.Episode
import com.ultrazg.xyztv.data.model.MileageEntry
import com.ultrazg.xyztv.data.model.MileageOverview
import com.ultrazg.xyztv.data.model.PlaybackProgressInfo
import com.ultrazg.xyztv.data.model.PodcastBulletin
import com.ultrazg.xyztv.data.model.PodcastHonor
import com.ultrazg.xyztv.data.model.PodcastOwnerInfo
import com.ultrazg.xyztv.data.model.Podcast
import com.ultrazg.xyztv.data.model.SearchPreset
import com.ultrazg.xyztv.data.model.SearchItem
import com.ultrazg.xyztv.data.model.SearchResponse
import com.ultrazg.xyztv.data.model.StickerBoardItem
import com.ultrazg.xyztv.data.model.StickerItem
import com.ultrazg.xyztv.data.model.TranscriptSentence
import com.ultrazg.xyztv.data.model.UserLite
import com.ultrazg.xyztv.data.model.UserPick
import com.ultrazg.xyztv.data.model.UserPreference
import com.ultrazg.xyztv.data.model.UserProfile
import com.ultrazg.xyztv.data.model.UserStats
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class PodcastRepository {

    private val api = RetrofitClient.instance
    private val officialContentApi = RetrofitClient.officialContentApi
    private val officialUserApi = RetrofitClient.officialUserApi
    private val gson = Gson()
    private val jsonType = "application/json".toMediaType()

    private val podcastType = object : TypeToken<Podcast>() {}.type
    private val episodeType = object : TypeToken<Episode>() {}.type

    private val homeLoadMoreKeys = listOf<String?>(null, "mediumDiscoveryPictorial", "discoveryTopic", "pick")

    private fun jsonBody(vararg pairs: Pair<String, Any?>): RequestBody {
        val obj = JSONObject()
        for ((key, value) in pairs) {
            if (value != null) {
                obj.put(
                    key,
                    when (value) {
                        is Collection<*> -> JSONArray(value)
                        is Array<*> -> JSONArray(value.toList())
                        else -> value
                    }
                )
            }
        }
        return obj.toString().toRequestBody(jsonType)
    }

    private fun ownerObject(id: String, type: String = "EPISODE"): JSONObject =
        JSONObject()
            .put("id", id)
            .put("type", type)

    suspend fun getHomeSections(): Result<List<DiscoverySection>> {
        AppLogger.info("repo", "getHomeSections start")
        return try {
            val sections = mutableListOf<DiscoverySection>()
            val failures = mutableListOf<String>()
            for (loadMoreKey in homeLoadMoreKeys) {
                val pageResult = getDiscovery(loadMoreKey)
                pageResult
                    .onSuccess { page ->
                        AppLogger.info(
                            "repo",
                            "getHomeSections page loadMoreKey=${loadMoreKey ?: "<root>"} sections=${page.sections.orEmpty().size}"
                        )
                        page.sections.orEmpty().forEach { section ->
                            if (section.items.orEmpty().isNotEmpty()) {
                                val duplicate = sections.any { it.title == section.title && it.type == section.type }
                                if (!duplicate) {
                                    sections += section
                                }
                            }
                        }
                    }
                    .onFailure {
                        failures += "${loadMoreKey ?: "<root>"}:${it.message}"
                        AppLogger.info(
                            "repo",
                            "getHomeSections page skipped loadMoreKey=${loadMoreKey ?: "<root>"} message=${it.message}"
                        )
                    }
                if (!TokenManager.isLoggedIn() || failures.any { it.contains("HTTP 401") }) {
                    AppLogger.info("repo", "getHomeSections stopped because auth is no longer valid")
                    break
                }
            }
            val sortedSections = sortHomeSections(sections)
            if (sortedSections.isEmpty() && failures.isNotEmpty()) {
                val message = if (failures.any { it.contains("HTTP 401") }) {
                    "HTTP 401：登录状态已失效，请重新登录"
                } else {
                    "Home discovery pages failed: ${failures.joinToString(" | ").take(300)}"
                }
                return Result.failure(Exception(message))
            }
            AppLogger.info(
                "repo",
                "getHomeSections success sections=${sortedSections.size} titles=${
                    sortedSections.joinToString(" | ") { "${it.title}:${it.items.orEmpty().size}" }.take(500)
                }"
            )
            Result.success(sortedSections)
        } catch (e: Exception) {
            AppLogger.error("repo", "getHomeSections exception", e)
            Result.failure(Exception("Home load failed: ${e.message}"))
        }
    }

    suspend fun getDiscovery(loadMoreKey: String? = null): Result<DiscoveryResponse> {
        AppLogger.info("repo", "getDiscovery loadMoreKey=${loadMoreKey ?: "<root>"}")
        return try {
            val res = api.discovery(
                if (loadMoreKey.isNullOrBlank()) jsonBody("returnAll" to "false")
                else jsonBody("loadMoreKey" to loadMoreKey)
            )
            if (!res.isSuccessful) {
                return Result.failure(httpError("discovery", res.code(), res.errorBody()?.string()))
            }

            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val dataArr = extractDiscoveryArray(normalized)
                ?: return Result.failure(Exception("Discovery data structure mismatch"))
            val parsedSections = parseDiscoverySections(dataArr)
            AppLogger.info(
                "repo",
                "getDiscovery parsed loadMoreKey=${loadMoreKey ?: "<root>"} roots=${dataArr.size()} sections=${parsedSections.size} sample=${
                    parsedSections.joinToString(" | ") { "${it.title}:${it.items.orEmpty().size}" }.take(300)
                }"
            )

            Result.success(
                DiscoveryResponse(
                    sections = parsedSections,
                    loadMoreKey = firstString(normalized, "loadMoreKey", "nextLoadMoreKey")
                        ?: extractEntityObject(normalized)?.let { firstString(it, "loadMoreKey", "nextLoadMoreKey") }
                )
            )
        } catch (e: Exception) {
            AppLogger.error("repo", "getDiscovery exception", e)
            Result.failure(Exception("Discovery load failed: ${e.message}"))
        }
    }

    suspend fun search(keyword: String, type: String = "PODCAST"): Result<SearchResponse> {
        AppLogger.info("repo", "search keyword=${keyword.take(40)} type=$type")
        return try {
            val res = api.search(
                jsonBody(
                    "keyword" to keyword,
                    "type" to type,
                    "limit" to "20",
                    "sourcePageName" to "4",
                    "currentPageName" to "4"
                )
            )
            if (!res.isSuccessful) {
                return Result.failure(httpError("search", res.code(), res.errorBody()?.string()))
            }

            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val dataArr = normalized.getAsJsonArray("data")
                ?: return Result.failure(Exception("Search data structure mismatch"))

            Result.success(
                SearchResponse(
                    data = parseSearchItems(dataArr),
                    loadMoreKey = normalized.get("loadMoreKey")?.toString()
                )
            )
        } catch (e: Exception) {
            AppLogger.error("repo", "search exception keyword=${keyword.take(40)} type=$type", e)
            Result.failure(Exception("Search failed: ${e.message}"))
        }
    }

    suspend fun getSearchPresets(): Result<List<SearchPreset>> {
        AppLogger.info("repo", "getSearchPresets")
        return try {
            val res = officialUserApi.searchPreset()
            if (!res.isSuccessful) {
                return Result.failure(httpError("search_preset", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.success(emptyList())
            Result.success(parseSearchPresets(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getSearchPresets exception", e)
            Result.failure(Exception("Search preset load failed: ${e.message}"))
        }
    }

    suspend fun getSubscriptions(): Result<List<Podcast>> {
        AppLogger.info("repo", "getSubscriptions")
        return try {
            val res = officialUserApi.subscriptionList(
                jsonBody(
                    "limit" to "20",
                    "sortOrder" to "desc",
                    "sortBy" to "subscribedAt"
                )
            )
            if (!res.isSuccessful) {
                return Result.failure(httpError("subscription", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Subscription structure mismatch"))
            Result.success(parsePodcasts(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getSubscriptions exception", e)
            Result.failure(Exception("Subscription load failed: ${e.message}"))
        }
    }

    suspend fun updateSubscription(pid: String, mode: String): Result<Unit> {
        AppLogger.info("repo", "updateSubscription pid=$pid mode=$mode")
        return try {
            val res = officialUserApi.subscriptionUpdate(jsonBody("pid" to pid, "mode" to mode))
            if (!res.isSuccessful) {
                return Result.failure(httpError("subscription_update", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "updateSubscription exception pid=$pid mode=$mode", e)
            Result.failure(Exception("Subscription update failed: ${e.message}"))
        }
    }

    suspend fun getFavoriteEpisodes(): Result<List<Episode>> {
        AppLogger.info("repo", "getFavoriteEpisodes")
        return try {
            val res = officialUserApi.favoriteEpisodeList(jsonBody())
            if (!res.isSuccessful) {
                return Result.failure(httpError("favorite_episode_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Favorite list structure mismatch"))
            Result.success(parseEpisodes(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getFavoriteEpisodes exception", e)
            Result.failure(Exception("Favorite list load failed: ${e.message}"))
        }
    }

    suspend fun updateFavoriteEpisode(eid: String, favorited: Boolean): Result<Unit> {
        AppLogger.info("repo", "updateFavoriteEpisode eid=$eid favorited=$favorited")
        return try {
            val res = officialUserApi.favoriteEpisodeUpdate(
                jsonBody(
                    "eid" to eid,
                    "favorited" to favorited,
                    "sourcePageName" to 8,
                    "currentPageName" to 9
                )
            )
            if (!res.isSuccessful) {
                return Result.failure(httpError("favorite_episode_update", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "updateFavoriteEpisode exception eid=$eid", e)
            Result.failure(Exception("Favorite update failed: ${e.message}"))
        }
    }

    suspend fun getPlayedHistory(): Result<List<Episode>> {
        AppLogger.info("repo", "getPlayedHistory")
        return try {
            val res = officialUserApi.playedHistoryList(jsonBody())
            if (!res.isSuccessful) {
                return Result.failure(httpError("episode_played_history_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Played history structure mismatch"))
            Result.success(parseEpisodes(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getPlayedHistory exception", e)
            Result.failure(Exception("Played history load failed: ${e.message}"))
        }
    }

    suspend fun markEpisodePlayed(eid: String): Result<Unit> {
        AppLogger.info("repo", "markEpisodePlayed eid=$eid")
        return try {
            val res = officialUserApi.playedHistoryCreate(jsonBody("eid" to eid))
            if (!res.isSuccessful) {
                return Result.failure(httpError("episode_played_history_list_update", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "markEpisodePlayed exception eid=$eid", e)
            Result.failure(Exception("Mark played failed: ${e.message}"))
        }
    }

    suspend fun getPlaybackProgress(eid: String): Result<PlaybackProgressInfo?> {
        AppLogger.info("repo", "getPlaybackProgress eid=$eid")
        return try {
            val res = officialUserApi.playbackProgressList(jsonBody("eids" to listOf(eid)))
            if (!res.isSuccessful) {
                return Result.failure(httpError("episode_play_progress", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: JsonArray()
            val first = arr.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
            Result.success(first?.let(::parsePlaybackProgress))
        } catch (e: Exception) {
            AppLogger.error("repo", "getPlaybackProgress exception eid=$eid", e)
            Result.failure(Exception("Playback progress load failed: ${e.message}"))
        }
    }

    suspend fun updatePlaybackProgress(
        eid: String,
        pid: String,
        progress: Int,
        playedAt: String
    ): Result<Unit> {
        AppLogger.info("repo", "updatePlaybackProgress eid=$eid pid=$pid progress=$progress")
        return try {
            val item = JSONObject()
                .put("eid", eid)
                .put("pid", pid)
                .put("progress", progress.coerceAtLeast(0))
                .put("playedAt", playedAt)
            val body = JSONObject().put("data", org.json.JSONArray().put(item))
                .toString()
                .toRequestBody("application/json".toMediaType())
            val res = officialUserApi.playbackProgressUpdate(body)
            if (!res.isSuccessful) {
                return Result.failure(httpError("episode_play_progress_update", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "updatePlaybackProgress exception eid=$eid pid=$pid", e)
            Result.failure(Exception("Playback progress update failed: ${e.message}"))
        }
    }

    suspend fun getProfile(): Result<UserProfile> {
        AppLogger.info("repo", "getProfile")
        return try {
            val res = officialUserApi.profile()
            if (!res.isSuccessful) {
                return Result.failure(httpError("profile", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val obj = extractEntityObject(normalized) ?: normalized
            Result.success(parseUserProfile(obj))
        } catch (e: Exception) {
            AppLogger.error("repo", "getProfile exception", e)
            Result.failure(Exception("Profile load failed: ${e.message}"))
        }
    }

    suspend fun getUserStats(uid: String): Result<UserStats> {
        AppLogger.info("repo", "getUserStats uid=$uid")
        return try {
            val res = officialUserApi.userStats(uid)
            if (!res.isSuccessful) {
                return Result.failure(httpError("user_stats", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val obj = extractEntityObject(normalized) ?: normalized
            Result.success(parseUserStats(obj))
        } catch (e: Exception) {
            AppLogger.error("repo", "getUserStats exception uid=$uid", e)
            Result.failure(Exception("User stats load failed: ${e.message}"))
        }
    }

    suspend fun getUnreadCount(): Result<Int> {
        AppLogger.info("repo", "getUnreadCount")
        return try {
            val res = officialUserApi.unreadCount()
            if (!res.isSuccessful) {
                return Result.failure(httpError("unread_count", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            Result.success(firstInt(normalized, "count", "unreadCount", "unread", "messageCount") ?: 0)
        } catch (e: Exception) {
            AppLogger.error("repo", "getUnreadCount exception", e)
            Result.failure(Exception("Unread count load failed: ${e.message}"))
        }
    }

    suspend fun getOwnedPodcasts(uid: String): Result<List<Podcast>> {
        AppLogger.info("repo", "getOwnedPodcasts uid=$uid")
        return try {
            val res = officialUserApi.ownedPodcasts(jsonBody("uid" to uid))
            if (!res.isSuccessful) {
                return Result.failure(httpError("owned_podcasts", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Owned podcasts structure mismatch"))
            Result.success(parsePodcasts(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getOwnedPodcasts exception uid=$uid", e)
            Result.failure(Exception("Owned podcasts load failed: ${e.message}"))
        }
    }

    suspend fun getTopList(category: String): Result<List<Episode>> {
        AppLogger.info("repo", "getTopList category=$category")
        return try {
            val upstreamCategory = when (category.uppercase()) {
                "HOT" -> "HOT_EPISODES_IN_24_HOURS"
                "ROCK" -> "SKYROCKET_EPISODES"
                "NEW" -> "NEW_STAR_EPISODES"
                else -> "HOT_EPISODES_IN_24_HOURS"
            }
            val res = officialUserApi.topList(upstreamCategory)
            if (!res.isSuccessful) {
                return Result.failure(httpError("top_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized)
                ?: return Result.failure(Exception("Top list structure mismatch for $upstreamCategory: ${normalized.toString().take(300)}"))
            Result.success(parseEpisodes(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getTopList exception category=$category", e)
            Result.failure(Exception("Top list load failed: ${e.message}"))
        }
    }

    suspend fun getRelatedPodcasts(pid: String): Result<List<Podcast>> {
        AppLogger.info("repo", "getRelatedPodcasts pid=$pid")
        return try {
            val res = officialUserApi.relatedPodcasts(jsonBody("pid" to pid, "position" to "BOTTOM"))
            if (!res.isSuccessful) {
                return Result.failure(httpError("podcast_related", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Related podcast structure mismatch"))
            Result.success(parsePodcasts(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getRelatedPodcasts exception pid=$pid", e)
            Result.failure(Exception("Related podcasts load failed: ${e.message}"))
        }
    }

    suspend fun getPodcastBulletin(pid: String): Result<PodcastBulletin?> {
        AppLogger.info("repo", "getPodcastBulletin pid=$pid")
        return try {
            val res = officialUserApi.podcastBulletin(pid)
            if (!res.isSuccessful) {
                return Result.failure(httpError("podcast_bulletin", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.success(null)
            val normalized = normalizeRoot(raw)
            val obj = extractEntityObject(normalized) ?: normalized
            Result.success(parsePodcastBulletin(obj))
        } catch (e: Exception) {
            AppLogger.error("repo", "getPodcastBulletin exception pid=$pid", e)
            Result.failure(Exception("Podcast bulletin load failed: ${e.message}"))
        }
    }

    suspend fun getPodcastInfo(pid: String): Result<PodcastOwnerInfo> {
        AppLogger.info("repo", "getPodcastInfo pid=$pid")
        return try {
            val res = officialUserApi.podcastInfo(pid)
            if (!res.isSuccessful) {
                return Result.failure(httpError("podcast_get_info", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val obj = extractEntityObject(normalized) ?: normalized
            Result.success(parsePodcastOwnerInfo(obj))
        } catch (e: Exception) {
            AppLogger.error("repo", "getPodcastInfo exception pid=$pid", e)
            Result.failure(Exception("Podcast info load failed: ${e.message}"))
        }
    }

    suspend fun getPodcastHonorList(pid: String): Result<List<PodcastHonor>> {
        AppLogger.info("repo", "getPodcastHonorList pid=$pid")
        return try {
            val res = officialUserApi.podcastHonorList(jsonBody("pid" to pid))
            if (!res.isSuccessful) {
                return Result.failure(httpError("podcast_honor_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Podcast honor structure mismatch"))
            Result.success(parsePodcastHonors(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getPodcastHonorList exception pid=$pid", e)
            Result.failure(Exception("Podcast honor load failed: ${e.message}"))
        }
    }

    suspend fun getPopularEpisodes(pid: String): Result<List<Episode>> {
        AppLogger.info("repo", "getPopularEpisodes pid=$pid")
        return try {
            val res = officialUserApi.popularEpisodeList(jsonBody("pid" to pid))
            if (!res.isSuccessful) {
                return Result.failure(httpError("episode_list_by_filter", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Popular episodes structure mismatch"))
            Result.success(parseEpisodes(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getPopularEpisodes exception pid=$pid", e)
            Result.failure(Exception("Popular episodes load failed: ${e.message}"))
        }
    }

    suspend fun getInboxEpisodes(): Result<List<Episode>> {
        AppLogger.info("repo", "getInboxEpisodes")
        return try {
            val res = officialUserApi.inboxList(jsonBody())
            if (!res.isSuccessful) {
                return Result.failure(httpError("inbox_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Inbox structure mismatch"))
            Result.success(parseEpisodes(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getInboxEpisodes exception", e)
            Result.failure(Exception("Inbox load failed: ${e.message}"))
        }
    }

    suspend fun getPilotDiscoveryEpisodes(): Result<List<Episode>> {
        AppLogger.info("repo", "getPilotDiscoveryEpisodes")
        return try {
            val res = officialUserApi.pilotDiscoveryList(jsonBody())
            if (!res.isSuccessful) {
                return Result.failure(httpError("pilot_discovery_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Pilot discovery structure mismatch"))
            Result.success(parseEpisodes(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getPilotDiscoveryEpisodes exception", e)
            Result.failure(Exception("Pilot discovery load failed: ${e.message}"))
        }
    }

    suspend fun getEditorPickHistory(): Result<List<EditorPickDay>> {
        AppLogger.info("repo", "getEditorPickHistory")
        return try {
            val res = officialUserApi.editorPickListHistory(jsonBody())
            if (!res.isSuccessful) {
                return Result.failure(httpError("editor_pick_list_history", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Editor pick structure mismatch"))
            Result.success(parseEditorPickDays(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getEditorPickHistory exception", e)
            Result.failure(Exception("Editor pick history load failed: ${e.message}"))
        }
    }

    suspend fun getRecentPicks(uid: String): Result<List<UserPick>> {
        AppLogger.info("repo", "getRecentPicks uid=$uid")
        return try {
            val res = officialUserApi.pickListRecent(jsonBody("uid" to uid))
            if (!res.isSuccessful) {
                return Result.failure(httpError("pick_list_recent", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Recent pick structure mismatch"))
            Result.success(parseUserPicks(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getRecentPicks exception uid=$uid", e)
            Result.failure(Exception("Recent picks load failed: ${e.message}"))
        }
    }

    suspend fun getPickHistory(uid: String): Result<List<UserPick>> {
        AppLogger.info("repo", "getPickHistory uid=$uid")
        return try {
            val res = officialUserApi.pickListHistory(jsonBody("uid" to uid))
            if (!res.isSuccessful) {
                return Result.failure(httpError("pick_list_history", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Pick history structure mismatch"))
            Result.success(parseUserPicks(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getPickHistory exception uid=$uid", e)
            Result.failure(Exception("Pick history load failed: ${e.message}"))
        }
    }

    suspend fun getStarSubscriptions(): Result<List<Podcast>> {
        AppLogger.info("repo", "getStarSubscriptions")
        return try {
            val res = officialUserApi.starSubscriptionList(jsonBody())
            if (!res.isSuccessful) {
                return Result.failure(httpError("subscription_star_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Star subscription structure mismatch"))
            Result.success(parsePodcasts(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getStarSubscriptions exception", e)
            Result.failure(Exception("Star subscriptions load failed: ${e.message}"))
        }
    }

    suspend fun getNonStarredSubscriptions(): Result<List<Podcast>> {
        AppLogger.info("repo", "getNonStarredSubscriptions")
        return try {
            val res = officialUserApi.nonStarredSubscriptionList(jsonBody())
            if (!res.isSuccessful) {
                return Result.failure(httpError("subscription_non_starred_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Non-star subscription structure mismatch"))
            Result.success(parsePodcasts(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getNonStarredSubscriptions exception", e)
            Result.failure(Exception("Non-star subscriptions load failed: ${e.message}"))
        }
    }

    suspend fun updateStarSubscription(pid: String, withStar: Boolean): Result<Unit> {
        AppLogger.info("repo", "updateStarSubscription pid=$pid withStar=$withStar")
        return try {
            val res = officialUserApi.starSubscriptionUpdate(jsonBody("pid" to pid, "withStar" to withStar))
            if (!res.isSuccessful) {
                return Result.failure(httpError("subscription_star_update", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "updateStarSubscription exception pid=$pid", e)
            Result.failure(Exception("Star subscription update failed: ${e.message}"))
        }
    }

    suspend fun getFollowing(uid: String): Result<List<UserLite>> {
        AppLogger.info("repo", "getFollowing uid=$uid")
        return try {
            val res = officialUserApi.followingList(jsonBody("uid" to uid))
            if (!res.isSuccessful) {
                return Result.failure(httpError("following_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Following structure mismatch"))
            Result.success(parseUsers(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getFollowing exception uid=$uid", e)
            Result.failure(Exception("Following load failed: ${e.message}"))
        }
    }

    suspend fun getFollowers(uid: String): Result<List<UserLite>> {
        AppLogger.info("repo", "getFollowers uid=$uid")
        return try {
            val res = officialUserApi.followerList(jsonBody("uid" to uid))
            if (!res.isSuccessful) {
                return Result.failure(httpError("follower_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Follower structure mismatch"))
            Result.success(parseUsers(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getFollowers exception uid=$uid", e)
            Result.failure(Exception("Followers load failed: ${e.message}"))
        }
    }

    suspend fun updateRelation(uid: String, relation: String): Result<Unit> {
        AppLogger.info("repo", "updateRelation uid=$uid relation=$relation")
        return try {
            val res = officialUserApi.relationUpdate(jsonBody("uid" to uid, "relation" to relation))
            if (!res.isSuccessful) {
                return Result.failure(httpError("user_relation_update", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "updateRelation exception uid=$uid relation=$relation", e)
            Result.failure(Exception("Relation update failed: ${e.message}"))
        }
    }

    suspend fun getUserPreference(): Result<UserPreference> {
        AppLogger.info("repo", "getUserPreference")
        return try {
            val res = officialUserApi.userPreferenceGet()
            if (!res.isSuccessful) {
                return Result.failure(httpError("user_preference_get", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val obj = extractEntityObject(normalized) ?: normalized
            Result.success(parseUserPreference(obj))
        } catch (e: Exception) {
            AppLogger.error("repo", "getUserPreference exception", e)
            Result.failure(Exception("Preference load failed: ${e.message}"))
        }
    }

    suspend fun updateUserPreference(type: String, flag: Boolean): Result<Unit> {
        AppLogger.info("repo", "updateUserPreference type=$type flag=$flag")
        return try {
            val res = officialUserApi.userPreferenceUpdate(jsonBody(type to flag))
            if (!res.isSuccessful) {
                return Result.failure(httpError("user_preference_update", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "updateUserPreference exception type=$type", e)
            Result.failure(Exception("Preference update failed: ${e.message}"))
        }
    }

    suspend fun getPrimaryComments(eid: String, order: String = "HOT"): Result<List<CommentItem>> {
        AppLogger.info("repo", "getPrimaryComments eid=$eid order=$order")
        return try {
            val bodies = listOf(
                jsonBody("owner" to ownerObject(eid), "order" to order, "limit" to 20),
                jsonBody("ownerId" to eid, "ownerType" to "EPISODE", "order" to order, "limit" to 20),
                jsonBody("id" to eid, "type" to "EPISODE", "order" to order, "limit" to 20)
            )
            var lastError: Exception? = null
            for ((index, body) in bodies.withIndex()) {
                val res = officialUserApi.commentPrimary(body)
                if (!res.isSuccessful) {
                    lastError = httpError("comment_primary[$index]", res.code(), res.errorBody()?.string())
                    continue
                }
                val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
                val normalized = normalizeRoot(raw)
                val arr = extractArray(normalized) ?: return Result.failure(Exception("Comment structure mismatch"))
                return Result.success(parseComments(arr))
            }
            Result.failure(lastError ?: Exception("Comment load failed"))
        } catch (e: Exception) {
            AppLogger.error("repo", "getPrimaryComments exception eid=$eid", e)
            Result.failure(Exception("Comment load failed: ${e.message}"))
        }
    }

    suspend fun getCommentThread(primaryCommentId: String, order: String = "SMART"): Result<List<CommentItem>> {
        AppLogger.info("repo", "getCommentThread primaryCommentId=$primaryCommentId order=$order")
        return try {
            val res = officialUserApi.commentThread(
                jsonBody("primaryCommentId" to primaryCommentId, "order" to order)
            )
            if (!res.isSuccessful) {
                return Result.failure(httpError("comment_thread", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Comment thread structure mismatch"))
            Result.success(parseComments(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getCommentThread exception primaryCommentId=$primaryCommentId", e)
            Result.failure(Exception("Comment thread load failed: ${e.message}"))
        }
    }

    suspend fun updateCommentLike(commentId: String, liked: Boolean, type: String = "COMMENT"): Result<Unit> {
        AppLogger.info("repo", "updateCommentLike commentId=$commentId liked=$liked type=$type")
        return try {
            val res = officialUserApi.commentLikeUpdate(
                jsonBody(
                    "id" to commentId,
                    "liked" to liked,
                    "type" to type
                )
            )
            if (!res.isSuccessful) {
                return Result.failure(httpError("comment_like_update", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "updateCommentLike exception commentId=$commentId", e)
            Result.failure(Exception("Comment like update failed: ${e.message}"))
        }
    }

    suspend fun updateCommentCollect(commentId: String, collected: Boolean): Result<Unit> {
        AppLogger.info("repo", "updateCommentCollect commentId=$commentId collected=$collected")
        return try {
            val res = if (collected) {
                officialUserApi.commentCollectCreate(jsonBody("commentId" to commentId))
            } else {
                officialUserApi.commentCollectRemove(jsonBody("commentId" to commentId))
            }
            if (!res.isSuccessful) {
                return Result.failure(httpError("comment_collect_update", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "updateCommentCollect exception commentId=$commentId", e)
            Result.failure(Exception("Comment collect update failed: ${e.message}"))
        }
    }

    suspend fun getCollectedComments(): Result<List<CommentItem>> {
        AppLogger.info("repo", "getCollectedComments")
        return try {
            val res = officialUserApi.commentCollectList(jsonBody())
            if (!res.isSuccessful) {
                return Result.failure(httpError("comment_collect_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Collected comments structure mismatch"))
            Result.success(parseComments(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getCollectedComments exception", e)
            Result.failure(Exception("Collected comments load failed: ${e.message}"))
        }
    }

    suspend fun createComment(ownerId: String, text: String, replyToCommentId: String? = null): Result<Unit> {
        AppLogger.info("repo", "createComment ownerId=$ownerId replyTo=${replyToCommentId ?: "<root>"}")
        return try {
            val res = officialUserApi.commentCreate(
                jsonBody(
                    "text" to text,
                    "id" to ownerId,
                    "type" to "EPISODE",
                    "replyToCommentId" to replyToCommentId
                )
            )
            if (!res.isSuccessful) {
                return Result.failure(httpError("comment_create", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "createComment exception ownerId=$ownerId", e)
            Result.failure(Exception("Comment create failed: ${e.message}"))
        }
    }

    suspend fun removeComment(commentId: String): Result<Unit> {
        AppLogger.info("repo", "removeComment commentId=$commentId")
        return try {
            val res = officialUserApi.commentRemove(jsonBody("commentId" to commentId))
            if (!res.isSuccessful) {
                return Result.failure(httpError("comment_remove", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "removeComment exception commentId=$commentId", e)
            Result.failure(Exception("Comment remove failed: ${e.message}"))
        }
    }

    suspend fun getBlockedUsers(): Result<List<UserLite>> {
        AppLogger.info("repo", "getBlockedUsers")
        return try {
            val res = officialUserApi.blockedUserList(jsonBody())
            if (!res.isSuccessful) {
                return Result.failure(httpError("blocked_user_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Blocked user structure mismatch"))
            Result.success(parseUsers(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getBlockedUsers exception", e)
            Result.failure(Exception("Blocked users load failed: ${e.message}"))
        }
    }

    suspend fun updateBlockedUser(uid: String, blocked: Boolean): Result<Unit> {
        AppLogger.info("repo", "updateBlockedUser uid=$uid blocked=$blocked")
        return try {
            val res = if (blocked) {
                officialUserApi.blockedUserCreate(jsonBody("uid" to uid))
            } else {
                officialUserApi.blockedUserRemove(jsonBody("uid" to uid))
            }
            if (!res.isSuccessful) {
                return Result.failure(httpError("blocked_user_update", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "updateBlockedUser exception uid=$uid", e)
            Result.failure(Exception("Blocked user update failed: ${e.message}"))
        }
    }

    suspend fun getMileageOverview(): Result<MileageOverview> {
        AppLogger.info("repo", "getMileageOverview")
        return try {
            val res = officialUserApi.mileageGet()
            if (!res.isSuccessful) {
                return Result.failure(httpError("mileage_get", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val obj = extractEntityObject(normalized) ?: normalized
            Result.success(parseMileageOverview(obj))
        } catch (e: Exception) {
            AppLogger.error("repo", "getMileageOverview exception", e)
            Result.failure(Exception("Mileage overview load failed: ${e.message}"))
        }
    }

    suspend fun getMileageList(all: Boolean): Result<List<MileageEntry>> {
        AppLogger.info("repo", "getMileageList all=$all")
        return try {
            val res = officialUserApi.mileageList(jsonBody("all" to all))
            if (!res.isSuccessful) {
                return Result.failure(httpError("mileage_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Mileage list structure mismatch"))
            Result.success(parseMileageEntries(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getMileageList exception all=$all", e)
            Result.failure(Exception("Mileage list load failed: ${e.message}"))
        }
    }

    suspend fun updateMileage(
        eid: String,
        pid: String,
        startPlayingTimestamp: Double,
        endPlayingTimestamp: Double,
        withSpeed: Float = 1f,
        isSpeaker: Boolean = true
    ): Result<Unit> {
        AppLogger.info("repo", "updateMileage eid=$eid pid=$pid")
        return try {
            val tracking = JSONObject().apply {
                put("eid", eid)
                put("pid", pid)
                put("startPlayingTimestamp", startPlayingTimestamp)
                put("endPlayingTimestamp", endPlayingTimestamp)
                put("isSpeaker", isSpeaker)
                put("isOffline", false)
                put("isTrial", false)
                put("withSpeed", withSpeed.toDouble())
            }
            val body = JSONObject().put("tracking", org.json.JSONArray().put(tracking))
                .toString()
                .toRequestBody(jsonType)
            val res = officialUserApi.mileageUpdate(body)
            if (!res.isSuccessful) {
                return Result.failure(httpError("mileage_update", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "updateMileage exception eid=$eid pid=$pid", e)
            Result.failure(Exception("Mileage update failed: ${e.message}"))
        }
    }

    suspend fun getClapSummary(eid: String, duration: Int): Result<ClapSummary> {
        AppLogger.info("repo", "getClapSummary eid=$eid duration=$duration")
        return try {
            val res = officialUserApi.clapList(jsonBody("eid" to eid, "duration" to duration))
            if (!res.isSuccessful) {
                return Result.failure(httpError("clap_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val obj = extractEntityObject(normalized) ?: normalized
            Result.success(parseClapSummary(obj))
        } catch (e: Exception) {
            AppLogger.error("repo", "getClapSummary exception eid=$eid", e)
            Result.failure(Exception("Clap summary load failed: ${e.message}"))
        }
    }

    suspend fun createClap(eid: String, timestamp: Long, duration: Long): Result<Unit> {
        AppLogger.info("repo", "createClap eid=$eid timestamp=$timestamp duration=$duration")
        return try {
            val res = officialUserApi.clapCreate(
                jsonBody("eid" to eid, "timestamp" to timestamp, "duration" to duration)
            )
            if (!res.isSuccessful) {
                return Result.failure(httpError("clap_create", res.code(), res.errorBody()?.string()))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.error("repo", "createClap exception eid=$eid", e)
            Result.failure(Exception("Clap create failed: ${e.message}"))
        }
    }

    suspend fun getStickers(uid: String): Result<List<StickerItem>> {
        AppLogger.info("repo", "getStickers uid=$uid")
        return try {
            val res = officialUserApi.stickerList(jsonBody("uid" to uid))
            if (!res.isSuccessful) {
                return Result.failure(httpError("sticker_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized) ?: return Result.failure(Exception("Sticker structure mismatch"))
            Result.success(parseStickers(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getStickers exception uid=$uid", e)
            Result.failure(Exception("Sticker list load failed: ${e.message}"))
        }
    }

    suspend fun getStickerBoard(uid: String): Result<List<StickerBoardItem>> {
        AppLogger.info("repo", "getStickerBoard uid=$uid")
        return try {
            val res = officialUserApi.stickerBoard(jsonBody("uid" to uid))
            if (!res.isSuccessful) {
                return Result.failure(httpError("sticker_board", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val dataObject = extractEntityObject(normalized) ?: normalized
            val stickers = dataObject.getAsJsonArray("stickers") ?: return Result.success(emptyList())
            Result.success(parseStickerBoardItems(stickers))
        } catch (e: Exception) {
            AppLogger.error("repo", "getStickerBoard exception uid=$uid", e)
            Result.failure(Exception("Sticker board load failed: ${e.message}"))
        }
    }

    suspend fun getCategories(): Result<List<CategorySummary>> {
        AppLogger.info("repo", "getCategories")
        return try {
            val res = officialContentApi.categoryList(jsonBody())
            if (!res.isSuccessful) {
                return Result.failure(httpError("category_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized)
                ?: return Result.failure(Exception("Category data structure mismatch"))
            Result.success(parseCategories(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getCategories exception", e)
            Result.failure(Exception("Category load failed: ${e.message}"))
        }
    }

    suspend fun getCategoryTabs(categoryId: String): Result<List<CategoryTab>> {
        AppLogger.info("repo", "getCategoryTabs categoryId=$categoryId")
        return try {
            val res = officialContentApi.categoryTabs(jsonBody("categoryId" to categoryId))
            if (!res.isSuccessful) {
                return Result.failure(httpError("category_tabs", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized)
                ?: return Result.failure(Exception("Category tabs structure mismatch"))
            Result.success(parseCategoryTabs(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getCategoryTabs exception categoryId=$categoryId", e)
            Result.failure(Exception("Category tabs load failed: ${e.message}"))
        }
    }

    suspend fun getCategoryPodcasts(categoryId: String, tab: String): Result<List<Podcast>> {
        AppLogger.info("repo", "getCategoryPodcasts categoryId=$categoryId tab=$tab")
        return try {
            val res = officialContentApi.categoryPodcastList(
                jsonBody(
                    "categoryId" to categoryId,
                    "tab" to tab,
                    "omitSubscribed" to false
                )
            )
            if (!res.isSuccessful) {
                return Result.failure(httpError("category_podcast_list", res.code(), res.errorBody()?.string()))
            }
            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val arr = extractArray(normalized)
                ?: return Result.failure(Exception("Category podcast list structure mismatch"))
            Result.success(parsePodcasts(arr))
        } catch (e: Exception) {
            AppLogger.error("repo", "getCategoryPodcasts exception categoryId=$categoryId tab=$tab", e)
            Result.failure(Exception("Category podcasts load failed: ${e.message}"))
        }
    }

    suspend fun getPodcastDetail(pid: String): Result<Podcast> {
        AppLogger.info("repo", "getPodcastDetail pid=$pid")
        return try {
            val res = api.podcastDetail(jsonBody("pid" to pid))
            if (!res.isSuccessful) {
                return Result.failure(httpError("podcast_detail", res.code(), res.errorBody()?.string()))
            }

            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val dataObj = extractEntityObject(normalized)
                ?: return Result.failure(Exception("Podcast detail structure mismatch"))

            Result.success(gson.fromJson(dataObj, podcastType))
        } catch (e: Exception) {
            AppLogger.error("repo", "getPodcastDetail exception pid=$pid", e)
            Result.failure(Exception("Podcast detail load failed: ${e.message}"))
        }
    }

    suspend fun getEpisodeList(pid: String): Result<List<Episode>> {
        AppLogger.info("repo", "getEpisodeList pid=$pid")
        return try {
            val res = api.episodeList(
                jsonBody(
                    "pid" to pid,
                    "order" to "desc",
                    "limit" to "20"
                )
            )
            if (!res.isSuccessful) {
                return Result.failure(httpError("episode_list", res.code(), res.errorBody()?.string()))
            }

            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val dataArr = normalized.getAsJsonArray("data")
                ?: return Result.failure(Exception("Episode list structure mismatch"))

            val episodes = mutableListOf<Episode>()
            for (i in 0 until dataArr.size()) {
                episodes.add(gson.fromJson(dataArr[i], episodeType))
            }
            Result.success(episodes)
        } catch (e: Exception) {
            AppLogger.error("repo", "getEpisodeList exception pid=$pid", e)
            Result.failure(Exception("Episode list load failed: ${e.message}"))
        }
    }

    suspend fun getEpisodeDetail(eid: String): Result<Episode> {
        AppLogger.info("repo", "getEpisodeDetail eid=$eid")
        return try {
            val res = api.episodeDetail(jsonBody("eid" to eid))
            if (!res.isSuccessful) {
                return Result.failure(httpError("episode_detail", res.code(), res.errorBody()?.string()))
            }

            val raw = res.body()?.string() ?: return Result.failure(Exception("Empty response body"))
            val normalized = normalizeRoot(raw)
            val dataObj = extractEntityObject(normalized)
                ?: return Result.failure(Exception("Episode detail structure mismatch"))

            Result.success(gson.fromJson(dataObj, episodeType))
        } catch (e: Exception) {
            AppLogger.error("repo", "getEpisodeDetail exception eid=$eid", e)
            Result.failure(Exception("Episode detail load failed: ${e.message}"))
        }
    }

    suspend fun getEpisodeTranscript(eid: String): Result<List<TranscriptSentence>> {
        AppLogger.info("repo", "getEpisodeTranscript eid=$eid")
        val versions = listOf("release", "asr", "draft")
        var lastError: Exception? = null
        for (version in versions) {
            try {
                val res = RetrofitClient.authApi.episodeTranscriptGet(
                    jsonBody("eid" to eid, "version" to version)
                )
                if (!res.isSuccessful) {
                    lastError = httpError("episode_transcript[$version]", res.code(), res.errorBody()?.string())
                    continue
                }
                val raw = res.body()?.string() ?: continue
                val trimmed = raw.trimStart()
                if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                    lastError = Exception("Transcript $version response is not JSON")
                    AppLogger.info("repo", "getEpisodeTranscript non-json response eid=$eid version=$version")
                    continue
                }
                val normalized = normalizeRoot(raw)
                val dataObj = extractEntityObject(normalized) ?: normalized
                val sentences = findTranscriptSentencesArray(dataObj)
                    ?: findTranscriptSentencesArray(normalized)
                    ?: continue
                val parsed = parseTranscriptSentences(sentences)
                val timed = parsed.filter { it.startMs != null }
                if (timed.isNotEmpty()) return Result.success(timed)
            } catch (e: Exception) {
                lastError = Exception("Transcript $version load failed: ${e.message}")
                AppLogger.info("repo", "getEpisodeTranscript unavailable eid=$eid version=$version message=${e.message}")
            }
        }
        return Result.failure(lastError ?: Exception("Transcript not available"))
    }

    private fun normalizeRoot(raw: String): JsonObject {
        var current = JsonParser().parse(raw).asJsonObject
        while (current.has("code")) {
            val wrappedData = current.get("data") ?: break
            if (!wrappedData.isJsonObject) break
            current = wrappedData.asJsonObject
        }
        return current
    }

    private fun extractEntityObject(root: JsonObject): JsonObject? {
        val data = root.get("data")
        return when {
            data == null -> root
            data.isJsonObject -> data.asJsonObject
            else -> null
        }
    }

    private fun extractArray(root: JsonObject): JsonArray? {
        val arrayKeys = listOf("data", "items", "podcasts", "tabs", "categories")
        return extractArrayRecursive(root, arrayKeys, depth = 0)
    }

    private fun extractDiscoveryArray(root: JsonObject): JsonArray? {
        return extractArrayRecursive(
            root = root,
            arrayKeys = listOf("data", "items", "modules", "target", "contents", "list"),
            depth = 0
        )
    }

    private fun extractArrayRecursive(
        root: JsonObject,
        arrayKeys: List<String>,
        depth: Int
    ): JsonArray? {
        if (depth > 4) return null
        for (key in arrayKeys) {
            val value = root.get(key) ?: continue
            if (value.isJsonArray) {
                return value.asJsonArray
            }
            if (value.isJsonObject) {
                extractArrayRecursive(value.asJsonObject, arrayKeys, depth + 1)?.let { return it }
            }
        }
        return null
    }

    private fun parseSearchItems(dataArr: JsonArray): List<SearchItem> {
        val items = mutableListOf<SearchItem>()
        for (i in 0 until dataArr.size()) {
            val obj = dataArr[i].asJsonObject
            when (obj.get("type")?.asString?.uppercase()) {
                "PODCAST" -> {
                    val podcast = gson.fromJson<Podcast>(obj, podcastType)
                    items.add(SearchItem(type = "PODCAST", podcast = podcast, title = podcast.title))
                }

                "EPISODE" -> {
                    val episode = gson.fromJson<Episode>(obj, episodeType)
                    items.add(
                        SearchItem(
                            type = "EPISODE",
                            podcast = episode.podcast,
                            episode = episode,
                            title = episode.title
                        )
                    )
                }
            }
        }
        return items
    }

    private fun parseDiscoverySections(dataArr: JsonArray): List<DiscoverySection> {
        val sections = mutableListOf<DiscoverySection>()
        for (i in 0 until dataArr.size()) {
            val root = dataArr[i].asJsonObject
            when (val rootType = root.get("type")?.asString?.uppercase()) {
                "DISCOVERY_COLLECTION" -> sections += parseDiscoveryCollection(root)
                "NEW_POWER" -> parseNewPowerSection(root)?.let(sections::add)
                else -> sections += parseFallbackDiscoverySections(root, rootType.orEmpty())
            }
        }
        return sections.distinctBy { "${it.title}|${it.type}" }
    }

    private fun parseDiscoveryCollection(root: JsonObject): List<DiscoverySection> {
        val modules = root.getAsJsonArray("data") ?: return emptyList()
        val sections = mutableListOf<DiscoverySection>()
        for (i in 0 until modules.size()) {
            val module = modules[i].asJsonObject
            val title = module.get("title")?.asString ?: continue
            val targets = module.getAsJsonArray("target") ?: continue
            val items = parseDiscoveryItems(targets)
            if (items.isNotEmpty()) {
                sections.add(
                    DiscoverySection(
                        title = title,
                        type = module.get("targetType")?.asString,
                        items = items
                    )
                )
            }
        }
        return sections
    }

    private fun parseFallbackDiscoverySections(root: JsonObject, rootType: String): List<DiscoverySection> {
        val sections = mutableListOf<DiscoverySection>()
        val rootTitle = firstString(root, "title", "name", "label")
        val dataValue = root.get("data")

        if (dataValue != null && dataValue.isJsonArray) {
            val dataArray = dataValue.asJsonArray
            sections += parseGenericModules(dataArray, rootTitle, rootType)
            val directItems = parseDiscoveryItems(dataArray)
            if (directItems.isNotEmpty() && shouldAddDirectDiscoverySection(sections, rootTitle, rootType, directItems)) {
                sections += DiscoverySection(
                    title = rootTitle ?: defaultSectionTitle(rootType),
                    type = rootType,
                    items = directItems
                )
            }
        }

        if (dataValue != null && dataValue.isJsonObject) {
            parseGenericModule(dataValue.asJsonObject, rootTitle, rootType)
                ?.takeIf { shouldAddDiscoverySection(sections, it) }
                ?.let(sections::add)
        }

        parseGenericModule(root, rootTitle, rootType)
            ?.takeIf { shouldAddDiscoverySection(sections, it) }
            ?.let(sections::add)

        return sections
            .filter { it.items.orEmpty().isNotEmpty() }
            .distinctBy { "${it.title}|${it.type}|${sectionItemSignature(it)}" }
    }

    private fun parseGenericModules(
        modules: JsonArray,
        parentTitle: String?,
        rootType: String
    ): List<DiscoverySection> {
        val sections = mutableListOf<DiscoverySection>()
        for (i in 0 until modules.size()) {
            val value = modules[i]
            if (!value.isJsonObject) continue
            parseGenericModule(value.asJsonObject, parentTitle, rootType)?.let(sections::add)
        }
        return sections
    }

    private fun parseGenericModule(
        module: JsonObject,
        parentTitle: String?,
        rootType: String
    ): DiscoverySection? {
        val title = firstString(module, "title", "name", "label", "description")
            ?: parentTitle
            ?: defaultSectionTitle(rootType)

        val arrayKeys = listOf("target", "items", "list", "podcasts", "episodes", "contents", "data")
        for (key in arrayKeys) {
            val value = module.get(key) ?: continue
            when {
                value.isJsonArray -> {
                    val items = parseDiscoveryItems(value.asJsonArray)
                    if (items.isNotEmpty()) {
                        return DiscoverySection(
                            title = title,
                            type = firstString(module, "targetType", "type", "moduleType") ?: rootType,
                            items = items
                        )
                    }
                }

                value.isJsonObject -> {
                    val nested = value.asJsonObject
                    for (nestedKey in arrayKeys) {
                        val nestedValue = nested.get(nestedKey) ?: continue
                        if (!nestedValue.isJsonArray) continue
                        val items = parseDiscoveryItems(nestedValue.asJsonArray)
                        if (items.isNotEmpty()) {
                            return DiscoverySection(
                                title = title,
                                type = firstString(module, "targetType", "type", "moduleType") ?: rootType,
                                items = items
                            )
                        }
                    }
                }
            }
        }

        return null
    }

    private fun parseNewPowerSection(root: JsonObject): DiscoverySection? {
        val data = root.getAsJsonObject("data") ?: return null
        val itemsArray = data.getAsJsonArray("items") ?: return null
        val items = parseDiscoveryItems(itemsArray)
        if (items.isEmpty()) return null

        val serialNumber = data.get("serialNumber")?.asString
        val title = if (serialNumber.isNullOrBlank()) "New Power" else "New Power $serialNumber"
        return DiscoverySection(title = title, type = "PODCAST", items = items)
    }

    private fun parseDiscoveryItems(source: JsonArray): List<DiscoveryItem> {
        val items = mutableListOf<DiscoveryItem>()
        for (i in 0 until source.size()) {
            val raw = source[i]
            if (!raw.isJsonObject) continue
            val obj = unwrapDiscoveryItemObject(raw.asJsonObject) ?: continue
            when {
                obj.has("podcast") -> {
                    val podcast = gson.fromJson<Podcast>(obj.getAsJsonObject("podcast"), podcastType)
                    items.add(DiscoveryItem(type = "PODCAST", podcast = podcast))
                }

                obj.has("episode") -> {
                    val episode = gson.fromJson<Episode>(obj.getAsJsonObject("episode"), episodeType)
                    items.add(DiscoveryItem(type = "EPISODE", podcast = episode.podcast, episode = episode))
                }

                obj.get("type")?.asString?.uppercase() == "PODCAST" -> {
                    val podcast = gson.fromJson<Podcast>(obj, podcastType)
                    items.add(DiscoveryItem(type = "PODCAST", podcast = podcast))
                }

                obj.get("type")?.asString?.uppercase() == "EPISODE" -> {
                    val episode = gson.fromJson<Episode>(obj, episodeType)
                    items.add(DiscoveryItem(type = "EPISODE", podcast = episode.podcast, episode = episode))
                }
            }
        }
        return items
    }

    private fun shouldAddDirectDiscoverySection(
        existing: List<DiscoverySection>,
        rootTitle: String?,
        rootType: String,
        directItems: List<DiscoveryItem>
    ): Boolean {
        if (existing.isEmpty()) return true
        val candidateTitle = rootTitle ?: defaultSectionTitle(rootType)
        if (candidateTitle == defaultSectionTitle(rootType) && existing.isNotEmpty()) return false
        val candidateSignature = discoveryItemSignature(directItems)
        return existing.none { sectionItemSignature(it) == candidateSignature }
    }

    private fun shouldAddDiscoverySection(
        existing: List<DiscoverySection>,
        candidate: DiscoverySection
    ): Boolean {
        val candidateSignature = sectionItemSignature(candidate)
        return existing.none {
            it.title == candidate.title ||
                sectionItemSignature(it) == candidateSignature
        }
    }

    private fun sectionItemSignature(section: DiscoverySection): String {
        return discoveryItemSignature(section.items.orEmpty())
    }

    private fun discoveryItemSignature(items: List<DiscoveryItem>): String {
        return items.joinToString("|") { item ->
            when {
                !item.podcast?.pid.isNullOrBlank() -> "p:${item.podcast?.pid}"
                !item.episode?.eid.isNullOrBlank() -> "e:${item.episode?.eid}"
                else -> item.type.orEmpty()
            }
        }
    }

    private fun unwrapDiscoveryItemObject(obj: JsonObject): JsonObject? {
        val candidateKeys = listOf("item", "target", "content", "data")
        for (key in candidateKeys) {
            val value = obj.get(key) ?: continue
            if (!value.isJsonObject) continue
            val nested = value.asJsonObject
            if (
                nested.has("podcast") ||
                nested.has("episode") ||
                nested.has("pid") ||
                nested.has("eid") ||
                nested.get("type")?.isJsonPrimitive == true
            ) {
                return nested
            }
        }
        return obj
    }

    private fun parseCategories(source: JsonArray): List<CategorySummary> {
        val categories = mutableListOf<CategorySummary>()
        for (i in 0 until source.size()) {
            val obj = source[i]
            if (!obj.isJsonObject) continue
            val item = obj.asJsonObject
            val id = firstString(item, "id", "categoryId", "value", "key").orEmpty()
            val name = firstString(item, "name", "title", "label").orEmpty()
            if (id.isNotBlank() && name.isNotBlank()) {
                categories += CategorySummary(
                    id = id,
                    name = name,
                    description = firstString(item, "description", "brief", "subtitle")
                )
            }
        }
        return categories
    }

    private fun parseCategoryTabs(source: JsonArray): List<CategoryTab> {
        val tabs = mutableListOf<CategoryTab>()
        for (i in 0 until source.size()) {
            val element = source[i]
            when {
                element.isJsonPrimitive -> {
                    val value = element.asString
                    if (value.isNotBlank()) {
                        tabs += CategoryTab(value = value, label = value)
                    }
                }

                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    val value = firstString(obj, "tab", "value", "code", "id", "name").orEmpty()
                    val label = firstString(obj, "name", "title", "label", "tab", "value").orEmpty()
                    if (value.isNotBlank()) {
                        tabs += CategoryTab(value = value, label = label.ifBlank { value })
                    }
                }
            }
        }
        return tabs.distinctBy { it.value }
    }

    private fun parsePodcasts(source: JsonArray): List<Podcast> {
        val podcasts = mutableListOf<Podcast>()
        for (i in 0 until source.size()) {
            val element = source[i]
            if (!element.isJsonObject) continue
            val obj = element.asJsonObject
            val podcastObject = when {
                obj.has("podcast") && obj.get("podcast").isJsonObject -> obj.getAsJsonObject("podcast")
                obj.get("type")?.asString?.uppercase() == "PODCAST" -> obj
                else -> null
            }
            if (podcastObject != null) {
                podcasts += gson.fromJson<Podcast>(podcastObject, podcastType)
            }
        }
        return podcasts.distinctBy { it.pid }
    }

    private fun parseEpisodes(source: JsonArray): List<Episode> {
        val episodes = mutableListOf<Episode>()
        for (i in 0 until source.size()) {
            val element = source[i]
            if (!element.isJsonObject) continue
            val obj = element.asJsonObject
            val episodeObject = when {
                obj.has("episode") && obj.get("episode").isJsonObject -> obj.getAsJsonObject("episode")
                obj.has("item") && obj.get("item").isJsonObject -> {
                    val item = obj.getAsJsonObject("item")
                    when {
                        item.has("episode") && item.get("episode").isJsonObject -> item.getAsJsonObject("episode")
                        item.get("type")?.asString?.uppercase() == "EPISODE" -> item
                        item.has("eid") -> item
                        else -> null
                    }
                }
                obj.get("type")?.asString?.uppercase() == "EPISODE" -> obj
                obj.has("eid") -> obj
                else -> null
            }
            if (episodeObject != null) {
                episodes += gson.fromJson<Episode>(episodeObject, episodeType)
            }
        }
        return episodes.distinctBy { it.eid }
    }

    private fun parseSearchPresets(source: JsonArray): List<SearchPreset> {
        val presets = mutableListOf<SearchPreset>()
        for (i in 0 until source.size()) {
            val element = source[i]
            when {
                element.isJsonPrimitive -> {
                    val text = element.asString
                    if (text.isNotBlank()) presets += SearchPreset(text = text)
                }

                element.isJsonObject -> {
                    val obj = element.asJsonObject
                    val text = firstString(obj, "title", "text", "keyword", "name", "query").orEmpty()
                    if (text.isNotBlank()) presets += SearchPreset(text = text)
                }
            }
        }
        return presets.distinctBy { it.text }
    }

    private fun parseUserPicks(source: JsonArray): List<UserPick> {
        val picks = mutableListOf<UserPick>()
        for (i in 0 until source.size()) {
            val element = source[i]
            if (!element.isJsonObject) continue
            val obj = element.asJsonObject
            val episode = obj.getAsJsonObject("episode")?.let { gson.fromJson<Episode>(it, episodeType) }
            picks += UserPick(
                id = firstString(obj, "id").orEmpty(),
                storyText = obj.getAsJsonObject("story")?.let { firstString(it, "text", "content") },
                pickedAt = firstString(obj, "pickedAt", "createdAt"),
                episode = episode,
                likeCount = firstInt(obj, "likeCount") ?: 0,
                isLiked = obj.get("isLiked")?.asBoolean ?: false
            )
        }
        return picks.distinctBy { it.id.ifBlank { it.episode?.eid.orEmpty() } }
    }

    private fun parseEditorPickDays(source: JsonArray): List<EditorPickDay> {
        val days = mutableListOf<EditorPickDay>()
        for (i in 0 until source.size()) {
            val element = source[i]
            if (!element.isJsonObject) continue
            val obj = element.asJsonObject
            val picksArray = obj.getAsJsonArray("picks") ?: continue
            val entries = mutableListOf<EditorPickEntry>()
            for (j in 0 until picksArray.size()) {
                val pickElement = picksArray[j]
                if (!pickElement.isJsonObject) continue
                val pick = pickElement.asJsonObject
                val episode = pick.getAsJsonObject("episode")?.let { gson.fromJson<Episode>(it, episodeType) }
                if (episode != null) {
                    entries += EditorPickEntry(
                        episode = episode,
                        commentText = pick.getAsJsonObject("comment")?.let { firstString(it, "text", "content") }
                    )
                }
            }
            if (entries.isNotEmpty()) {
                days += EditorPickDay(
                    date = firstString(obj, "date", "dateIsoStr").orEmpty(),
                    entries = entries
                )
            }
        }
        return days
    }

    private fun parseUsers(source: JsonArray): List<UserLite> {
        val users = mutableListOf<UserLite>()
        for (i in 0 until source.size()) {
            val element = source[i]
            if (!element.isJsonObject) continue
            users += parseUserLite(element.asJsonObject)
        }
        return users.distinctBy { it.uid }
    }

    private fun parseStickers(source: JsonArray): List<StickerItem> {
        val stickers = mutableListOf<StickerItem>()
        for (i in 0 until source.size()) {
            val element = source[i]
            if (!element.isJsonObject) continue
            stickers += parseSticker(element.asJsonObject)
        }
        return stickers.distinctBy { it.id }
    }

    private fun parseStickerBoardItems(source: JsonArray): List<StickerBoardItem> {
        val items = mutableListOf<StickerBoardItem>()
        for (i in 0 until source.size()) {
            val element = source[i]
            if (!element.isJsonObject) continue
            val obj = element.asJsonObject
            val stickerObj = obj.getAsJsonObject("sticker") ?: continue
            items += StickerBoardItem(
                sticker = parseSticker(stickerObj),
                x = firstInt(obj, "x") ?: 0,
                y = firstInt(obj, "y") ?: 0,
                rotation = runCatching { obj.get("rotation")?.asFloat ?: 0f }.getOrDefault(0f)
            )
        }
        return items
    }

    private fun parseSticker(obj: JsonObject): StickerItem {
        val image = obj.getAsJsonObject("image")
        return StickerItem(
            id = firstString(obj, "id").orEmpty(),
            name = firstString(obj, "name", "title").orEmpty(),
            description = firstString(obj, "description"),
            issuer = firstString(obj, "issuer"),
            imageUrl = image?.let { firstString(it, "picUrl", "largePicUrl", "middlePicUrl", "smallPicUrl") },
            ownedAt = firstString(obj, "ownedAt")
        )
    }

    private fun parseMileageOverview(obj: JsonObject): MileageOverview {
        return MileageOverview(
            totalPlayedSeconds = firstLong(obj, "totalPlayedSeconds") ?: 0,
            lastSevenDayPlayedSeconds = firstLong(obj, "lastSevenDayPlayedSeconds") ?: 0,
            lastThirtyDayPlayedSeconds = firstLong(obj, "lastThirtyDayPlayedSeconds") ?: 0,
            tagline = firstString(obj, "tagline")
        )
    }

    private fun parseMileageEntries(source: JsonArray): List<MileageEntry> {
        val entries = mutableListOf<MileageEntry>()
        for (i in 0 until source.size()) {
            val element = source[i]
            if (!element.isJsonObject) continue
            val obj = element.asJsonObject
            val podcast = obj.getAsJsonObject("podcast")?.let { gson.fromJson<Podcast>(it, podcastType) }
            if (podcast != null) {
                entries += MileageEntry(
                    playedSeconds = firstLong(obj, "playedSeconds") ?: 0,
                    podcast = podcast
                )
            }
        }
        return entries
    }

    private fun parseClapSummary(obj: JsonObject): ClapSummary {
        val clapArray = obj.getAsJsonArray("episodeClaps")
        val myClapsArray = obj.getAsJsonArray("myClaps")
        val buckets = mutableListOf<ClapBucket>()
        if (clapArray != null) {
            for (i in 0 until clapArray.size()) {
                val item = clapArray[i]
                if (!item.isJsonObject) continue
                buckets += ClapBucket(count = firstInt(item.asJsonObject, "count") ?: 0)
            }
        }
        val myClaps = mutableListOf<Int>()
        if (myClapsArray != null) {
            for (i in 0 until myClapsArray.size()) {
                val item = myClapsArray[i]
                if (!item.isJsonObject) continue
                firstInt(item.asJsonObject, "index")?.let(myClaps::add)
            }
        }
        return ClapSummary(episodeClaps = buckets, myClaps = myClaps)
    }

    private fun parseTranscriptSentences(source: JsonArray): List<TranscriptSentence> {
        val sentences = mutableListOf<TranscriptSentence>()
        for (i in 0 until source.size()) {
            val element = source[i]
            if (!element.isJsonObject) continue
            val obj = element.asJsonObject
            val text = firstString(obj, "text", "sentence").orEmpty()
            if (text.isBlank()) continue
            sentences += TranscriptSentence(
                index = firstInt(obj, "index") ?: i,
                text = text,
                startMs = firstLong(obj, "startMs", "start", "fromMs", "from"),
                endMs = firstLong(obj, "endMs", "end", "toMs", "to")
            )
        }
        return sentences.sortedBy { it.index }
    }

    private fun findTranscriptSentencesArray(obj: JsonObject): JsonArray? {
        obj.getAsJsonArray("sentences")?.let { return it }
        val data = obj.get("data")
        if (data?.isJsonObject == true) {
            data.asJsonObject.getAsJsonArray("sentences")?.let { return it }
            data.asJsonObject.get("transcript")?.takeIf { it.isJsonObject }
                ?.asJsonObject
                ?.getAsJsonArray("sentences")
                ?.let { return it }
        }
        obj.get("transcript")?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?.getAsJsonArray("sentences")
            ?.let { return it }
        return null
    }

    private fun parsePlaybackProgress(obj: JsonObject): PlaybackProgressInfo {
        return PlaybackProgressInfo(
            eid = firstString(obj, "eid").orEmpty(),
            pid = firstString(obj, "pid").orEmpty(),
            progress = firstInt(obj, "progress") ?: 0,
            playedAt = firstString(obj, "playedAt")
        )
    }

    private fun parseComments(source: JsonArray): List<CommentItem> {
        val comments = mutableListOf<CommentItem>()
        for (i in 0 until source.size()) {
            val element = source[i]
            if (!element.isJsonObject) continue
            comments += parseComment(element.asJsonObject)
        }
        return comments.distinctBy { it.id }
    }

    private fun parseComment(obj: JsonObject): CommentItem {
        val replies = obj.getAsJsonArray("replies")?.let { parseComments(it) }.orEmpty()
        val episode = obj.getAsJsonObject("episode")?.let { gson.fromJson<Episode>(it, episodeType) }
        return CommentItem(
            id = firstString(obj, "id").orEmpty(),
            ownerId = obj.getAsJsonObject("owner")?.let { firstString(it, "id") },
            text = firstString(obj, "text", "content").orEmpty(),
            author = obj.getAsJsonObject("author")?.let(::parseUserLite),
            createdAt = firstString(obj, "createdAt", "collectedAt"),
            likeCount = firstInt(obj, "likeCount") ?: 0,
            liked = obj.get("liked")?.asBoolean ?: false,
            collected = obj.get("collected")?.asBoolean ?: false,
            pinned = obj.get("pinned")?.asBoolean ?: false,
            threadReplyCount = firstInt(obj, "threadReplyCount", "replyCount") ?: replies.size,
            episode = episode,
            replies = replies,
            replyToAuthorName = obj.getAsJsonObject("replyToComment")
                ?.getAsJsonObject("author")
                ?.let { firstString(it, "nickname", "name") }
        )
    }

    private fun parseUserLite(obj: JsonObject): UserLite {
        val avatarUrl = when {
            obj.get("avatar")?.isJsonObject == true -> {
                val avatar = obj.getAsJsonObject("avatar")
                when {
                    avatar.get("picture")?.isJsonObject == true ->
                        firstString(avatar.getAsJsonObject("picture"), "picUrl", "largePicUrl", "middlePicUrl", "smallPicUrl")
                    else -> firstString(avatar, "picUrl", "largePicUrl", "middlePicUrl", "smallPicUrl")
                }
            }

            else -> firstString(obj, "avatarUrl")
        }

        return UserLite(
            uid = firstString(obj, "uid", "id").orEmpty(),
            nickname = firstString(obj, "nickname", "name").orEmpty(),
            bio = firstString(obj, "bio", "description"),
            avatarUrl = avatarUrl,
            relation = firstString(obj, "relation"),
            ipLoc = firstString(obj, "ipLoc"),
            isBlockedByViewer = obj.get("isBlockedByViewer")?.asBoolean ?: false
        )
    }

    private fun parseUserProfile(obj: JsonObject): UserProfile {
        val avatarUrl = when {
            obj.get("avatar")?.isJsonObject == true -> {
                val avatar = obj.getAsJsonObject("avatar")
                when {
                    avatar.get("picture")?.isJsonObject == true ->
                        firstString(avatar.getAsJsonObject("picture"), "picUrl", "largePicUrl", "middlePicUrl", "smallPicUrl")
                    else -> firstString(avatar, "picUrl", "largePicUrl", "middlePicUrl", "smallPicUrl")
                }
            }

            else -> firstString(obj, "avatarUrl")
        }

        return UserProfile(
            uid = firstString(obj, "uid", "id").orEmpty(),
            nickname = firstString(obj, "nickname", "name").orEmpty(),
            bio = firstString(obj, "bio", "description"),
            avatarUrl = avatarUrl
        )
    }

    private fun parseUserPreference(obj: JsonObject): UserPreference {
        return UserPreference(
            isRecentPlayedHidden = obj.get("isRecentPlayedHidden")?.asBoolean ?: false,
            isListenMileageHiddenInComment = obj.get("isListenMileageHiddenInComment")?.asBoolean ?: false,
            isStickerLibraryHidden = obj.get("isStickerLibraryHidden")?.asBoolean ?: false,
            isStickerBoardHidden = obj.get("isStickerBoardHidden")?.asBoolean ?: false,
            rejectHotPush = obj.get("rejectHotPush")?.asBoolean ?: false,
            rejectRecommendation = obj.get("rejectRecommendation")?.asBoolean ?: false
        )
    }

    private fun parseUserStats(obj: JsonObject): UserStats {
        return UserStats(
            followingCount = firstInt(obj, "followingCount", "following") ?: 0,
            followerCount = firstInt(obj, "followerCount", "follower") ?: 0,
            subscriptionCount = firstInt(obj, "subscriptionCount", "subscriptionsCount") ?: 0,
            totalPlayedSeconds = (firstLong(obj, "totalPlayedSeconds", "listenSeconds", "playedSeconds", "totalListenSeconds")
                ?: firstLong(obj, "listenDuration")
                ?: 0L)
        )
    }

    private fun parsePodcastBulletin(obj: JsonObject): PodcastBulletin? {
        val title = firstString(obj, "title", "name")
        val content = firstString(obj, "content", "description", "text")
        if (title.isNullOrBlank() && content.isNullOrBlank()) return null
        return PodcastBulletin(title = title, content = content)
    }

    private fun parsePodcastOwnerInfo(obj: JsonObject): PodcastOwnerInfo {
        return PodcastOwnerInfo(
            subject = firstString(obj, "subject"),
            ipLoc = firstString(obj, "ipLoc")
        )
    }

    private fun parsePodcastHonors(source: JsonArray): List<PodcastHonor> {
        val honors = mutableListOf<PodcastHonor>()
        for (i in 0 until source.size()) {
            val element = source[i]
            if (!element.isJsonObject) continue
            val obj = element.asJsonObject
            honors += PodcastHonor(
                id = firstString(obj, "id").orEmpty(),
                campaignTitle = firstString(obj, "campaignTitle"),
                title = firstString(obj, "title", "name").orEmpty(),
                url = firstString(obj, "url")
            )
        }
        return honors.distinctBy { it.id.ifBlank { it.title } }
    }

    private fun firstString(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            val value = obj.get(key) ?: continue
            if (value.isJsonPrimitive) {
                val text = value.asString
                if (text.isNotBlank()) return text
            }
        }
        return null
    }

    private fun firstInt(obj: JsonObject, vararg keys: String): Int? {
        for (key in keys) {
            val value = obj.get(key) ?: continue
            if (value.isJsonPrimitive) {
                runCatching { return value.asInt }
            }
        }
        return null
    }

    private fun firstLong(obj: JsonObject, vararg keys: String): Long? {
        for (key in keys) {
            val value = obj.get(key) ?: continue
            if (value.isJsonPrimitive) {
                runCatching { return value.asLong }
            }
        }
        return null
    }

    private fun sortHomeSections(sections: List<DiscoverySection>): List<DiscoverySection> {
        return sections
            .mapIndexed { index, section -> index to section }
            .sortedWith(
                compareBy<Pair<Int, DiscoverySection>> { sectionPriority(it.second.title) }
                    .thenBy { it.first }
            )
            .map { it.second }
    }

    private fun sectionPriority(title: String?): Int {
        val text = title.orEmpty()
        return when {
            text.contains("编辑精选") -> 0
            text.contains("为你精选") -> 1
            text.contains("最热榜") -> 2
            text.contains("锋芒榜") -> 3
            text.contains("新星榜") -> 4
            text.contains("榜") -> 5
            text.contains("精选") -> 6
            text.contains("大家都在听") -> 7
            text.contains("新节目") -> 8
            else -> 20
        }
    }

    private fun defaultSectionTitle(rootType: String): String {
        return when (rootType.uppercase()) {
            "NEW_POWER" -> "新节目广场"
            else -> "推荐内容"
        }
    }

    private fun httpError(endpoint: String, statusCode: Int, responseBody: String?): Exception {
        if (statusCode == 401) {
            if (TokenManager.isLoggedIn()) {
                TokenManager.clear()
                AppLogger.info("repo", "Cleared expired auth token after HTTP 401 on $endpoint")
            }
            return Exception("HTTP 401：登录状态已失效，请重新登录")
        }
        val snippet = responseBody
            ?.replace(Regex("<[^>]+>"), " ")
            ?.replace(Regex("\\\\u003c[^>]+?\\\\u003e"), " ")
            ?.replace(Regex("\\\\[rn]"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.take(160)
        val suffix = snippet?.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()
        return Exception("HTTP $statusCode on $endpoint$suffix")
    }
}
