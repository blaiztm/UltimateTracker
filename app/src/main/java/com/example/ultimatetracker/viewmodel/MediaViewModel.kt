package com.example.ultimatetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ultimatetracker.data.model.MediaItem
import com.example.ultimatetracker.data.model.BuiltInMediaTypes
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.data.repository.MediaRepository
import com.example.ultimatetracker.data.remote.CatalogItem
import com.example.ultimatetracker.data.remote.TmdbClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

data class HomeUiState(
    val items: List<MediaItem> = emptyList(),
    val category: WatchCategory? = null,
    val query: String = "",
    val availableTags: List<String> = emptyList(),
    val selectedTags: Set<String> = emptySet(),
)

data class MediaFormState(
    val id: Long = 0,
    val title: String = "",
    val type: String = BuiltInMediaTypes.MOVIE,
    val length: String = "",
    val genres: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val category: WatchCategory = WatchCategory.PLANNED,
    val coverUri: String? = null,
    val createdAt: Long = 0,
)

data class CatalogUiState(
    val query: String = "",
    val results: List<CatalogItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val keyMissing: Boolean = false,
)

enum class FormValidationError { TITLE, DURATION, EPISODES }

fun MediaFormState.validationError(): FormValidationError? = when {
    title.isBlank() -> FormValidationError.TITLE
    length.toIntOrNull() == null || length.toInt() <= 0 ->
        if (type == BuiltInMediaTypes.MOVIE) FormValidationError.DURATION else FormValidationError.EPISODES
    else -> null
}

class MediaViewModel(private val repository: MediaRepository, private val tmdbClient: TmdbClient) : ViewModel() {
    private val selectedCategory = MutableStateFlow<WatchCategory?>(null)
    private val query = MutableStateFlow("")
    private val selectedTags = MutableStateFlow<Set<String>>(emptySet())
    private val _catalogState = MutableStateFlow(CatalogUiState(keyMissing = !tmdbClient.isConfigured))
    val catalogState = _catalogState
    private val _catalogDraft = MutableStateFlow<CatalogItem?>(null)
    val catalogDraft = _catalogDraft
    private var catalogJob: Job? = null

    val mediaTypes = repository.observeCustomTypes()
        .map { custom -> (BuiltInMediaTypes.entries + custom).distinct() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BuiltInMediaTypes.entries)

    private val filters = combine(selectedCategory, query, selectedTags) { category, text, tags -> Triple(category, text, tags) }

    val homeState = combine(repository.observeAll(), filters) { items, (category, text, tags) ->
        val normalized = text.trim().lowercase()
        val availableTags = items.flatMap { it.genres + it.keywords }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
        HomeUiState(
            items = items.filter { item ->
                (category == null || item.category == category) &&
                    (normalized.isEmpty() || item.title.lowercase().contains(normalized) ||
                        item.keywords.any { it.lowercase().contains(normalized) }) &&
                    tags.all { selected -> (item.genres + item.keywords).any { it.equals(selected, ignoreCase = true) } }
            },
            category = category,
            query = text,
            availableTags = availableTags,
            selectedTags = tags,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setCategory(value: WatchCategory?) { selectedCategory.value = value }
    fun setQuery(value: String) { query.value = value }
    fun toggleTag(value: String) {
        selectedTags.value = selectedTags.value.let { if (value in it) it - value else it + value }
    }
    fun clearTags() { selectedTags.value = emptySet() }
    fun observeItem(id: Long) = repository.observeById(id)
    fun addCustomType(name: String, onAdded: (String) -> Unit) {
        val normalized = name.trim()
        if (normalized.isEmpty()) return
        viewModelScope.launch {
            repository.addCustomType(normalized)
            onAdded(normalized)
        }
    }

    fun searchCatalog(query: String, language: String) {
        _catalogState.value = _catalogState.value.copy(query = query, hasError = false)
        catalogJob?.cancel()
        if (query.isBlank() || !tmdbClient.isConfigured) {
            _catalogState.value = _catalogState.value.copy(results = emptyList(), isLoading = false)
            return
        }
        catalogJob = viewModelScope.launch {
            delay(350)
            _catalogState.value = _catalogState.value.copy(isLoading = true)
            runCatching { tmdbClient.search(query, language) }
                .onSuccess { _catalogState.value = _catalogState.value.copy(results = it, isLoading = false) }
                .onFailure { _catalogState.value = _catalogState.value.copy(results = emptyList(), isLoading = false, hasError = true) }
        }
    }

    fun selectCatalogItem(item: CatalogItem) { _catalogDraft.value = item }
    fun consumeCatalogDraft() { _catalogDraft.value = null }
    fun findCover(title: String, language: String, onResult: (String?) -> Unit) = viewModelScope.launch {
        val cover = runCatching { tmdbClient.search(title, language).firstOrNull()?.coverUri }.getOrNull()
        onResult(cover)
    }

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
                    genres = form.genres,
                    keywords = form.keywords,
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

    fun updateCategory(item: MediaItem, category: WatchCategory) = viewModelScope.launch {
        repository.save(item.copy(category = category, updatedAt = System.currentTimeMillis()))
    }
}

class MediaViewModelFactory(private val repository: MediaRepository, private val tmdbClient: TmdbClient) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MediaViewModel(repository, tmdbClient) as T
}
