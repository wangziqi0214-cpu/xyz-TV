package com.ultrazg.xyztv.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

enum class HomePlacement {
    TOP,
    SHELF
}

enum class HomeFeature(
    val key: String,
    val title: String,
    val summary: String
) {
    CATEGORIES(
        key = "categories",
        title = "分类探索",
        summary = "把分类探索放在首页顶部入口，或者保留成横向浏览栏目。"
    ),
    PILOT_DISCOVERY(
        key = "pilot_discovery",
        title = "新节目广场",
        summary = "展示新节目广场，默认用横滑栏目来浏览。"
    ),
    EDITOR_PICKS(
        key = "editor_picks",
        title = "编辑精选",
        summary = "横滑模式下只展示当天的编辑精选。"
    ),
    TOP_LISTS(
        key = "top_lists",
        title = "首页榜单",
        summary = "显示最热榜、飙升榜和新星榜。"
    )
}

object HomeLayoutManager {
    private const val PREF_NAME = "xyz_tv_home_layout"
    private const val KEY_PLACEMENT_PREFIX = "feature_placement_"
    private const val KEY_ORDER = "feature_order"

    private lateinit var prefs: SharedPreferences

    var version by mutableIntStateOf(0)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun placementOf(feature: HomeFeature): HomePlacement {
        val saved = prefs.getString(KEY_PLACEMENT_PREFIX + feature.key, null)
        return runCatching { HomePlacement.valueOf(saved.orEmpty()) }
            .getOrDefault(defaultPlacement(feature))
    }

    fun setPlacement(feature: HomeFeature, placement: HomePlacement) {
        prefs.edit().putString(KEY_PLACEMENT_PREFIX + feature.key, placement.name).apply()
        bumpVersion()
        AppLogger.info("home_layout", "setPlacement feature=${feature.key} placement=${placement.name}")
    }

    fun orderedFeatures(): List<HomeFeature> {
        val savedOrder = prefs.getString(KEY_ORDER, null)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

        val savedSet = savedOrder.toSet()
        val knownByKey = HomeFeature.entries.associateBy { it.key }
        val ordered = savedOrder.mapNotNull(knownByKey::get).toMutableList()
        HomeFeature.entries.filterNot { it.key in savedSet }.forEach(ordered::add)
        return ordered
    }

    fun moveEarlier(feature: HomeFeature) {
        val current = orderedFeatures().toMutableList()
        val index = current.indexOf(feature)
        if (index <= 0) return
        current[index] = current[index - 1].also { current[index - 1] = current[index] }
        saveOrder(current)
        AppLogger.info("home_layout", "moveEarlier feature=${feature.key}")
    }

    fun moveLater(feature: HomeFeature) {
        val current = orderedFeatures().toMutableList()
        val index = current.indexOf(feature)
        if (index == -1 || index >= current.lastIndex) return
        current[index] = current[index + 1].also { current[index + 1] = current[index] }
        saveOrder(current)
        AppLogger.info("home_layout", "moveLater feature=${feature.key}")
    }

    fun canMoveEarlier(feature: HomeFeature): Boolean = orderedFeatures().indexOf(feature) > 0

    fun canMoveLater(feature: HomeFeature): Boolean {
        val order = orderedFeatures()
        val index = order.indexOf(feature)
        return index in 0 until order.lastIndex
    }

    fun orderLabel(feature: HomeFeature): Int = orderedFeatures().indexOf(feature) + 1

    private fun saveOrder(features: List<HomeFeature>) {
        prefs.edit().putString(KEY_ORDER, features.joinToString(",") { it.key }).apply()
        bumpVersion()
    }

    private fun bumpVersion() {
        version += 1
    }

    private fun defaultPlacement(feature: HomeFeature): HomePlacement {
        return when (feature) {
            HomeFeature.CATEGORIES -> HomePlacement.SHELF
            HomeFeature.PILOT_DISCOVERY -> HomePlacement.SHELF
            HomeFeature.EDITOR_PICKS -> HomePlacement.SHELF
            HomeFeature.TOP_LISTS -> HomePlacement.TOP
        }
    }
}
