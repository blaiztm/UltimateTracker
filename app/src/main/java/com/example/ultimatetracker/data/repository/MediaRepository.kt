package com.example.ultimatetracker.data.repository

import com.example.ultimatetracker.data.local.MediaDao
import com.example.ultimatetracker.data.local.MediaTypeDao
import com.example.ultimatetracker.data.local.MediaTypeEntity
import com.example.ultimatetracker.data.local.toEntity
import com.example.ultimatetracker.data.local.toModel
import com.example.ultimatetracker.data.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class MediaRepository(
    private val dao: MediaDao,
    private val typeDao: MediaTypeDao,
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
}
