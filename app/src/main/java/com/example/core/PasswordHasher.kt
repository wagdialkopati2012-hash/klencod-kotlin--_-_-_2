package com.example.core

import java.security.MessageDigest
import java.security.SecureRandom

object PasswordHasher {
  private val secureRandom = SecureRandom()

  fun generateSalt(): String {
    val bytes = ByteArray(16)
    secureRandom.nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun hashPassword(password: String, salt: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val input = "$salt:$password".toByteArray(Charsets.UTF_8)
    val hashed = digest.digest(input)
    return hashed.joinToString("") { "%02x".format(it) }
  }

  fun verifyPassword(password: String, salt: String, expectedHash: String): Boolean {
    val actual = hashPassword(password, salt)
    return MessageDigest.isEqual(
      actual.toByteArray(Charsets.UTF_8),
      expectedHash.toByteArray(Charsets.UTF_8),
    )
  }
}
