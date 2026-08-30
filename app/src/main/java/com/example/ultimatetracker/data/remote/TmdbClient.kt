package com.example.ultimatetracker.data.remote

import com.example.ultimatetracker.data.model.BuiltInMediaTypes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class CatalogItem(
    val id: Long,
    val title: String,
    val originalTitle: String,
    val mediaType: String,
    val coverUri: String?,
    val year: String?,
)

class TmdbClient(private val token: String) {
    val isConfigured: Boolean get() = token.isNotBlank()

    suspend fun search(query: String, language: String): List<CatalogItem> = withContext(Dispatchers.IO) {
        if (!isConfigured || query.isBlank()) return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8.name())
        val locale = if (language == "ru") "ru-RU" else "en-US"
        val connection = URL("https://api.themoviedb.org/3/search/multi?query=$encodedQuery&include_adult=false&language=$locale&page=1")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode !in 200..299) error("TMDB HTTP ${connection.responseCode}")
            val root = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val results = root.getJSONArray("results")
            buildList {
                for (index in 0 until results.length()) {
                    val item = results.getJSONObject(index)
                    val kind = item.optString("media_type")
                    if (kind != "movie" && kind != "tv") continue
                    val titleKey = if (kind == "movie") "title" else "name"
                    val originalKey = if (kind == "movie") "original_title" else "original_name"
                    val dateKey = if (kind == "movie") "release_date" else "first_air_date"
                    val title = item.optString(titleKey).takeIf(String::isNotBlank) ?: continue
                    val posterPath = item.optString("poster_path").takeIf { it.isNotBlank() && it != "null" }
                    add(
                        CatalogItem(
                            id = item.getLong("id"),
                            title = title,
                            originalTitle = item.optString(originalKey, title),
                            mediaType = if (kind == "movie") BuiltInMediaTypes.MOVIE else BuiltInMediaTypes.SERIES,
                            coverUri = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                            year = item.optString(dateKey).takeIf { it.length >= 4 }?.take(4),
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
