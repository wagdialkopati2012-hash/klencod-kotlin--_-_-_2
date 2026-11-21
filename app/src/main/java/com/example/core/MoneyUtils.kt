package com.example.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyUtils {
  private val usdFormatter =
    DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US)).apply {
      minimumFractionDigits = 0
      maximumFractionDigits = 2
    }

  private val usdFixedFormatter =
    DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

  fun normalizeDigits(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
      when (ch) {
        in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
        in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
        '٫', '،' -> sb.append('.')
        else -> sb.append(ch)
      }
    }
    return sb.toString()
  }

  fun parseUsdOrNull(raw: String): BigDecimal? {
    val cleaned = normalizeDigits(raw)
      .replace("$", "")
      .replace(",", "")
      .replace("USD", "", ignoreCase = true)
      .trim()
    if (cleaned.isEmpty()) return null
    return cleaned.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)
  }

  fun parseQuantityOrNull(raw: String): Int? {
    val cleaned = normalizeDigits(raw).trim()
    return cleaned.toIntOrNull()
  }

  fun calculateLineTotal(unitPrice: BigDecimal, quantity: Int): BigDecimal {
    if (quantity <= 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
    return unitPrice.multiply(BigDecimal(quantity)).setScale(2, RoundingMode.HALF_UP)
  }

  fun calculateQuotationTotal(lineTotals: List<BigDecimal>): BigDecimal {
    return lineTotals
      .fold(BigDecimal.ZERO) { acc, value -> acc.add(value) }
      .setScale(2, RoundingMode.HALF_UP)
  }

  fun formatUsd(amount: BigDecimal, alwaysShowCents: Boolean = false): String {
    val scaled = amount.setScale(2, RoundingMode.HALF_UP)
    val hasCents = scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0
    val formatted =
      if (alwaysShowCents || hasCents) {
        usdFixedFormatter.format(scaled)
      } else {
        usdFormatter.format(scaled)
      }
    return "$$formatted"
  }

  fun formatUsdString(rawAmount: String, alwaysShowCents: Boolean = false): String {
    val bd = rawAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    return formatUsd(bd, alwaysShowCents)
  }

  /** Amount in words in the current app language (used on screen; the PDF stays Arabic). */
  fun amountInWords(amount: BigDecimal): String =
    if (com.example.ui.i18n.Translator.language == com.example.data.prefs.AppLanguage.EN) {
      amountInEnglishWords(amount)
    } else {
      amountInArabicWords(amount)
    }

  private val enOnes =
    arrayOf(
      "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
      "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen",
      "nineteen",
    )
  private val enTens =
    arrayOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

  private fun englishBelowThousand(n: Int): String {
    val parts = mutableListOf<String>()
    val h = n / 100
    val rem = n % 100
    if (h > 0) parts.add("${enOnes[h]} hundred")
    if (rem > 0) {
      if (rem < 20) {
        parts.add(enOnes[rem])
      } else {
        val u = rem % 10
        parts.add(if (u > 0) "${enTens[rem / 10]}-${enOnes[u]}" else enTens[rem / 10])
      }
    }
    return parts.joinToString(" ")
  }

  private fun englishInteger(number: Long): String {
    if (number == 0L) return "zero"
    var n = number
    val parts = mutableListOf<String>()
    val billions = (n / 1_000_000_000L).toInt()
    n %= 1_000_000_000L
    val millions = (n / 1_000_000L).toInt()
    n %= 1_000_000L
    val thousands = (n / 1_000L).toInt()
    val remainder = (n % 1_000L).toInt()
    if (billions > 0) parts.add("${englishBelowThousand(billions)} billion")
    if (millions > 0) parts.add("${englishBelowThousand(millions)} million")
    if (thousands > 0) parts.add("${englishBelowThousand(thousands)} thousand")
    if (remainder > 0) parts.add(englishBelowThousand(remainder))
    return parts.joinToString(" ")
  }

  fun amountInEnglishWords(amount: BigDecimal): String {
    val scaled = amount.setScale(2, RoundingMode.HALF_UP).max(BigDecimal.ZERO)
    val integerPart = scaled.toLong()
    val centsPart = scaled.remainder(BigDecimal.ONE).movePointRight(2).toInt()
    val dollars =
      "${englishInteger(integerPart)} US ${if (integerPart == 1L) "dollar" else "dollars"}"
    return if (centsPart > 0) {
      "Only $dollars and ${englishInteger(centsPart.toLong())} ${if (centsPart == 1) "cent" else "cents"}"
    } else {
      "Only $dollars"
    }
  }

  fun amountInArabicWords(amount: BigDecimal): String {
    val scaled = amount.setScale(2, RoundingMode.HALF_UP).max(BigDecimal.ZERO)
    val integerPart = scaled.toLong()
    val centsPart = scaled.remainder(BigDecimal.ONE).movePointRight(2).toInt()

    val dollarsText =
      when (integerPart) {
        0L -> "صفر دولار أمريكي"
        1L -> "دولار أمريكي واحد"
        2L -> "دولاران أمريكيان"
        in 3L..10L -> "${convertIntegerToArabic(integerPart)} دولارات أمريكية"
        else -> "${convertIntegerToArabic(integerPart)} دولاراً أمريكياً"
      }

    return if (centsPart > 0) {
      val centsText =
        when (centsPart) {
          1 -> "سنت واحد"
          2 -> "سنتان"
          in 3..10 -> "${convertIntegerToArabic(centsPart.toLong())} سنتات"
          else -> "${convertIntegerToArabic(centsPart.toLong())} سنتاً"
        }
      "فقط $dollarsText و$centsText لا غير"
    } else {
      "فقط $dollarsText لا غير"
    }
  }

  private val ones =
    arrayOf(
      "",
      "واحد",
      "اثنان",
      "ثلاثة",
      "أربعة",
      "خمسة",
      "ستة",
      "سبعة",
      "ثمانية",
      "تسعة",
      "عشرة",
      "أحد عشر",
      "اثنا عشر",
      "ثلاثة عشر",
      "أربعة عشر",
      "خمسة عشر",
      "ستة عشر",
      "سبعة عشر",
      "ثمانية عشر",
      "تسعة عشر",
    )

  private val tens =
    arrayOf(
      "",
      "عشرة",
      "عشرون",
      "ثلاثون",
      "أربعون",
      "خمسون",
      "ستون",
      "سبعون",
      "ثمانون",
      "تسعون",
    )

  private val hundreds =
    arrayOf(
      "",
      "مائة",
      "مائتان",
      "ثلاثمائة",
      "أربعمائة",
      "خمسمائة",
      "ستمائة",
      "سبعمائة",
      "ثمانمائة",
      "تسعمائة",
    )

  private fun convertIntegerToArabic(number: Long): String {
    if (number == 0L) return "صفر"
    val parts = mutableListOf<String>()
    var n = number

    val billions = (n / 1_000_000_000L).toInt()
    n %= 1_000_000_000L
    val millions = (n / 1_000_000L).toInt()
    n %= 1_000_000L
    val thousands = (n / 1_000L).toInt()
    val remainder = (n % 1_000L).toInt()

    if (billions > 0) {
      parts.add(
        when (billions) {
          1 -> "مليار"
          2 -> "ملياران"
          in 3..10 -> "${convertBelowThousand(billions)} مليارات"
          else -> "${convertBelowThousand(billions)} ملياراً"
        }
      )
    }

    if (millions > 0) {
      parts.add(
        when (millions) {
          1 -> "مليون"
          2 -> "مليونان"
          in 3..10 -> "${convertBelowThousand(millions)} ملايين"
          else -> "${convertBelowThousand(millions)} مليوناً"
        }
      )
    }

    if (thousands > 0) {
      parts.add(
        when (thousands) {
          1 -> "ألف"
          2 -> "ألفان"
          in 3..10 -> "${convertBelowThousand(thousands)} آلاف"
          else -> "${convertBelowThousand(thousands)} ألفاً"
        }
      )
    }

    if (remainder > 0) {
      parts.add(convertBelowThousand(remainder))
    }

    return parts.joinToString(" و")
  }

  private fun convertBelowThousand(n: Int): String {
    val h = n / 100
    val rem = n % 100
    val list = mutableListOf<String>()
    if (h > 0) list.add(hundreds[h])
    if (rem > 0) {
      if (rem < 20) {
        list.add(ones[rem])
      } else {
        val u = rem % 10
        val t = rem / 10
        if (u > 0) {
          list.add("${ones[u]} و${tens[t]}")
        } else {
          list.add(tens[t])
        }
      }
    }
    return list.joinToString(" و")
  }
}
