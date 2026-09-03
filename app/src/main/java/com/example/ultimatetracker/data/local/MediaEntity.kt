package com.example.ultimatetracker.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ultimatetracker.data.local.account.UserListEntity
import com.example.ultimatetracker.data.model.MediaItem

@Entity(
    tableName = "media_items",
    foreignKeys = [ForeignKey(
        entity = UserListEntity::class,
        parentColumns = ["id"],
        childColumns = ["listId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["listId"]), Index(value = ["listId", "updatedAt"])],
)
data class MediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val title: String,
    val type: String,
    val length: Int,
    val genres: List<String>,
    val keywords: List<String>,
    val category: String,
    val coverUri: String?,
    val review: String,
    val rating: Int?,
    val priority: Int? = null,
    val watchedEpisodes: Int = 0,
    val watchStartedAt: Long? = null,
    val watchEndedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val rowVersion: Long = 1,
    val deletedAt: Long? = null,
)

fun MediaEntity.toModel() = MediaItem(id, title, type, length, genres, keywords, category, coverUri, review, rating, priority, watchedEpisodes, watchStartedAt, watchEndedAt, createdAt, updatedAt, listId, rowVersion)
fun MediaItem.toEntity(ownedListId: Long) = MediaEntity(id, ownedListId, title, type, length, genres, keywords, category, coverUri, review, rating, priority, watchedEpisodes, watchStartedAt, watchEndedAt, createdAt, updatedAt, rowVersion)
