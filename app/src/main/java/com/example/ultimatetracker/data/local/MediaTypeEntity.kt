package com.example.ultimatetracker.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "custom_media_types", indices = [Index(value = ["name"], unique = true)])
data class MediaTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Dao
interface MediaTypeDao {
    @Query("SELECT name FROM custom_media_types ORDER BY name COLLATE NOCASE")
    fun observeNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(type: MediaTypeEntity)
}
