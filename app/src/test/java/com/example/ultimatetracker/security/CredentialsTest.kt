package com.example.ultimatetracker.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialsTest {
    @Test
    fun passwordHashUsesRandomSaltAndVerifiesWithoutStoringPassword() {
        val hasher = PasswordHasher(iterations = 1_000)
        val first = hasher.hash("correct horse 7".toCharArray())
        val second = hasher.hash("correct horse 7".toCharArray())

        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
        assertTrue(hasher.verify("correct horse 7".toCharArray(), first.hash, first.salt, first.iterations))
        assertFalse(hasher.verify("wrong password 7".toCharArray(), first.hash, first.salt, first.iterations))
        assertFalse(first.hash.contains("correct horse"))
    }

    @Test
    fun sessionTokensAreRandomAndOnlyStableAfterHashing() {
        val service = SessionTokenService()
        val first = service.create()
        val second = service.create()

        assertNotEquals(first, second)
        assertNotEquals(first, service.hash(first))
        assertTrue(service.hash(first) == service.hash(first))
    }

    @Test
    fun credentialsAreNormalizedAndValidated() {
        assertTrue(isValidEmail(" Person@Example.COM "))
        assertTrue(normalizeEmail(" Person@Example.COM ") == "person@example.com")
        assertFalse(isValidEmail("not-an-email"))
        assertTrue(isValidPassword("long password 7"))
        assertFalse(isValidPassword("short7"))
        assertFalse(isValidPassword("onlyletterslong"))
    }
}
