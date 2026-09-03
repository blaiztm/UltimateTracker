package com.example.ultimatetracker.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.ultimatetracker.data.local.account.UserListEntity
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "custom_categories",
    foreignKeys = [ForeignKey(entity = UserListEntity::class, parentColumns = ["id"], childColumns = ["listId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["listId", "normalizedName"], unique = true), Index(value = ["listId", "position"])],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val listId: Long,
    val name: String,
    val normalizedName: String,
    val color: String,
    val position: Long,
    val createdAt: Long,
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM custom_categories WHERE listId = :listId ORDER BY position, name COLLATE NOCASE") fun observeAll(listId: Long): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM custom_categories WHERE listId IN (:listIds) ORDER BY listId, position") suspend fun snapshotForLists(listIds: List<Long>): List<CategoryEntity>
    @Query("SELECT COUNT(*) FROM custom_categories WHERE id = :id AND listId = :listId") suspend fun belongsToList(id: String, listId: Long): Int
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM custom_categories WHERE listId = :listId") suspend fun nextPosition(listId: Long): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(category: CategoryEntity)
    @Query("DELETE FROM custom_categories WHERE id = :id AND listId = :listId") suspend fun delete(id: String, listId: Long): Int
}
