package com.example.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
  private val arabicMonths =
    arrayOf(
      "يناير",
      "فبراير",
      "مارس",
      "أبريل",
      "مايو",
      "يونيو",
      "يوليو",
      "أغسطس",
      "سبتمبر",
      "أكتوبر",
      "نوفمبر",
      "ديسمبر",
    )

  private val arabicWeekdays =
    mapOf(
      Calendar.SATURDAY to "السبت",
      Calendar.SUNDAY to "الأحد",
      Calendar.MONDAY to "الإثنين",
      Calendar.TUESDAY to "الثلاثاء",
      Calendar.WEDNESDAY to "الأربعاء",
      Calendar.THURSDAY to "الخميس",
      Calendar.FRIDAY to "الجمعة",
    )

  fun formatFullGregorianHeader(millis: Long = System.currentTimeMillis()): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    val weekday = arabicWeekdays[cal.get(Calendar.DAY_OF_WEEK)] ?: ""
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = arabicMonths[cal.get(Calendar.MONTH)]
    val year = cal.get(Calendar.YEAR)
    return "$weekday، $day $month $year"
  }

  fun formatNumericDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
    return sdf.format(Date(millis))
  }

  fun formatDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.US)
    return sdf.format(Date(millis))
  }

  fun formatMonthShort(monthIndexZeroBased: Int): String {
    return arabicMonths.getOrElse(monthIndexZeroBased.coerceIn(0, 11)) { "" }
  }

  fun startOfDayMillis(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = millis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  fun endOfDayMillis(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = millis
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 59)
      set(Calendar.SECOND, 59)
      set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
  }
}
