package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tender_prefs")

enum class AppThemeStyle(val arabicName: String) {
  LIQUID_ROYAL("الأزرق الملكي الزجاجي"),
  DEEP_NAVY("الكحلي المؤسسي"),
  OCEAN_TEAL("الأزرق الفيروزي"),
  EMERALD("الأخضر الزمردي"),
  INDIGO_SKY("النيلي السماوي"),
  VIOLET("البنفسجي الملكي"),
  ROSE("الوردي الياقوتي"),
  SUNSET("البرتقالي الغروب"),
  GOLD("الذهبي الفاخر"),
  GRAPHITE("الرمادي الفحمي");

  companion object {
    fun fromString(value: String?): AppThemeStyle =
      entries.firstOrNull { it.name == value } ?: LIQUID_ROYAL
  }
}

enum class ThemeMode(val arabicName: String) {
  SYSTEM("حسب النظام"),
  LIGHT("فاتح"),
  DARK("داكن");

  companion object {
    fun fromString(value: String?): ThemeMode? = entries.firstOrNull { it.name == value }
  }
}

enum class AppLanguage(val code: String, val nativeName: String) {
  AR("ar", "العربية"),
  EN("en", "English");

  companion object {
    fun fromString(value: String?): AppLanguage =
      entries.firstOrNull { it.name == value } ?: AR
  }
}

enum class FontSizeOption(val scale: Float, val arabicName: String) {
  SMALL(0.9f, "صغير"),
  NORMAL(1f, "متوسط"),
  LARGE(1.12f, "كبير"),
  XLARGE(1.25f, "كبير جداً");

  companion object {
    fun fromString(value: String?): FontSizeOption =
      entries.firstOrNull { it.name == value } ?: NORMAL
  }
}

data class AppearancePrefs(
  val themeMode: ThemeMode = ThemeMode.LIGHT,
  val themeStyle: AppThemeStyle = AppThemeStyle.LIQUID_ROYAL,
  val amoled: Boolean = false,
  val language: AppLanguage = AppLanguage.AR,
  val fontSize: FontSizeOption = FontSizeOption.NORMAL,
)

class AppPreferences(private val context: Context) {
  private object Keys {
    val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val AMOLED = booleanPreferencesKey("amoled_black")
    val LANGUAGE = stringPreferencesKey("app_language")
    val FONT_SIZE = stringPreferencesKey("font_size")
    val THEME_STYLE = stringPreferencesKey("theme_style")
    val REMEMBER_USERNAME = booleanPreferencesKey("remember_username")
    val SAVED_USERNAME = stringPreferencesKey("saved_username")
    val LOGGED_IN_USER_ID = longPreferencesKey("logged_in_user_id")
  }

  val appearance: Flow<AppearancePrefs> =
    context.dataStore.data
      .map { prefs ->
        AppearancePrefs(
          themeMode =
            ThemeMode.fromString(prefs[Keys.THEME_MODE])
              ?: if (prefs[Keys.IS_DARK_MODE] == true) ThemeMode.DARK else ThemeMode.LIGHT,
          themeStyle = AppThemeStyle.fromString(prefs[Keys.THEME_STYLE]),
          amoled = prefs[Keys.AMOLED] ?: false,
          language = AppLanguage.fromString(prefs[Keys.LANGUAGE]),
          fontSize = FontSizeOption.fromString(prefs[Keys.FONT_SIZE]),
        )
      }
      .distinctUntilChanged()

  val isDarkMode: Flow<Boolean> =
    context.dataStore.data.map { prefs -> prefs[Keys.IS_DARK_MODE] ?: false }

  val themeStyle: Flow<AppThemeStyle> =
    context.dataStore.data.map { prefs -> AppThemeStyle.fromString(prefs[Keys.THEME_STYLE]) }

  val rememberUsername: Flow<Boolean> =
    context.dataStore.data.map { prefs -> prefs[Keys.REMEMBER_USERNAME] ?: true }

  val savedUsername: Flow<String> =
    context.dataStore.data.map { prefs -> prefs[Keys.SAVED_USERNAME] ?: "" }

  val loggedInUserId: Flow<Long?> =
    context.dataStore.data.map { prefs ->
      val id = prefs[Keys.LOGGED_IN_USER_ID] ?: -1L
      if (id > 0L) id else null
    }

  suspend fun setDarkMode(enabled: Boolean) {
    context.dataStore.edit { it[Keys.IS_DARK_MODE] = enabled }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun setAmoled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.AMOLED] = enabled }
  }

  suspend fun setLanguage(language: AppLanguage) {
    context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
  }

  suspend fun setFontSize(option: FontSizeOption) {
    context.dataStore.edit { it[Keys.FONT_SIZE] = option.name }
  }

  suspend fun setThemeStyle(style: AppThemeStyle) {
    context.dataStore.edit { it[Keys.THEME_STYLE] = style.name }
  }

  suspend fun saveLoginPreference(remember: Boolean, username: String) {
    context.dataStore.edit { prefs ->
      prefs[Keys.REMEMBER_USERNAME] = remember
      prefs[Keys.SAVED_USERNAME] = if (remember) username else ""
    }
  }

  suspend fun setLoggedInUserId(userId: Long?) {
    context.dataStore.edit { prefs ->
      if (userId != null && userId > 0L) {
        prefs[Keys.LOGGED_IN_USER_ID] = userId
      } else {
        prefs.remove(Keys.LOGGED_IN_USER_ID)
      }
    }
  }
}
