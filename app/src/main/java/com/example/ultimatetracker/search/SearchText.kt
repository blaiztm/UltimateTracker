package com.example.ultimatetracker.search

import java.util.Locale

data class NormalizedText(val normalized: String, val compact: String, val tokens: List<String>)

fun normalizeSearchText(value: String): NormalizedText {
    val normalized = value.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")
    return NormalizedText(normalized, normalized.replace(" ", ""), normalized.split(' ').filter(String::isNotBlank))
}

fun damerauLevenshteinDistance(left: String, right: String): Int {
    val d = Array(left.length + 1) { IntArray(right.length + 1) }
    for (i in 0..left.length) d[i][0] = i
    for (j in 0..right.length) d[0][j] = j
    for (i in 1..left.length) for (j in 1..right.length) {
        val cost = if (left[i - 1] == right[j - 1]) 0 else 1
        d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
        if (i > 1 && j > 1 && left[i - 1] == right[j - 2] && left[i - 2] == right[j - 1])
            d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + cost)
    }
    return d[left.length][right.length]
}

fun normalizedSimilarity(left: String, right: String): Double {
    if (left == right) return 1.0
    val maxLength = maxOf(left.length, right.length)
    return if (maxLength == 0) 1.0 else 1.0 - damerauLevenshteinDistance(left, right).toDouble() / maxLength
}

fun textMatchScore(query: NormalizedText, candidate: NormalizedText): Double {
    if (query.normalized.isEmpty()) return 1.0
    if (candidate.normalized == query.normalized) return 1.0
    if (candidate.compact == query.compact) return 0.98
    if (candidate.normalized.startsWith(query.normalized)) return 0.92
    if (candidate.normalized.contains(query.normalized)) return 0.88
    val compactScore = normalizedSimilarity(query.compact, candidate.compact)
    val tokenScore = query.tokens.maxOfOrNull { q -> candidate.tokens.maxOfOrNull { normalizedSimilarity(q, it) } ?: 0.0 } ?: 0.0
    return maxOf(compactScore, tokenScore)
}
