package com.example.ultimatetracker.data.backup

import com.example.ultimatetracker.data.model.WatchCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    private val payload = BackupPayload(
        exportedAt = 1234,
        appVersion = "alpha-1.5",
        lists = listOf(BackupList(
            title = "Favorites",
            position = 0,
            archived = false,
            items = listOf(BackupItem(
                title = "Example",
                type = "SERIES",
                length = 12,
                genres = listOf("Drama"),
                keywords = listOf("Sample"),
                category = WatchCategory.WATCHING,
                coverUri = "https://example.com/cover.jpg",
                coverMimeType = null,
                coverBase64 = null,
                review = "Good",
                rating = 8,
                watchedEpisodes = 4,
                createdAt = 100,
                updatedAt = 200,
            )),
        )),
    )

    @Test
    fun roundTripPreservesListsAndItemsWithoutCredentials() {
        val json = BackupCodec.encode(payload)
        val decoded = BackupCodec.decode(json)

        assertEquals(payload, decoded)
        assertTrue(json.contains(BackupCodec.FORMAT))
        assertFalse(json.contains("password", ignoreCase = true))
        assertFalse(json.contains("session", ignoreCase = true))
    }

    @Test
    fun rejectsWrongFormatAndFutureSchema() {
        val wrong = BackupCodec.encode(payload).replace(BackupCodec.FORMAT, "some-other-app")
        val wrongError = assertThrows(BackupFormatException::class.java) { BackupCodec.decode(wrong) }
        assertEquals(BackupFormatException.Reason.WRONG_FORMAT, wrongError.reason)

        val future = BackupCodec.encode(payload).replace("\"schemaVersion\": ${BackupCodec.SCHEMA_VERSION}", "\"schemaVersion\": 99")
        val futureError = assertThrows(BackupFormatException::class.java) { BackupCodec.decode(future) }
        assertEquals(BackupFormatException.Reason.UNSUPPORTED_VERSION, futureError.reason)
    }

    @Test
    fun rejectsInvalidRangesBeforeImport() {
        val invalid = payload.copy(lists = listOf(payload.lists.single().copy(
            items = listOf(payload.lists.single().items.single().copy(rating = 11)),
        )))
        val error = assertThrows(BackupFormatException::class.java) { BackupCodec.encode(invalid) }
        assertEquals(BackupFormatException.Reason.INVALID_DATA, error.reason)
    }

    @Test
    fun preservesEmbeddedCoverPayload() {
        val embedded = payload.copy(lists = listOf(payload.lists.single().copy(
            items = listOf(payload.lists.single().items.single().copy(
                coverUri = null,
                coverMimeType = "image/png",
                coverBase64 = "AQIDBA==",
            )),
        )))
        assertEquals(embedded, BackupCodec.decode(BackupCodec.encode(embedded)))
    }
}
