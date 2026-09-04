package com.example.ultimatetracker.viewmodel

import com.example.ultimatetracker.data.model.BuiltInMediaTypes
import com.example.ultimatetracker.data.model.MediaItem
import com.example.ultimatetracker.data.model.WatchCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MediaStatisticsAndSortingTest {
    @Test fun completedWorksContributeFullEpisodeAndMovieStatistics() {
        val state = calculateStatistics(listOf(
            item("Series", BuiltInMediaTypes.SERIES, 12, WatchCategory.COMPLETED, watched = 0),
            item("Watching", BuiltInMediaTypes.ANIME, 10, WatchCategory.WATCHING, watched = 4),
            item("Done movie", BuiltInMediaTypes.MOVIE, 120, WatchCategory.COMPLETED),
            item("Planned movie", BuiltInMediaTypes.MOVIE, 90, WatchCategory.PLANNED),
        ))

        assertEquals(16, state.watchedEpisodes)
        assertEquals(22, state.totalEpisodes)
        assertEquals(120, state.movieMinutes)
    }

    @Test fun dateSortingKeepsMissingDatesLastInBothDirections() {
        val undated = item("Undated", started = null)
        val early = item("Early", started = 10)
        val late = item("Late", started = 20)

        assertEquals(listOf("Early", "Late", "Undated"), listOf(undated, late, early).sortedWith(SortMode.WATCH_START_DATE.comparator(SortDirection.ASCENDING)).map { it.title })
        assertEquals(listOf("Late", "Early", "Undated"), listOf(undated, early, late).sortedWith(SortMode.WATCH_START_DATE.comparator(SortDirection.DESCENDING)).map { it.title })
    }

    @Test fun libraryIdentityIsExactNormalizedAndDistinguishesMovieFromSeries() {
        assertEquals("title".libraryIdentity(BuiltInMediaTypes.MOVIE), "  TITLE  ".libraryIdentity(BuiltInMediaTypes.MOVIE))
        assertNotEquals("Title".libraryIdentity(BuiltInMediaTypes.MOVIE), "Title".libraryIdentity(BuiltInMediaTypes.SERIES))
        assertNotEquals("Title".libraryIdentity(BuiltInMediaTypes.MOVIE), "Title extended".libraryIdentity(BuiltInMediaTypes.MOVIE))
    }

    private fun item(
        title: String,
        type: String = BuiltInMediaTypes.SERIES,
        length: Int = 1,
        category: WatchCategory = WatchCategory.WATCHING,
        watched: Int = 0,
        started: Long? = null,
    ) = MediaItem(0, title, type, length, emptyList(), emptyList(), category.name, null, "", null, watchedEpisodes = watched, watchStartedAt = started, createdAt = 0, updatedAt = 0)
}
