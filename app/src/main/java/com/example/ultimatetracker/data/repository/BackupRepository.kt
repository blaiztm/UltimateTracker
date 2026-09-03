package com.example.ultimatetracker.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.example.ultimatetracker.data.backup.BackupCodec
import com.example.ultimatetracker.data.backup.BackupFormatException
import com.example.ultimatetracker.data.backup.BackupItem
import com.example.ultimatetracker.data.backup.BackupCategory
import com.example.ultimatetracker.data.backup.BackupList
import com.example.ultimatetracker.data.backup.BackupPayload
import com.example.ultimatetracker.data.local.AppDatabase
import com.example.ultimatetracker.data.local.MediaEntity
import com.example.ultimatetracker.data.local.account.AuditEventEntity
import com.example.ultimatetracker.data.local.account.UserListEntity
import com.example.ultimatetracker.data.model.BuiltInMediaTypes
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.data.model.CategoryRef
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.UUID

data class BackupResult(val listCount: Int, val itemCount: Int)

class BackupRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val identityStore: ActiveIdentityStore,
) {
    suspend fun exportTo(uri: Uri, appVersion: String): BackupResult = withContext(Dispatchers.IO) {
        val identity = identityStore.identity.value ?: throw BackupFormatException(BackupFormatException.Reason.INVALID_DATA)
        val lists = database.accountDao().snapshotLists(identity.userId)
        val items = if (lists.isEmpty()) emptyList() else database.mediaDao().snapshotForLists(lists.map { it.id })
        val byList = items.groupBy { it.listId }
        val categoriesByList = if (lists.isEmpty()) emptyMap() else database.categoryDao().snapshotForLists(lists.map { it.id }).groupBy { it.listId }
        val payload = BackupPayload(
            exportedAt = System.currentTimeMillis(),
            appVersion = appVersion,
            lists = lists.map { list ->
                BackupList(list.title, list.position, list.archivedAt != null, categoriesByList[list.id].orEmpty().map { BackupCategory(it.id, it.name, com.example.ultimatetracker.data.model.CategoryColor.valueOf(it.color), it.position, it.createdAt) }, byList[list.id].orEmpty().map(::toBackupItem))
            },
        )
        val bytes = BackupCodec.encode(payload).toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_BACKUP_BYTES) throw BackupFormatException(BackupFormatException.Reason.LIMIT_EXCEEDED)
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: throw IllegalStateException("Cannot open destination")
        database.accountDao().insertAudit(AuditEventEntity(userId = identity.userId, action = "backup.exported", subjectType = "backup", subjectId = null, createdAt = System.currentTimeMillis()))
        BackupResult(lists.size, items.size)
    }

    suspend fun importFrom(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val identity = identityStore.identity.value ?: throw BackupFormatException(BackupFormatException.Reason.INVALID_DATA)
        val json = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_BACKUP_BYTES) throw BackupFormatException(BackupFormatException.Reason.LIMIT_EXCEEDED)
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        } ?: throw IllegalStateException("Cannot open source")
        val payload = BackupCodec.decode(json)
        val createdCovers = mutableListOf<File>()
        try {
            database.withTransaction {
                val accountDao = database.accountDao()
                val mediaDao = database.mediaDao()
                payload.lists.sortedBy { it.position }.forEach { sourceList ->
                    val time = System.currentTimeMillis()
                    val listId = accountDao.insertList(UserListEntity(
                        ownerUserId = identity.userId,
                        title = sourceList.title,
                        position = accountDao.nextListPosition(identity.userId),
                        createdAt = time,
                        updatedAt = time,
                        archivedAt = time.takeIf { sourceList.archived },
                    ))
                    sourceList.categories.forEach { category ->
                        database.categoryDao().insert(com.example.ultimatetracker.data.local.CategoryEntity(category.id, listId, category.name, category.name.trim().lowercase(), category.color.name, category.position, category.createdAt))
                    }
                    sourceList.items.forEach { item ->
                        item.type.takeUnless { it in BuiltInMediaTypes.entries }?.let { database.mediaTypeDao().insert(com.example.ultimatetracker.data.local.MediaTypeEntity(name = it)) }
                        val cover = materializeCover(item, createdCovers)
                        mediaDao.upsert(MediaEntity(
                            listId = listId,
                            title = item.title,
                            type = item.type,
                            length = item.length,
                            genres = item.genres,
                            keywords = item.keywords,
                            category = item.category,
                            coverUri = cover,
                            review = item.review,
                            rating = item.rating,
                            priority = item.priority,
                            watchedEpisodes = item.watchedEpisodes,
                            watchStartedAt = item.watchStartedAt,
                            watchEndedAt = item.watchEndedAt,
                            createdAt = item.createdAt,
                            updatedAt = item.updatedAt,
                        ))
                    }
                }
                accountDao.insertAudit(AuditEventEntity(userId = identity.userId, action = "backup.imported", subjectType = "backup", subjectId = null, createdAt = System.currentTimeMillis()))
            }
        } catch (error: Throwable) {
            createdCovers.forEach(File::delete)
            throw error
        }
        BackupResult(payload.lists.size, payload.lists.sumOf { it.items.size })
    }

    private fun toBackupItem(entity: MediaEntity): BackupItem {
        val cover = entity.coverUri
        val localBytes = cover?.takeUnless { it.startsWith("http://") || it.startsWith("https://") }?.let(::readCover)
        return BackupItem(
            title = entity.title,
            type = entity.type,
            length = entity.length,
            genres = entity.genres,
            keywords = entity.keywords,
            category = entity.category,
            coverUri = cover?.takeIf { it.startsWith("http://") || it.startsWith("https://") },
            coverMimeType = localBytes?.first,
            coverBase64 = localBytes?.second,
            review = entity.review,
            rating = entity.rating,
            priority = entity.priority,
            watchedEpisodes = entity.watchedEpisodes,
            watchStartedAt = entity.watchStartedAt,
            watchEndedAt = entity.watchEndedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    private fun readCover(value: String): Pair<String, String>? {
        val uri = value.toUri()
        val input = when (uri.scheme) {
            "file" -> uri.path?.let(::File)?.takeIf(File::isFile)?.let(::FileInputStream)
            else -> context.contentResolver.openInputStream(uri)
        } ?: return null
        val bytes = input.use { it.readBounded(BackupCodec.MAX_EMBEDDED_COVER_BYTES) }
        val mime = context.contentResolver.getType(uri) ?: guessMime(uri.lastPathSegment.orEmpty())
        return mime to Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun materializeCover(item: BackupItem, created: MutableList<File>): String? {
        item.coverUri?.let { return it }
        val encoded = item.coverBase64 ?: return null
        val bytes = try { Base64.decode(encoded, Base64.DEFAULT) } catch (_: IllegalArgumentException) {
            throw BackupFormatException(BackupFormatException.Reason.INVALID_DATA)
        }
        if (bytes.size > BackupCodec.MAX_EMBEDDED_COVER_BYTES) throw BackupFormatException(BackupFormatException.Reason.LIMIT_EXCEEDED)
        val directory = File(context.filesDir, "imported_covers").also { it.mkdirs() }
        val extension = when (item.coverMimeType) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        val file = File(directory, "${UUID.randomUUID()}.$extension")
        file.outputStream().use { it.write(bytes) }
        created += file
        return Uri.fromFile(file).toString()
    }

    private fun java.io.InputStream.readBounded(limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            if (total > limit) throw BackupFormatException(BackupFormatException.Reason.LIMIT_EXCEEDED)
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun guessMime(name: String): String = when {
        name.endsWith(".png", true) -> "image/png"
        name.endsWith(".webp", true) -> "image/webp"
        else -> "image/jpeg"
    }

    private companion object { const val MAX_BACKUP_BYTES = 50 * 1024 * 1024 }
}
