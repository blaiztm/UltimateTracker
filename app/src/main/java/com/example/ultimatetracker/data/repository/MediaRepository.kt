package com.example.ultimatetracker.data.repository

import androidx.room.withTransaction
import com.example.ultimatetracker.data.local.AppDatabase
import com.example.ultimatetracker.data.local.CategoryDao
import com.example.ultimatetracker.data.local.CategoryEntity
import com.example.ultimatetracker.data.local.MediaDao
import com.example.ultimatetracker.data.local.MediaTypeDao
import com.example.ultimatetracker.data.local.MediaTypeEntity
import com.example.ultimatetracker.data.local.StatusOverrideDao
import com.example.ultimatetracker.data.local.StatusOverrideEntity
import com.example.ultimatetracker.data.local.toEntity
import com.example.ultimatetracker.data.local.toModel
import com.example.ultimatetracker.data.model.MediaItem
import com.example.ultimatetracker.data.model.CategoryColor
import com.example.ultimatetracker.data.model.CategoryRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.UUID
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MediaRepository(
    private val database: AppDatabase,
    private val dao: MediaDao,
    private val typeDao: MediaTypeDao,
    private val categoryDao: CategoryDao,
    private val statusOverrideDao: StatusOverrideDao,
    private val identityStore: ActiveIdentityStore,
) {
    fun observeAll(): Flow<List<MediaItem>> = identityStore.identity.flatMapLatest { identity ->
        if (identity == null) flowOf(emptyList())
        else dao.observeAll(identity.listId).map { list -> list.map { it.toModel() } }
    }

    fun observeById(id: Long): Flow<MediaItem?> = identityStore.identity.flatMapLatest { identity ->
        if (identity == null) flowOf(null)
        else dao.observeById(id, identity.listId).map { it?.toModel() }
    }

    suspend fun save(item: MediaItem): Long {
        val listId = identityStore.identity.value?.listId ?: throw SecurityException("No active list")
        if (item.id != 0L && item.listId != 0L && item.listId != listId) throw SecurityException("List ownership mismatch")
        if (CategoryRef.builtIn(item.category) == null && categoryDao.belongsToList(item.category, listId) != 1) throw IllegalArgumentException("Category is not owned by the active list")
        return dao.upsert(item.toEntity(listId))
    }

    suspend fun delete(item: MediaItem) {
        val listId = identityStore.identity.value?.listId ?: throw SecurityException("No active list")
        check(dao.softDelete(item.id, listId, System.currentTimeMillis()) == 1) { "Item is not owned by the active list" }
    }
    fun observeCustomTypes(): Flow<List<String>> = typeDao.observeNames()
    suspend fun addCustomType(name: String) {
        val normalized = name.trim()
        if (normalized.isNotEmpty()) typeDao.insert(MediaTypeEntity(name = normalized))
    }
    fun observeCategories() = identityStore.identity.flatMapLatest { identity ->
        if (identity == null) flowOf(emptyList()) else categoryDao.observeAll(identity.listId)
    }
    fun observeStatusOverrides() = identityStore.identity.flatMapLatest { identity ->
        if (identity == null) flowOf(emptyList()) else statusOverrideDao.observeAll(identity.listId)
    }
    suspend fun updateBuiltInStatus(status: String, name: String, color: CategoryColor) {
        val listId = identityStore.identity.value?.listId ?: throw SecurityException("No active list")
        require(CategoryRef.builtIn(status) != null)
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        statusOverrideDao.upsert(StatusOverrideEntity(listId, status, trimmed, color.name))
    }
    @Suppress("unused")
    suspend fun addCategory(name: String, color: CategoryColor) {
        val listId = identityStore.identity.value?.listId ?: throw SecurityException("No active list")
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        categoryDao.insert(CategoryEntity(UUID.randomUUID().toString(), listId, trimmed, trimmed.lowercase(Locale.ROOT), color.name, categoryDao.nextPosition(listId), System.currentTimeMillis()))
    }
    @Suppress("unused")
    suspend fun deleteCategory(id: String, replacement: String) {
        val listId = identityStore.identity.value?.listId ?: throw SecurityException("No active list")
        require(replacement != id)
        if (CategoryRef.builtIn(replacement) == null && categoryDao.belongsToList(replacement, listId) != 1) throw IllegalArgumentException("Replacement is not owned by the active list")
        database.withTransaction {
            check(categoryDao.delete(id, listId) == 1) { "Category is not owned by the active list" }
            dao.replaceCategory(listId, id, replacement, System.currentTimeMillis())
        }
    }
}
