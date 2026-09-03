package com.example.ultimatetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ultimatetracker.data.model.MediaItem
import com.example.ultimatetracker.data.model.BuiltInMediaTypes
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.data.model.CategoryColor
import com.example.ultimatetracker.data.model.CategoryRef
import com.example.ultimatetracker.data.local.CategoryEntity
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
import kotlin.time.Duration.Companion.milliseconds
import java.util.Locale
import com.example.ultimatetracker.search.MediaSearchEngine
import com.example.ultimatetracker.search.CatalogSearchRanker

data class HomeUiState(
    val items: List<MediaItem> = emptyList(),
    val category: String? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val query: String = "",
    val availableGenres: List<String> = emptyList(),
    val availableKeywords: List<String> = emptyList(),
    val selectedGenres: Set<String> = emptySet(),
    val selectedKeywords: Set<String> = emptySet(),
    val availableTypes: List<String> = emptyList(),
    val selectedTypes: Set<String> = emptySet(),
    val sortMode: SortMode = SortMode.TITLE,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
)

enum class SortMode { TITLE, RATING, PRIORITY, DURATION }
enum class SortDirection { ASCENDING, DESCENDING }

data class MediaFormState(
    val id: Long = 0,
    val title: String = "",
    val type: String = BuiltInMediaTypes.MOVIE,
    val length: String = "",
    val genres: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val category: String = WatchCategory.PLANNED.name,
    val coverUri: String? = null,
    val review: String = "",
    val rating: String = "",
    val priority: String = "",
    val watchedEpisodes: String = "",
    val watchStartedAt: Long? = null,
    val watchEndedAt: Long? = null,
    val createdAt: Long = 0,
)

data class CatalogUiState(
    val query: String = "",
    val results: List<CatalogSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val keyMissing: Boolean = false,
)

data class CatalogSearchResult(
    val item: CatalogItem,
    val isInLibrary: Boolean,
)

data class TagVocabulary(
    val genres: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
)

data class StatisticsUiState(
    val totalTitles: Int = 0,
    val completedCount: Int = 0,
    val categoryCounts: Map<String, Int> = emptyMap(),
    val typeCounts: Map<String, Int> = emptyMap(),
    val averageRating: Double? = null,
    val ratingDistribution: Map<Int, Int> = emptyMap(),
    val movieMinutes: Int = 0,
    val totalEpisodes: Int = 0,
    val watchedEpisodes: Int = 0,
    val priorityDistribution: Map<Int, Int> = emptyMap(),
    val genres: List<Pair<String, Int>> = emptyList(),
    val keywords: List<Pair<String, Int>> = emptyList(),
)

private data class CatalogSearchState(
    val query: String = "",
    val results: List<CatalogItem> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val keyMissing: Boolean = false,
)

enum class FormValidationError { TITLE, DURATION, EPISODES, WATCHED_EPISODES, RATING, PRIORITY, DATE_RANGE }

fun MediaFormState.validationError(): FormValidationError? = when {
    title.isBlank() -> FormValidationError.TITLE
    length.toIntOrNull() == null || length.toInt() <= 0 ->
        if (type == BuiltInMediaTypes.MOVIE) FormValidationError.DURATION else FormValidationError.EPISODES
    CategoryRef.builtIn(category) in setOf(WatchCategory.WATCHING, WatchCategory.ON_HOLD) && type != BuiltInMediaTypes.MOVIE && (watchedEpisodes.toIntOrNull() == null || watchedEpisodes.toInt() !in 0..length.toInt()) -> FormValidationError.WATCHED_EPISODES
    rating.isNotBlank() && rating.toIntOrNull() !in 1..10 -> FormValidationError.RATING
    priority.isNotBlank() && priority.toIntOrNull() !in 1..4 -> FormValidationError.PRIORITY
    watchStartedAt != null && watchEndedAt != null && watchEndedAt < watchStartedAt -> FormValidationError.DATE_RANGE
    else -> null
}

