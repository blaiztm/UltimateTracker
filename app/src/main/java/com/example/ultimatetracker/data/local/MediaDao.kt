package com.example.ultimatetracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE listId = :listId AND deletedAt IS NULL ORDER BY title COLLATE NOCASE")
    fun observeAll(listId: Long): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id AND listId = :listId AND deletedAt IS NULL")
    fun observeById(id: Long, listId: Long): Flow<MediaEntity?>

    @Query("SELECT * FROM media_items WHERE listId IN (:listIds) AND deletedAt IS NULL ORDER BY listId, createdAt, id")
    suspend fun snapshotForLists(listIds: List<Long>): List<MediaEntity>

    @Upsert suspend fun upsert(item: MediaEntity): Long

    @Query("UPDATE media_items SET category = :replacement, updatedAt = :now, rowVersion = rowVersion + 1 WHERE listId = :listId AND category = :category AND deletedAt IS NULL")
    suspend fun replaceCategory(listId: Long, category: String, replacement: String, now: Long)

    @Query("UPDATE media_items SET deletedAt = :now, updatedAt = :now, rowVersion = rowVersion + 1 WHERE id = :id AND listId = :listId AND deletedAt IS NULL")
    suspend fun softDelete(id: Long, listId: Long, now: Long): Int
}
