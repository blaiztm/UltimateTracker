package com.example.ultimatetracker.data.repository

import com.example.ultimatetracker.data.local.MediaDao
import com.example.ultimatetracker.data.local.toEntity
import com.example.ultimatetracker.data.local.toModel
import com.example.ultimatetracker.data.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MediaRepository(private val dao: MediaDao) {
    fun observeAll(): Flow<List<MediaItem>> = dao.observeAll().map { list -> list.map { it.toModel() } }
    fun observeById(id: Long): Flow<MediaItem?> = dao.observeById(id).map { it?.toModel() }
    suspend fun save(item: MediaItem) = dao.upsert(item.toEntity())
    suspend fun delete(item: MediaItem) = dao.delete(item.toEntity())
}
