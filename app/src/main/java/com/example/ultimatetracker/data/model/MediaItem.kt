package com.example.ultimatetracker.data.model

import androidx.annotation.StringRes
import com.example.ultimatetracker.R

object BuiltInMediaTypes {
    const val MOVIE = "MOVIE"
    const val SERIES = "SERIES"
    const val ANIME = "ANIME"
    val entries = listOf(MOVIE, SERIES, ANIME)
}

enum class WatchCategory(@param:StringRes val titleRes: Int) {
    PLANNED(R.string.planned), WATCHING(R.string.watching), COMPLETED(R.string.completed), ON_HOLD(R.string.on_hold)
}

@StringRes
fun builtInTypeTitleRes(type: String): Int? = when (type) {
    BuiltInMediaTypes.MOVIE -> R.string.movie
    BuiltInMediaTypes.SERIES -> R.string.series
    BuiltInMediaTypes.ANIME -> R.string.anime
    else -> null
}

data class MediaItem(
    val id: Long = 0,
    val title: String,
    val type: String,
    val length: Int,
    val genres: List<String>,
    val keywords: List<String>,
    val category: WatchCategory,
    val coverUri: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
