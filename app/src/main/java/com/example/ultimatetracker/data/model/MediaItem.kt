package com.example.ultimatetracker.data.model

enum class MediaType(val title: String) {
    MOVIE("Фильм"), SERIES("Сериал"), ANIME("Аниме")
}

enum class WatchCategory(val title: String) {
    PLANNED("Запланировано"), WATCHING("Смотрю"), COMPLETED("Просмотрено"), ON_HOLD("Отложено")
}

data class MediaItem(
    val id: Long = 0,
    val title: String,
    val type: MediaType,
    val length: Int,
    val genres: List<String>,
    val keywords: List<String>,
    val category: WatchCategory,
    val coverUri: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
