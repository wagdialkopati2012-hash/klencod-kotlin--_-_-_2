package com.example.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.data.prefs.AppLanguage
import java.util.concurrent.ConcurrentHashMap

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.AR }

/**
 * Lightweight runtime translator. The app's UI texts are written in Arabic; when the user picks
 * English, every text shown through [com.example.ui.i18n.Text] (and [Translator.t]) is looked up in
 * [EnglishStrings]. Unknown texts (user data such as customer or item names) are left untouched.
 */
object Translator {
  @Volatile var language: AppLanguage = AppLanguage.AR

  private val cache = ConcurrentHashMap<String, String>()
  private val arabicChar = Regex("[\\u0600-\\u06FF]")

  private val dateRule =
    Regex(
      "^(السبت|الأحد|الإثنين|الثلاثاء|الأربعاء|الخميس|الجمعة)، (\\d+) " +
        "(يناير|فبراير|مارس|أبريل|مايو|يونيو|يوليو|أغسطس|سبتمبر|أكتوبر|نوفمبر|ديسمبر) (\\d+)$"
    )

  private val templateRules: List<Pair<Regex, String>> by lazy {
    EnglishStrings.templates.map { (ar, en) ->
      val parts = ar.split("{}")
      val sb = StringBuilder("^")
      parts.forEachIndexed { index, part ->
        sb.append(Regex.escape(part))
        if (index < parts.lastIndex) {
          // A placeholder at the very start of the text is always a number in our templates.
          sb.append(if (index == 0 && part.isEmpty()) "(\\d+)" else "(.+?)")
        }
      }
      sb.append("$")
      Regex(sb.toString(), RegexOption.DOT_MATCHES_ALL) to en
    }
  }

  fun t(text: String, lang: AppLanguage = language): String {
    if (lang == AppLanguage.AR || text.isEmpty()) return text
    cache[text]?.let { return it }
    val result = translate(text)
    if (cache.size < 4000) cache[text] = result
    return result
  }

  private fun translate(text: String): String {
    if (!arabicChar.containsMatchIn(text)) return text
    EnglishStrings.exact[text]?.let { return it }
    val trimmed = text.trim()
    if (trimmed != text) {
      EnglishStrings.exact[trimmed]?.let { return it }
    }

    dateRule.matchEntire(trimmed)?.let { m ->
      val (weekday, day, month, year) = m.destructured
      return "${translate(weekday)}, ${translate(month)} $day, $year"
    }

    for ((regex, en) in templateRules) {
      val m = regex.matchEntire(trimmed) ?: continue
      var out = en
      m.groupValues.drop(1).forEachIndexed { i, value ->
        out = out.replace("{$i}", translate(value))
      }
      return out
    }
    return text
  }
}
