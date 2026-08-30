package com.example.ultimatetracker.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasswordDigest(val hash: String, val salt: String, val iterations: Int)

class PasswordHasher(
    private val iterations: Int = 210_000,
    private val random: SecureRandom = SecureRandom(),
) {
    fun hash(password: CharArray): PasswordDigest {
        val salt = ByteArray(16).also(random::nextBytes)
        val bytes = derive(password, salt, iterations)
        return PasswordDigest(encode(bytes), encode(salt), iterations)
    }

    fun verify(password: CharArray, expectedHash: String, salt: String, iterations: Int): Boolean {
        val actual = derive(password, decode(salt), iterations)
        return MessageDigest.isEqual(actual, decode(expectedHash))
    }

    private fun derive(password: CharArray, salt: ByteArray, rounds: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, rounds, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            password.fill('\u0000')
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().withoutPadding().encodeToString(bytes)
    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)
}

class SessionTokenService(private val random: SecureRandom = SecureRandom()) {
    fun create(): String = ByteArray(32).also(random::nextBytes).let {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it)
    }

    fun hash(token: String): String = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

fun normalizeEmail(value: String): String = value.trim().lowercase()

fun isValidEmail(value: String): Boolean {
    val normalized = normalizeEmail(value)
    return normalized.length in 3..254 && normalized.substringAfterLast('@', "").contains('.') &&
        normalized.count { it == '@' } == 1 && !normalized.any(Char::isWhitespace)
}

fun isValidPassword(value: CharSequence): Boolean =
    value.length in 10..128 && value.any(Char::isLetter) && value.any(Char::isDigit)