class MediaViewModel(private val repository: MediaRepository, private val tmdbClient: TmdbClient, private val mediaSearchEngine: MediaSearchEngine = MediaSearchEngine(), private val catalogSearchRanker: CatalogSearchRanker = CatalogSearchRanker()) : ViewModel() {
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")
    private val selectedGenres = MutableStateFlow<Set<String>>(emptySet())
    private val selectedKeywords = MutableStateFlow<Set<String>>(emptySet())
    private val selectedTypes = MutableStateFlow<Set<String>>(emptySet())
    private val sortMode = MutableStateFlow(SortMode.TITLE)
    private val sortDirection = MutableStateFlow(SortDirection.ASCENDING)
    private val _catalogState = MutableStateFlow(CatalogSearchState(keyMissing = !tmdbClient.isConfigured))
    val libraryTitleMembership = repository.observeAll()
        .map { items -> items.map { it.title.normalizedIdentity() }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val catalogState = combine(_catalogState, libraryTitleMembership) { state, libraryTitles ->
        CatalogUiState(
            query = state.query,
            results = state.results.map { CatalogSearchResult(it, it.title.normalizedIdentity() in libraryTitles) },
            isLoading = state.isLoading,
            hasError = state.hasError,
            keyMissing = state.keyMissing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState(keyMissing = !tmdbClient.isConfigured))
    private val _catalogDraft = MutableStateFlow<CatalogItem?>(null)
    val catalogDraft = _catalogDraft
    private val _tmdbToken = MutableStateFlow(tmdbClient.currentToken())
    val tmdbToken = _tmdbToken
    private var catalogJob: Job? = null

    val mediaTypes = repository.observeCustomTypes()
        .map { custom -> (BuiltInMediaTypes.entries + custom).distinct() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BuiltInMediaTypes.entries)

    val categories = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tagVocabulary = repository.observeAll()
        .map { items ->
            TagVocabulary(
                genres = items.flatMap { it.genres }.normalizedDistinct(),
                keywords = items.flatMap { it.keywords }.normalizedDistinct(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagVocabulary())

    val statistics = repository.observeAll().map(::calculateStatistics)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    @Suppress("UNCHECKED_CAST")
    private val filters = combine(
        selectedCategory,
        query,
        selectedGenres,
        selectedKeywords,
        selectedTypes,
        sortMode,
        sortDirection,
    ) { args: Array<Any?> ->
        FilterState(
            category = args[0] as String?,
            query = args[1] as String,
            genres = args[2] as Set<String>,
            keywords = args[3] as Set<String>,
            types = args[4] as Set<String>,
            sort = args[5] as SortMode,
            direction = args[6] as SortDirection,
        )
    }

    val homeState = combine(repository.observeAll(), repository.observeCategories(), filters) { items, categories, filters ->
        val category = filters.category
        val text = filters.query
        val genres = filters.genres
        val keywords = filters.keywords
        val types = filters.types
        val filteredItems = items.filter { item ->
            (category == null || item.category == category) &&
                (types.isEmpty() || item.type in types) &&
                genres.all { selected -> item.genres.any { it.equals(selected, ignoreCase = true) } } &&
                keywords.all { selected -> item.keywords.any { it.equals(selected, ignoreCase = true) } }
        }
        HomeUiState(
            items = mediaSearchEngine.search(text, filteredItems).map { it.value }.sortedWith(filters.sort.comparator(filters.direction)),
            category = category,
            categories = categories,
            query = text,
            availableGenres = items.flatMap { it.genres }.normalizedDistinct(),
            availableKeywords = items.flatMap { it.keywords }.normalizedDistinct(),
            selectedGenres = genres,
            selectedKeywords = keywords,
            availableTypes = items.map { it.type }.distinct().sortedBy { it.lowercase() },
            selectedTypes = types,
            sortMode = filters.sort,
            sortDirection = filters.direction,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setCategory(value: String?) {
        val planned = value?.let(CategoryRef::builtIn) == WatchCategory.PLANNED
        if (planned && sortMode.value == SortMode.RATING) sortMode.value = SortMode.PRIORITY
        if (!planned && sortMode.value == SortMode.PRIORITY) sortMode.value = SortMode.RATING
        selectedCategory.value = value
    }
    fun setQuery(value: String) { query.value = value }
    fun toggleGenre(value: String) { selectedGenres.value = selectedGenres.value.toggle(value) }
    fun toggleKeyword(value: String) { selectedKeywords.value = selectedKeywords.value.toggle(value) }
    fun toggleType(value: String) { selectedTypes.value = selectedTypes.value.toggle(value) }
    fun setSortMode(value: SortMode) { sortMode.value = value }
    fun toggleSortDirection() { sortDirection.value = if (sortDirection.value == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING }
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
            delay(350.milliseconds)
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
                    genres = form.genres.normalizedDistinct(),
                    keywords = form.keywords.normalizedDistinct(),
                    category = form.category,
                    coverUri = form.coverUri,
                    review = form.review.trim(),
                    rating = form.rating.toIntOrNull(),
                    priority = form.priority.toIntOrNull(),
                    watchedEpisodes = if (CategoryRef.builtIn(form.category) in setOf(WatchCategory.WATCHING, WatchCategory.ON_HOLD) && form.type != BuiltInMediaTypes.MOVIE) form.watchedEpisodes.toInt() else 0,
                    watchStartedAt = form.watchStartedAt,
                    watchEndedAt = form.watchEndedAt,
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

    fun updateCategory(item: MediaItem, category: String) = viewModelScope.launch {
        repository.save(item.copy(category = category, updatedAt = System.currentTimeMillis()))
    }

    @Suppress("unused")
    fun addCategory(name: String, color: CategoryColor) = viewModelScope.launch { repository.addCategory(name, color) }
    @Suppress("unused")
    fun deleteCategory(id: String, replacement: String) = viewModelScope.launch { repository.deleteCategory(id, replacement) }
}

private data class FilterState(val category: String?, val query: String, val genres: Set<String>, val keywords: Set<String>, val types: Set<String>, val sort: SortMode, val direction: SortDirection)
private fun Set<String>.toggle(value: String) = if (value in this) this - value else this + value
private fun String.normalizedIdentity() = trim().lowercase(Locale.ROOT)
private fun List<String>.normalizedDistinct() = asSequence()
    .map(String::trim)
    .filter(String::isNotEmpty)
    .distinctBy(String::normalizedIdentity)
    .sortedBy(String::normalizedIdentity)
    .toList()
private fun calculateStatistics(items: List<MediaItem>): StatisticsUiState {
    val rated = items.mapNotNull { it.rating }
    val progressing = items.filter { CategoryRef.builtIn(it.category) in setOf(WatchCategory.WATCHING, WatchCategory.ON_HOLD) && it.type != BuiltInMediaTypes.MOVIE }
    fun frequent(values: List<String>) = values.groupingBy { it.trim() }.eachCount().entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).take(5).map { it.key to it.value }
    return StatisticsUiState(
        totalTitles = items.size,
        completedCount = items.count { CategoryRef.builtIn(it.category) == WatchCategory.COMPLETED },
        categoryCounts = items.groupingBy { it.category }.eachCount(),
        typeCounts = items.groupingBy { it.type }.eachCount(),
        averageRating = rated.takeIf { it.isNotEmpty() }?.average(),
        ratingDistribution = rated.groupingBy { it }.eachCount(),
        movieMinutes = items.filter { it.type == BuiltInMediaTypes.MOVIE }.sumOf { it.length },
        totalEpisodes = progressing.sumOf { it.length }, watchedEpisodes = progressing.sumOf { it.watchedEpisodes },
        priorityDistribution = items.filter { CategoryRef.builtIn(it.category) == WatchCategory.PLANNED }.mapNotNull { it.priority }.groupingBy { it }.eachCount(),
        genres = frequent(items.flatMap { it.genres }), keywords = frequent(items.flatMap { it.keywords }),
    )
}
private fun SortMode.comparator(direction: SortDirection): Comparator<MediaItem> {
    val titleTieBreaker = compareBy<MediaItem> { it.title.lowercase() }.thenBy { it.id }
    return when (this) {
        SortMode.TITLE -> if (direction == SortDirection.ASCENDING) titleTieBreaker else compareByDescending<MediaItem> { it.title.lowercase() }.then(titleTieBreaker)
        SortMode.RATING -> compareBy<MediaItem> { it.rating == null }.then(if (direction == SortDirection.ASCENDING) compareBy { it.rating } else compareByDescending { it.rating }).then(titleTieBreaker)
        SortMode.PRIORITY -> compareBy<MediaItem> { it.priority == null }.then(if (direction == SortDirection.ASCENDING) compareBy { it.priority } else compareByDescending { it.priority }).then(titleTieBreaker)
        SortMode.DURATION -> (if (direction == SortDirection.ASCENDING) compareBy<MediaItem> { it.length } else compareByDescending { it.length }).then(titleTieBreaker)
    }
}

class MediaViewModelFactory(private val repository: MediaRepository, private val tmdbClient: TmdbClient) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MediaViewModel(repository, tmdbClient) as T
}
