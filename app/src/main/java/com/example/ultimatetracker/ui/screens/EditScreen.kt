package com.example.ultimatetracker.ui.screens

import android.content.Intent
import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import java.text.DateFormat
import java.util.Calendar
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ultimatetracker.data.model.BuiltInMediaTypes
import com.example.ultimatetracker.data.model.CategoryRef
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.data.model.CategoryColor
import com.example.ultimatetracker.data.local.CategoryEntity
import com.example.ultimatetracker.ui.components.MediaCover
import com.example.ultimatetracker.viewmodel.MediaFormState
import com.example.ultimatetracker.viewmodel.MediaViewModel
import com.example.ultimatetracker.viewmodel.validationError
import com.example.ultimatetracker.viewmodel.FormValidationError
import com.example.ultimatetracker.R
import com.example.ultimatetracker.ui.categoryLabel
import com.example.ultimatetracker.ui.mediaTypeLabel

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun EditScreen(viewModel: MediaViewModel, itemId: Long, onBack: () -> Unit) {
    val existing by viewModel.observeItem(itemId).collectAsStateWithLifecycle(initialValue = null)
    val mediaTypes by viewModel.mediaTypes.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val tagVocabulary by viewModel.tagVocabulary.collectAsStateWithLifecycle()
    val catalogDraft by viewModel.catalogDraft.collectAsStateWithLifecycle()
    var form by remember(itemId) { mutableStateOf(MediaFormState(id = itemId)) }
    var initialized by remember(itemId) { mutableStateOf(itemId == 0L) }
    var showErrors by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val language = LocalConfiguration.current.locales[0].language
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            form = form.copy(coverUri = it.toString())
        }
    }
    LaunchedEffect(existing, initialized) {
        val item = existing
        if (!initialized && item != null) {
            form = MediaFormState(
                id = item.id, title = item.title, type = item.type, length = item.length.toString(),
                genres = item.genres, keywords = item.keywords, category = item.category, coverUri = item.coverUri,
                review = item.review, rating = item.rating?.toString().orEmpty(), priority = item.priority?.toString().orEmpty(), watchedEpisodes = item.watchedEpisodes.toString(), watchStartedAt = item.watchStartedAt, watchEndedAt = item.watchEndedAt, createdAt = item.createdAt,
            )
            initialized = true
        }
    }
    LaunchedEffect(catalogDraft, itemId) {
        if (itemId == 0L) catalogDraft?.let { suggestion ->
            form = form.copy(
                title = suggestion.title,
                type = suggestion.mediaType,
                coverUri = suggestion.coverUri,
                length = suggestion.length?.toString().orEmpty(),
                genres = suggestion.genres,
            )
            viewModel.consumeCatalogDraft()
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(if (itemId == 0L) R.string.new_item else R.string.editing)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MediaCover(form.coverUri, form.title, Modifier.fillMaxWidth().height(220.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.choose_cover)) }
                if (form.coverUri != null) OutlinedButton(onClick = { form = form.copy(coverUri = null) }) { Text(stringResource(R.string.remove)) }
            }
            OutlinedButton(
                onClick = { viewModel.findCover(form.title, language) { uri -> if (uri != null) form = form.copy(coverUri = uri) } },
                enabled = form.title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.auto_cover)) }
            OutlinedTextField(
                value = form.title,
                onValueChange = { form = form.copy(title = it) },
                label = { Text(stringResource(R.string.title_required)) },
                isError = showErrors && form.title.isBlank(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            TypeMenu(
                selected = form.type,
                options = mediaTypes,
                onSelect = { form = form.copy(type = it) },
                onAdd = { name -> viewModel.addCustomType(name) { form = form.copy(type = it) } },
            )
            OutlinedTextField(
                value = form.length,
                onValueChange = { value -> if (value.all(Char::isDigit)) form = form.copy(length = value) },
                label = { Text(stringResource(if (form.type == BuiltInMediaTypes.MOVIE) R.string.duration_required else R.string.episodes_required)) },
                isError = showErrors && (form.length.toIntOrNull() ?: 0) <= 0,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            TagEditor(stringResource(R.string.genres), form.genres, tagVocabulary.genres, { form = form.copy(genres = form.genres + it) }, { form = form.copy(genres = form.genres - it) })
            TagEditor(stringResource(R.string.keywords), form.keywords, tagVocabulary.keywords, { form = form.copy(keywords = form.keywords + it) }, { form = form.copy(keywords = form.keywords - it) })
            OutlinedTextField(
                value = form.review,
                onValueChange = { if (it.length <= 500) form = form.copy(review = it) },
                label = { Text(stringResource(R.string.review)) },
                supportingText = { Text(stringResource(R.string.review_length, form.review.length)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
            )
            if (CategoryRef.builtIn(form.category) == WatchCategory.PLANNED) {
                OutlinedTextField(
                    value = form.priority,
                    onValueChange = { value -> if (value.length <= 1 && value.all(Char::isDigit)) form = form.copy(priority = value) },
                    label = { Text(stringResource(R.string.priority_optional)) },
                    isError = showErrors && form.priority.isNotBlank() && form.priority.toIntOrNull() !in 1..4,
                    supportingText = { if (showErrors && form.priority.isNotBlank() && form.priority.toIntOrNull() !in 1..4) Text(stringResource(R.string.error_priority)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
            } else {
                OutlinedTextField(
                    value = form.rating,
                    onValueChange = { value -> if (value.length <= 2 && value.all(Char::isDigit)) form = form.copy(rating = value) },
                    label = { Text(stringResource(R.string.rating_optional)) },
                    isError = showErrors && form.rating.isNotBlank() && form.rating.toIntOrNull() !in 1..10,
                    supportingText = { if (showErrors && form.rating.isNotBlank() && form.rating.toIntOrNull() !in 1..10) Text(stringResource(R.string.error_rating)) },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
            }
            if (CategoryRef.builtIn(form.category) in setOf(WatchCategory.WATCHING, WatchCategory.ON_HOLD) && form.type != BuiltInMediaTypes.MOVIE) {
                OutlinedTextField(
                    value = form.watchedEpisodes,
                    onValueChange = { value -> if (value.all(Char::isDigit)) form = form.copy(watchedEpisodes = value) },
                    label = { Text(stringResource(R.string.watched_episodes)) },
                    isError = showErrors && form.validationError() == FormValidationError.WATCHED_EPISODES,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
            }
            DateSelector(stringResource(R.string.watch_start_date), form.watchStartedAt) { form = form.copy(watchStartedAt = it) }
            DateSelector(stringResource(R.string.watch_end_date), form.watchEndedAt) { form = form.copy(watchEndedAt = it) }
            CategoryMenu(form.category, categories, onSelect = { form = form.copy(category = it) }, onAdd = { name, color -> viewModel.addCategory(name, color) })
            if (showErrors) form.validationError()?.let { error ->
                Text(stringResource(when (error) { FormValidationError.TITLE -> R.string.error_title; FormValidationError.DURATION -> R.string.error_duration; FormValidationError.EPISODES -> R.string.error_episodes; FormValidationError.WATCHED_EPISODES -> R.string.error_watched_episodes; FormValidationError.RATING -> R.string.error_rating; FormValidationError.PRIORITY -> R.string.error_priority; FormValidationError.DATE_RANGE -> R.string.error_date_range }), color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    showErrors = true
                    if (form.validationError() == null) viewModel.save(form, onBack)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.save)) }
        }
    }
}

@Composable
private fun DateSelector(label: String, value: Long?, onValueChange: (Long?) -> Unit) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                val calendar = Calendar.getInstance().apply { value?.let { timeInMillis = it } }
                DatePickerDialog(context, { _, year, month, day ->
                    onValueChange(Calendar.getInstance().apply { set(year, month, day, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis)
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
            },
            modifier = Modifier.weight(1f),
        ) { Text("$label: ${value?.let { DateFormat.getDateInstance().format(it) } ?: stringResource(R.string.not_specified)}") }
        if (value != null) TextButton(onClick = { onValueChange(null) }) { Text(stringResource(R.string.clear)) }
    }
}

@Composable
private fun TypeMenu(selected: String, options: List<String>, onSelect: (String) -> Unit, onAdd: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var newType by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                focusManager.clearFocus(force = true)
                keyboard?.hide()
                expanded = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.type_format, mediaTypeLabel(selected))) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(mediaTypeLabel(option)) }, onClick = { expanded = false; onSelect(option) }) }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_custom_type)) },
                leadingIcon = { Icon(Icons.Default.Add, null) },
                onClick = { expanded = false; showDialog = true },
            )
        }
    }
    if (showDialog) AlertDialog(
        onDismissRequest = { showDialog = false },
        title = { Text(stringResource(R.string.new_type)) },
        text = { OutlinedTextField(newType, { newType = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { if (newType.isNotBlank()) { onAdd(newType); newType = ""; showDialog = false } }) { Text(stringResource(R.string.add)) }
        },
        dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun CategoryMenu(selected: String, custom: List<CategoryEntity>, onSelect: (String) -> Unit, onAdd: (String, CategoryColor) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(CategoryColor.RED) }
    val label = CategoryRef.builtIn(selected)?.let { categoryLabel(it) } ?: custom.firstOrNull { it.id == selected }?.name ?: selected
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.category_format, label)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WatchCategory.entries.forEach { category -> DropdownMenuItem(text = { Text(categoryLabel(category)) }, onClick = { onSelect(category.name); expanded = false }) }
            custom.forEach { category -> DropdownMenuItem(text = { Text(category.name) }, onClick = { onSelect(category.id); expanded = false }) }
            DropdownMenuItem(text = { Text(stringResource(R.string.add_category)) }, leadingIcon = { Icon(Icons.Default.Add, null) }, onClick = { expanded = false; adding = true })
        }
    }
    if (adding) AlertDialog(onDismissRequest = { adding = false }, title = { Text(stringResource(R.string.add_category)) }, text = {
        Column { OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.category_name)) }, singleLine = true); CategoryColor.entries.forEach { option -> TextButton(onClick = { color = option }) { Text(if (color == option) "✓ ${option.name.lowercase()}" else option.name.lowercase()) } } }
    }, confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { onAdd(name, color); adding = false; name = "" } }) { Text(stringResource(R.string.add)) } }, dismissButton = { TextButton(onClick = { adding = false }) { Text(stringResource(R.string.cancel)) } })
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TagEditor(label: String, tags: List<String>, suggestions: List<String>, onAdd: (String) -> Unit, onRemove: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    fun submit() {
        val tag = input.trim()
        if (tag.isNotEmpty() && tags.none { it.equals(tag, ignoreCase = true) }) onAdd(tag)
        input = ""
    }
    val matchingSuggestions = suggestions.filter { suggestion ->
        suggestion.contains(input.trim(), ignoreCase = true) && tags.none { it.equals(suggestion, ignoreCase = true) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(label) },
            placeholder = { Text(stringResource(R.string.tag_input_hint)) },
            trailingIcon = { IconButton(onClick = ::submit) { Icon(Icons.Default.Add, stringResource(R.string.add_tag)) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit(); keyboard?.hide() }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (matchingSuggestions.isNotEmpty()) FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            matchingSuggestions.forEach { suggestion ->
                InputChip(
                    selected = false,
                    onClick = { onAdd(suggestion); input = "" },
                    label = { Text(suggestion) },
                )
            }
        }
        if (tags.isNotEmpty()) FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tags.forEach { tag ->
                InputChip(
                    selected = true,
                    onClick = { },
                    label = { Text(tag) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.remove_tag, tag),
                            modifier = Modifier.size(18.dp).clickable { onRemove(tag) },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun <T> EnumMenu(label: String, selected: T, options: List<T>, title: @Composable (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${title(selected)}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(title(option)) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
