package com.example.ultimatetracker.search

import com.example.ultimatetracker.data.remote.CatalogItem

class CatalogSearchRanker {
    fun rank(query: String, items: List<CatalogItem>): List<CatalogItem> {
        val q = normalizeSearchText(query)
        return items.withIndex().sortedWith(compareByDescending<IndexedValue<CatalogItem>> { maxOf(textMatchScore(q, normalizeSearchText(it.value.title)), textMatchScore(q, normalizeSearchText(it.value.originalTitle))) }.thenBy { it.index }).map { it.value }
    }
}
