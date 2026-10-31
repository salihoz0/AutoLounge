package com.autohub.park.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autohub.park.ui.CarVisualTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.carConfigDataStore by preferencesDataStore(name = "autolounge_car_config")

data class CarConfig(
    val theme: CarVisualTheme = CarVisualTheme.ONYX_EMERALD,
    val themeMode: String = "dark", // Kullanıcı isteği: Varsayılanda saf siyah ekran
    val showYoutube: Boolean = true,
    val showBrowser: Boolean = true,
    val showGemini: Boolean = true,
    val showQuickSearch: Boolean = true,
    val showIptv: Boolean = true,
    val defaultUrl: String = "https://www.google.com",
    val gridColumns: Int = 4, // CarPlay / iOS / Android launcher standardı
    val cardOrder: List<String> = emptyList()
)

@Singleton
class CarConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("car_theme")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_SHOW_YOUTUBE = booleanPreferencesKey("show_youtube")
        private val KEY_SHOW_BROWSER = booleanPreferencesKey("show_browser")
        private val KEY_SHOW_GEMINI = booleanPreferencesKey("show_gemini")
        private val KEY_SHOW_QUICK_SEARCH = booleanPreferencesKey("show_quick_search")
        private val KEY_SHOW_IPTV = booleanPreferencesKey("show_iptv")
        private val KEY_DEFAULT_URL = stringPreferencesKey("default_url")
        private val KEY_GRID_COLUMNS = intPreferencesKey("grid_columns")
        private val KEY_CARD_ORDER = stringPreferencesKey("card_order")
    }

    val configFlow: Flow<CarConfig> = context.carConfigDataStore.data.map { prefs ->
        val orderRaw = prefs[KEY_CARD_ORDER] ?: ""
        val parsedOrder = if (orderRaw.isNotBlank()) orderRaw.split(",").map { it.trim() } else emptyList()
        CarConfig(
            theme = CarVisualTheme.fromId(prefs[KEY_THEME] ?: CarVisualTheme.ONYX_EMERALD.id),
            themeMode = prefs[KEY_THEME_MODE] ?: "dark",
            showYoutube = prefs[KEY_SHOW_YOUTUBE] ?: true,
            showBrowser = prefs[KEY_SHOW_BROWSER] ?: true,
            showGemini = prefs[KEY_SHOW_GEMINI] ?: true,
            showQuickSearch = prefs[KEY_SHOW_QUICK_SEARCH] ?: true,
            showIptv = prefs[KEY_SHOW_IPTV] ?: true,
            defaultUrl = prefs[KEY_DEFAULT_URL] ?: "https://www.google.com",
            gridColumns = prefs[KEY_GRID_COLUMNS] ?: 4,
            cardOrder = parsedOrder
        )
    }

    suspend fun setCardOrder(order: List<String>) {
        context.carConfigDataStore.edit { prefs ->
            prefs[KEY_CARD_ORDER] = order.joinToString(",")
        }
    }


    suspend fun setTheme(theme: CarVisualTheme) {
        context.carConfigDataStore.edit { it[KEY_THEME] = theme.id }
    }

    suspend fun setThemeMode(mode: String) {
        context.carConfigDataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun toggleCard(cardKey: String, visible: Boolean) {
        context.carConfigDataStore.edit { prefs ->
            when (cardKey) {
                "youtube" -> prefs[KEY_SHOW_YOUTUBE] = visible
                "browser" -> prefs[KEY_SHOW_BROWSER] = visible
                "gemini" -> prefs[KEY_SHOW_GEMINI] = visible
                "quick_search" -> prefs[KEY_SHOW_QUICK_SEARCH] = visible
                "iptv" -> prefs[KEY_SHOW_IPTV] = visible
            }
        }
    }

    suspend fun setDefaultUrl(url: String) {
        context.carConfigDataStore.edit { it[KEY_DEFAULT_URL] = url }
    }

    suspend fun setGridColumns(cols: Int) {
        context.carConfigDataStore.edit { it[KEY_GRID_COLUMNS] = cols.coerceIn(2, 4) }
    }

    suspend fun resetToDefaults() {
        context.carConfigDataStore.edit { prefs ->
            prefs[KEY_THEME] = CarVisualTheme.ONYX_EMERALD.id
            prefs[KEY_THEME_MODE] = "dark"
            prefs[KEY_SHOW_YOUTUBE] = true
            prefs[KEY_SHOW_BROWSER] = true
            prefs[KEY_SHOW_GEMINI] = true
            prefs[KEY_SHOW_QUICK_SEARCH] = true
            prefs[KEY_SHOW_IPTV] = true
            prefs[KEY_DEFAULT_URL] = "https://www.google.com"
            prefs[KEY_GRID_COLUMNS] = 4
            prefs.remove(KEY_CARD_ORDER)
        }
    }

}


