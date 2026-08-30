package com.example.ultimatetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ultimatetracker.data.model.MediaItem
import com.example.ultimatetracker.data.model.MediaType
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val items: List<MediaItem> = emptyList(),
    val category: WatchCategory? = null,
    val query: String = "",
)

data class MediaFormState(
    val id: Long = 0,
    val title: String = "",
    val type: MediaType = MediaType.MOVIE,
    val length: String = "",
    val genres: String = "",
    val keywords: String = "",
    val category: WatchCategory = WatchCategory.PLANNED,
    val coverUri: String? = null,
    val createdAt: Long = 0,
)

fun MediaFormState.validationError(): String? = when {
    title.isBlank() -> "Введите название"
    length.toIntOrNull() == null || length.toInt() <= 0 ->
        if (type == MediaType.MOVIE) "Укажите длительность в минутах" else "Укажите количество серий"
    else -> null
}

class MediaViewModel(private val repository: MediaRepository) : ViewModel() {
    private val selectedCategory = MutableStateFlow<WatchCategory?>(null)
    private val query = MutableStateFlow("")

    val homeState = combine(repository.observeAll(), selectedCategory, query) { items, category, text ->
        val normalized = text.trim().lowercase()
        HomeUiState(
            items = items.filter { item ->
                (category == null || item.category == category) &&
                    (normalized.isEmpty() || item.title.lowercase().contains(normalized) ||
                        item.keywords.any { it.lowercase().contains(normalized) })
            },
            category = category,
            query = text,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setCategory(value: WatchCategory?) { selectedCategory.value = value }
    fun setQuery(value: String) { query.value = value }
    fun observeItem(id: Long) = repository.observeById(id)

    fun save(form: MediaFormState, onSaved: () -> Unit) {
        if (form.validationError() != null) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.save(
                MediaItem(
                    id = form.id,
                    title = form.title.trim(),
                    type = form.type,
                    length = form.length.toInt(),
                    genres = form.genres.toTags(),
                    keywords = form.keywords.toTags(),
                    category = form.category,
                    coverUri = form.coverUri,
                    createdAt = form.createdAt.takeIf { it > 0 } ?: now,
                    updatedAt = now,
                )
            )
            onSaved()
        }
    }

    fun delete(item: MediaItem, onDeleted: () -> Unit) = viewModelScope.launch {
        repository.delete(item)
        onDeleted()
    }
}

private fun String.toTags() = split(',', ';').map(String::trim).filter(String::isNotBlank).distinct()

class MediaViewModelFactory(private val repository: MediaRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MediaViewModel(repository) as T
}
