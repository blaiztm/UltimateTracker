package com.example.ultimatetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ultimatetracker.data.model.MediaItem
import com.example.ultimatetracker.data.model.MediaType
import com.example.ultimatetracker.data.model.WatchCategory

@Entity(tableName = "media_items")
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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

fun MediaEntity.toModel() = MediaItem(id, title, type, length, genres, keywords, category, coverUri, createdAt, updatedAt)
fun MediaItem.toEntity() = MediaEntity(id, title, type, length, genres, keywords, category, coverUri, createdAt, updatedAt)
