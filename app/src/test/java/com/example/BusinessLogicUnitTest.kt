package com.example

import com.example.core.MoneyUtils
import com.example.core.PasswordHasher
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessLogicUnitTest {

  @Test
  fun `bigDecimal quotation calculations avoid floating point drift`() {
    val unitPrice = MoneyUtils.parseUsdOrNull("١٢٠.٥٥")
    assertNotNull(unitPrice)
    val lineTotal = MoneyUtils.calculateLineTotal(unitPrice!!, 3)
    assertEquals(BigDecimal("361.65"), lineTotal)

    val grandTotal =
      MoneyUtils.calculateQuotationTotal(listOf(lineTotal, BigDecimal("100.35")))
    assertEquals(BigDecimal("462.00"), grandTotal)
    assertEquals("$462", MoneyUtils.formatUsd(grandTotal))
  }

  @Test
  fun `arabic tafqeet formats dollars and cents accurately`() {
    val words = MoneyUtils.amountInArabicWords(BigDecimal("2850"))
    assertTrue(words.contains("ألفان"))
    assertTrue(words.contains("ثمانمائة"))
    assertTrue(words.contains("خمسون"))
    assertTrue(words.contains("دولاراً أمريكياً"))

    val withCents = MoneyUtils.amountInArabicWords(BigDecimal("125.50"))
    assertTrue(withCents.contains("سنتاً"))
  }

  @Test
  fun `password hashing verifies valid password and rejects wrong password`() {
    val salt = PasswordHasher.generateSalt()
    val hash = PasswordHasher.hashPassword("Secret@2026", salt)
    assertTrue(PasswordHasher.verifyPassword("Secret@2026", salt, hash))
    assertFalse(PasswordHasher.verifyPassword("wrong", salt, hash))
  }
}
