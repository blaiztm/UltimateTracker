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
import com.example.ultimatetracker.search.MediaSearchEngine
import com.example.ultimatetracker.search.CatalogSearchRanker

data class HomeUiState(
    val items: List<MediaItem> = emptyList(),
    val category: WatchCategory? = null,
    val query: String = "",
    val availableGenres: List<String> = emptyList(),
    val availableKeywords: List<String> = emptyList(),
    val selectedGenres: Set<String> = emptySet(),
    val selectedKeywords: Set<String> = emptySet(),
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
    val review: String = "",
    val rating: String = "",
    val watchedEpisodes: String = "",
    val createdAt: Long = 0,
)

data class CatalogUiState(
    val query: String = "",
    val results: List<CatalogItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val keyMissing: Boolean = false,
)

enum class FormValidationError { TITLE, DURATION, EPISODES, WATCHED_EPISODES, RATING }

fun MediaFormState.validationError(): FormValidationError? = when {
    title.isBlank() -> FormValidationError.TITLE
    length.toIntOrNull() == null || length.toInt() <= 0 ->
        if (type == BuiltInMediaTypes.MOVIE) FormValidationError.DURATION else FormValidationError.EPISODES
    category == WatchCategory.WATCHING && type != BuiltInMediaTypes.MOVIE && (watchedEpisodes.toIntOrNull() == null || watchedEpisodes.toInt() !in 0..length.toInt()) -> FormValidationError.WATCHED_EPISODES
    rating.isNotBlank() && rating.toIntOrNull() !in 1..10 -> FormValidationError.RATING
    else -> null
}

class MediaViewModel(private val repository: MediaRepository, private val tmdbClient: TmdbClient, private val mediaSearchEngine: MediaSearchEngine = MediaSearchEngine(), private val catalogSearchRanker: CatalogSearchRanker = CatalogSearchRanker()) : ViewModel() {
    private val selectedCategory = MutableStateFlow<WatchCategory?>(null)
    private val query = MutableStateFlow("")
    private val selectedGenres = MutableStateFlow<Set<String>>(emptySet())
    private val selectedKeywords = MutableStateFlow<Set<String>>(emptySet())
    private val _catalogState = MutableStateFlow(CatalogUiState(keyMissing = !tmdbClient.isConfigured))
    val catalogState = _catalogState
    private val _catalogDraft = MutableStateFlow<CatalogItem?>(null)
    val catalogDraft = _catalogDraft
    private val _tmdbToken = MutableStateFlow(tmdbClient.currentToken())
    val tmdbToken = _tmdbToken
    private var catalogJob: Job? = null

    val mediaTypes = repository.observeCustomTypes()
        .map { custom -> (BuiltInMediaTypes.entries + custom).distinct() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BuiltInMediaTypes.entries)

    private val filters = combine(selectedCategory, query, selectedGenres, selectedKeywords) { category, text, genres, keywords ->
        FilterState(category, text, genres, keywords)
    }

    val homeState = combine(repository.observeAll(), filters) { items, filters ->
        val category = filters.category
        val text = filters.query
        val genres = filters.genres
        val keywords = filters.keywords
        val filteredItems = items.filter { item ->
            (category == null || item.category == category) &&
                genres.all { selected -> item.genres.any { it.equals(selected, ignoreCase = true) } } &&
                keywords.all { selected -> item.keywords.any { it.equals(selected, ignoreCase = true) } }
        }
        HomeUiState(
            items = mediaSearchEngine.search(text, filteredItems).map { it.value },
            category = category,
            query = text,
            availableGenres = items.flatMap { it.genres }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() },
            availableKeywords = items.flatMap { it.keywords }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() },
            selectedGenres = genres,
            selectedKeywords = keywords,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setCategory(value: WatchCategory?) { selectedCategory.value = value }
    fun setQuery(value: String) { query.value = value }
    fun toggleGenre(value: String) { selectedGenres.value = selectedGenres.value.toggle(value) }
    fun toggleKeyword(value: String) { selectedKeywords.value = selectedKeywords.value.toggle(value) }
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
                .onSuccess { _catalogState.value = _catalogState.value.copy(results = catalogSearchRanker.rank(query, it), isLoading = false) }
                .onFailure { _catalogState.value = _catalogState.value.copy(results = emptyList(), isLoading = false, hasError = true) }
        }
    }

    fun selectCatalogItem(item: CatalogItem, language: String, onSelected: () -> Unit) = viewModelScope.launch {
        _catalogDraft.value = runCatching { tmdbClient.loadDetails(item, language) }.getOrDefault(item)
        onSelected()
    }
    fun consumeCatalogDraft() { _catalogDraft.value = null }
    fun saveTmdbToken(value: String) {
        tmdbClient.updateToken(value)
        _tmdbToken.value = tmdbClient.currentToken()
        _catalogState.value = _catalogState.value.copy(keyMissing = !tmdbClient.isConfigured, hasError = false)
    }
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
                    review = form.review.trim(),
                    rating = form.rating.toIntOrNull(),
                    watchedEpisodes = if (form.category == WatchCategory.WATCHING && form.type != BuiltInMediaTypes.MOVIE) form.watchedEpisodes.toInt() else 0,
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

private data class FilterState(val category: WatchCategory?, val query: String, val genres: Set<String>, val keywords: Set<String>)
private fun Set<String>.toggle(value: String) = if (value in this) this - value else this + value

class MediaViewModelFactory(private val repository: MediaRepository, private val tmdbClient: TmdbClient) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MediaViewModel(repository, tmdbClient) as T
}
