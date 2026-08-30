package com.example.ultimatetracker.search

import com.example.ultimatetracker.data.model.MediaItem

data class SearchResult<T>(val value: T, val score: Double)

class MediaSearchEngine {
    fun search(query: String, items: List<MediaItem>): List<SearchResult<MediaItem>> {
        val normalized = normalizeSearchText(query)
        if (normalized.normalized.isEmpty()) return items.map { SearchResult(it, 1.0) }
        val threshold = when (normalized.compact.length) { in 0..2 -> 1.0; in 3..4 -> .80; in 5..7 -> .72; else -> .68 }
        return items.mapNotNull { item ->
            val score = maxOf(textMatchScore(normalized, normalizeSearchText(item.title)), item.keywords.maxOfOrNull { textMatchScore(normalized, normalizeSearchText(it)) * .85 } ?: 0.0)
            if (score >= threshold) SearchResult(item, score) else null
        }.sortedWith(compareByDescending<SearchResult<MediaItem>> { it.score }.thenBy { it.value.title.lowercase() }.thenBy { it.value.id })
    }
}
