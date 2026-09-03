package com.example.ultimatetracker.data.backup

import com.example.ultimatetracker.data.model.CategoryColor
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class BackupPayload(
    val exportedAt: Long,
    val appVersion: String,
    val lists: List<BackupList>,
)

data class BackupList(
    val title: String,
    val position: Int,
    val archived: Boolean,
    val categories: List<BackupCategory> = emptyList(),
    val items: List<BackupItem>,
)

data class BackupCategory(val id: String, val name: String, val color: CategoryColor, val position: Long, val createdAt: Long)

data class BackupItem(
    val title: String,
    val type: String,
    val length: Int,
    val genres: List<String>,
    val keywords: List<String>,
    val category: String,
    val coverUri: String?,
    val coverMimeType: String?,
    val coverBase64: String?,
    val review: String,
    val rating: Int?,
    val priority: Int? = null,
    val watchedEpisodes: Int,
    val watchStartedAt: Long? = null,
    val watchEndedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

class BackupFormatException(val reason: Reason) : IllegalArgumentException(reason.name) {
    enum class Reason { MALFORMED, WRONG_FORMAT, UNSUPPORTED_VERSION, LIMIT_EXCEEDED, INVALID_DATA }
}

object BackupCodec {
    const val FORMAT = "ultimate-tracker-backup"
    const val SCHEMA_VERSION = 3
    const val MAX_LISTS = 500
    const val MAX_ITEMS = 50_000
    const val MAX_EMBEDDED_COVER_BYTES = 5 * 1024 * 1024

    fun encode(payload: BackupPayload): String {
        validate(payload)
        val root = JSONObject()
            .put("format", FORMAT)
            .put("schemaVersion", SCHEMA_VERSION)
            .put("exportedAt", payload.exportedAt)
            .put("appVersion", payload.appVersion)
        val lists = JSONArray()
        payload.lists.forEach { list ->
            val items = JSONArray()
            list.items.forEach { item ->
                items.put(JSONObject()
                    .put("title", item.title)
                    .put("type", item.type)
                    .put("length", item.length)
                    .put("genres", JSONArray(item.genres))
                    .put("keywords", JSONArray(item.keywords))
                    .put("category", item.category)
                    .put("coverUri", item.coverUri ?: JSONObject.NULL)
                    .put("coverMimeType", item.coverMimeType ?: JSONObject.NULL)
                    .put("coverBase64", item.coverBase64 ?: JSONObject.NULL)
                    .put("review", item.review)
                    .put("rating", item.rating ?: JSONObject.NULL)
                    .put("priority", item.priority ?: JSONObject.NULL)
                    .put("watchedEpisodes", item.watchedEpisodes)
                    .put("watchStartedAt", item.watchStartedAt ?: JSONObject.NULL)
                    .put("watchEndedAt", item.watchEndedAt ?: JSONObject.NULL)
                    .put("createdAt", item.createdAt)
                    .put("updatedAt", item.updatedAt))
            }
            lists.put(JSONObject()
                .put("title", list.title)
                .put("position", list.position)
                .put("archived", list.archived)
                .put("categories", JSONArray(list.categories.map { JSONObject().put("id", it.id).put("name", it.name).put("color", it.color.name).put("position", it.position).put("createdAt", it.createdAt) }))
                .put("items", items))
        }
        return root.put("lists", lists).toString(2)
    }

    fun decode(json: String): BackupPayload {
        try {
            val root = JSONObject(json)
            if (root.optString("format") != FORMAT) throw BackupFormatException(BackupFormatException.Reason.WRONG_FORMAT)
            if (root.optInt("schemaVersion", -1) !in 1..SCHEMA_VERSION) throw BackupFormatException(BackupFormatException.Reason.UNSUPPORTED_VERSION)
            val listArray = root.getJSONArray("lists")
            if (listArray.length() > MAX_LISTS) throw BackupFormatException(BackupFormatException.Reason.LIMIT_EXCEEDED)
            var itemCount = 0
            val lists = ArrayList<BackupList>(listArray.length())
            for (listIndex in 0 until listArray.length()) {
                val sourceList = listArray.getJSONObject(listIndex)
                val sourceItems = sourceList.getJSONArray("items")
                itemCount += sourceItems.length()
                if (itemCount > MAX_ITEMS) throw BackupFormatException(BackupFormatException.Reason.LIMIT_EXCEEDED)
                val items = ArrayList<BackupItem>(sourceItems.length())
                for (itemIndex in 0 until sourceItems.length()) {
                    val item = sourceItems.getJSONObject(itemIndex)
                    items += BackupItem(
                        title = item.getString("title"),
                        type = item.getString("type"),
                        length = item.getInt("length"),
                        genres = item.getJSONArray("genres").toStringList(),
                        keywords = item.getJSONArray("keywords").toStringList(),
                        category = item.getString("category"),
                        coverUri = item.nullableString("coverUri"),
                        coverMimeType = item.nullableString("coverMimeType"),
                        coverBase64 = item.nullableString("coverBase64"),
                        review = item.getString("review"),
                        rating = if (item.isNull("rating")) null else item.getInt("rating"),
                        priority = if (!item.has("priority") || item.isNull("priority")) null else item.getInt("priority"),
                        watchedEpisodes = item.getInt("watchedEpisodes"),
                        watchStartedAt = if (!item.has("watchStartedAt") || item.isNull("watchStartedAt")) null else item.getLong("watchStartedAt"),
                        watchEndedAt = if (!item.has("watchEndedAt") || item.isNull("watchEndedAt")) null else item.getLong("watchEndedAt"),
                        createdAt = item.getLong("createdAt"),
                        updatedAt = item.getLong("updatedAt"),
                    )
                }
                lists += BackupList(
                    title = sourceList.getString("title"),
                    position = sourceList.getInt("position"),
                    archived = sourceList.getBoolean("archived"),
                    categories = if (sourceList.has("categories")) sourceList.getJSONArray("categories").let { categories -> List(categories.length()) { index -> categories.getJSONObject(index).let { category -> BackupCategory(category.getString("id"), category.getString("name"), runCatching { CategoryColor.valueOf(category.getString("color")) }.getOrElse { throw BackupFormatException(BackupFormatException.Reason.INVALID_DATA) }, category.getLong("position"), category.getLong("createdAt")) } } } else emptyList(),
                    items = items,
                )
            }
            return BackupPayload(
                exportedAt = root.getLong("exportedAt"),
                appVersion = root.getString("appVersion"),
                lists = lists,
            ).also(::validate)
        } catch (error: BackupFormatException) {
            throw error
        } catch (_: JSONException) {
            throw BackupFormatException(BackupFormatException.Reason.MALFORMED)
        }
    }

    fun validate(payload: BackupPayload) {
        if (payload.lists.size > MAX_LISTS || payload.lists.sumOf { it.items.size } > MAX_ITEMS) limit()
        if (payload.exportedAt < 0 || payload.appVersion.length !in 1..40) invalid()
        payload.lists.forEach { list ->
            if (list.title.isBlank() || list.title.length > 80 || list.position < 0) invalid()
            if (list.categories.any { it.id.isBlank() || it.name.trim().isEmpty() || it.name.length > 80 || it.position < 0 || it.createdAt < 0 } || list.categories.map { it.name.trim().lowercase() }.distinct().size != list.categories.size) invalid()
            list.items.forEach { item ->
                if (item.title.isBlank() || item.title.length > 250 || item.type.isBlank() || item.type.length > 80) invalid()
                if (item.length <= 0 || item.genres.size > 100 || item.keywords.size > 100) invalid()
                if ((item.genres + item.keywords).any { it.isBlank() || it.length > 80 }) invalid()
                if (item.review.length > 500 || item.rating != null && item.rating !in 1..10 || item.priority != null && item.priority !in 1..4) invalid()
                if (item.watchedEpisodes < 0 || item.watchedEpisodes > item.length || item.createdAt < 0 || item.updatedAt < 0) invalid()
                if (item.watchStartedAt != null && item.watchStartedAt < 0 || item.watchEndedAt != null && item.watchEndedAt < 0 || item.watchStartedAt != null && item.watchEndedAt != null && item.watchEndedAt < item.watchStartedAt) invalid()
                if (item.coverUri != null && !item.coverUri.startsWith("http://") && !item.coverUri.startsWith("https://")) invalid()
                if ((item.coverMimeType == null) != (item.coverBase64 == null)) invalid()
                item.coverBase64?.let {
                    if (it.length > ((MAX_EMBEDDED_COVER_BYTES + 2L) / 3L * 4L)) limit()
                    if (!it.matches(BASE64_PATTERN)) invalid()
                }
                if (item.coverMimeType != null && !item.coverMimeType.startsWith("image/")) invalid()
            }
        }
    }

    private fun JSONArray.toStringList(): List<String> = List(length()) { getString(it) }
    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key)
    private fun invalid(): Nothing = throw BackupFormatException(BackupFormatException.Reason.INVALID_DATA)
    private fun limit(): Nothing = throw BackupFormatException(BackupFormatException.Reason.LIMIT_EXCEEDED)
    private val BASE64_PATTERN = Regex("^[A-Za-z0-9+/]*={0,2}$")
}
