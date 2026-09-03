package com.example.ultimatetracker.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ultimatetracker.data.local.account.UserListEntity
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "status_overrides",
    primaryKeys = ["listId", "status"],
    foreignKeys = [ForeignKey(entity = UserListEntity::class, parentColumns = ["id"], childColumns = ["listId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("listId")],
)
data class StatusOverrideEntity(
    val listId: Long,
    val status: String,
    val name: String,
    val color: String,
)

@Dao
interface StatusOverrideDao {
    @Query("SELECT * FROM status_overrides WHERE listId = :listId")
    fun observeAll(listId: Long): Flow<List<StatusOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: StatusOverrideEntity)
}
