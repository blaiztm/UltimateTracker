package com.example.ultimatetracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY title COLLATE NOCASE")
    fun observeAll(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    fun observeById(id: Long): Flow<MediaEntity?>

    @Upsert suspend fun upsert(item: MediaEntity): Long
    @Delete suspend fun delete(item: MediaEntity)
}
