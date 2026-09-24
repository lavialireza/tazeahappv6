package com.example.bookapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.AdvancedSearchOptions
import com.example.bookapp.data.AdvancedSearchResult
import com.example.bookapp.data.DialogueSearchResult
import com.example.bookapp.data.FieldEntity
import com.example.bookapp.data.RoleEntity
import com.example.bookapp.data.SearchMatchMode
import com.example.bookapp.data.SearchSortMode
import com.example.bookapp.data.SectionEntity
import com.example.bookapp.data.TaziehEntity
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    fields: List<FieldEntity>,
    allTaziehs: List<TaziehEntity>,
    allRoles: List<RoleEntity>,
    allSections: List<SectionEntity>,
    onSearch: suspend (query: String, options: AdvancedSearchOptions) -> List<AdvancedSearchResult>,
    onSearchDialogues: suspend (query: String) -> List<DialogueSearchResult> = { emptyList() },
    onResultClick: (AdvancedSearchResult) -> Unit,
    onDialogueResultClick: (DialogueSearchResult) -> Unit = {},
    isBookmarked: (Long) -> Boolean = { false },
    onToggleBookmark: (Long) -> Unit = {},
    showBookmarks: Boolean = true,
    advancedEnabled: Boolean = true,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<AdvancedSearchResult>()) }
    var dialogueResults by remember { mutableStateOf(emptyList<DialogueSearchResult>()) }
    var searched by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var selectedFieldId by remember { mutableStateOf<Long?>(null) }
    var selectedTaziehId by remember { mutableStateOf<Long?>(null) }
    var selectedRoleId by remember { mutableStateOf<Long?>(null) }
    var selectedSectionId by remember { mutableStateOf<Long?>(null) }
    var matchMode by remember { mutableStateOf(SearchMatchMode.ALL_WORDS) }
    var sortMode by remember { mutableStateOf(SearchSortMode.RELEVANCE) }
    var showAdvanced by remember { mutableStateOf(false) }
    var inSectionTitle by remember { mutableStateOf(true) }
    var inText by remember { mutableStateOf(true) }
    var inRole by remember { mutableStateOf(true) }
    var inTazieh by remember { mutableStateOf(true) }
    var inField by remember { mutableStateOf(true) }
    var inFootnote by remember { mutableStateOf(true) }

    val taziehsForField = remember(selectedFieldId, allTaziehs) {
        if (selectedFieldId == null) emptyList() else allTaziehs.filter { it.fieldId == selectedFieldId }
    }
    val rolesForTazieh = remember(selectedTaziehId, allRoles) {
        if (selectedTaziehId == null) emptyList() else allRoles.filter { it.taziehId == selectedTaziehId }
    }
    val sectionsForRole = remember(selectedRoleId, allSections) {
        if (selectedRoleId == null) emptyList() else allSections.filter { it.roleId == selectedRoleId }
    }

    val options = AdvancedSearchOptions(
        matchMode = matchMode,
        fieldId = selectedFieldId,
        taziehId = selectedTaziehId,
        roleId = selectedRoleId,
        sectionId = selectedSectionId,
        inSectionTitle = inSectionTitle,
        inText = inText,
        inRole = inRole,
        inTazieh = inTazieh,
        inField = inField,
        inFootnote = inFootnote,
        sortMode = sortMode
    )

    LaunchedEffect(query, options) {
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            dialogueResults = emptyList()
            searched = false
            searching = false
            return@LaunchedEffect
        }
        delay(300)
        searching = true
        results = onSearch(q, options)
        dialogueResults = if (selectedFieldId == null && selectedTaziehId == null && selectedRoleId == null && selectedSectionId == null) onSearchDialogues(q) else emptyList()
        searched = true
        searching = false
    }

    fun clearBelowField() { selectedTaziehId = null; selectedRoleId = null; selectedSectionId = null }
    fun clearBelowTazieh() { selectedRoleId = null; selectedSectionId = null }
    fun clearBelowRole() { selectedSectionId = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جستجوی پیشرفته") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("جستجو در", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            Text("زمینه", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp, top = 2.dp, bottom = 4.dp))
            LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selectedFieldId == null, { selectedFieldId = null; clearBelowField() }, label = { Text("همه زمینه‌ها") }) }
                items(fields) { field -> FilterChip(selectedFieldId == field.id, { selectedFieldId = field.id; clearBelowField() }, label = { Text(field.title) }) }
            }

            if (taziehsForField.isNotEmpty()) {
                Text("تعزیه", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selectedTaziehId == null, { selectedTaziehId = null; clearBelowTazieh() }, label = { Text("همه تعزیه‌ها") }) }
                    items(taziehsForField) { tazieh -> FilterChip(selectedTaziehId == tazieh.id, { selectedTaziehId = tazieh.id; clearBelowTazieh() }, label = { Text(tazieh.title) }) }
                }
            }

            if (rolesForTazieh.isNotEmpty()) {
                Text("نقش", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selectedRoleId == null, { selectedRoleId = null; clearBelowRole() }, label = { Text("همه نقش‌ها") }) }
                    items(rolesForTazieh) { role -> FilterChip(selectedRoleId == role.id, { selectedRoleId = role.id; clearBelowRole() }, label = { Text(role.title) }) }
                }
            }

            if (sectionsForRole.isNotEmpty()) {
                Text("بخش", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp))
                LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selectedSectionId == null, { selectedSectionId = null }, label = { Text("همه بخش‌ها") }) }
                    items(sectionsForRole) { section -> FilterChip(selectedSectionId == section.id, { selectedSectionId = section.id }, label = { Text(section.title) }) }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("عبارت جستجو") },
                placeholder = { Text("مثلاً: علی اکبر یا یا حسین") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                trailingIcon = { if (searching) CircularProgressIndicator(Modifier.size(20.dp)) }
            )

            if (advancedEnabled) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showAdvanced = true }) { Text("تنظیمات جستجوی پیشرفته ⚙") }
            }

            Spacer(Modifier.height(4.dp))
            if (searched && results.isEmpty() && dialogueResults.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("نتیجه‌ای یافت نشد") }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (dialogueResults.isNotEmpty()) {
                        item { Text("گفتگوها", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                        items(dialogueResults) { d ->
                            ListItem(headlineContent = { Text(d.dialogueTitle) }, supportingContent = { Text(d.taziehTitle) }, modifier = Modifier.clickable { onDialogueResultClick(d) })
                            HorizontalDivider()
                        }
                    }
                    if (results.isNotEmpty()) item { Text("بخش‌ها (${results.size})", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    items(results) { r ->
                        ListItem(
                            headlineContent = { Text(r.sectionTitle) },
                            supportingContent = {
                                Column {
                                    Text("${r.fieldTitle} ← ${r.taziehTitle} ← ${r.roleTitle}", style = MaterialTheme.typography.bodySmall)
                                    if (r.snippet.isNotBlank()) Text(r.snippet, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                    Text("محل تطبیق: ${r.matchSource}", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            trailingContent = if (showBookmarks) ({ IconButton(onClick = { onToggleBookmark(r.sectionId) }) { Icon(if (isBookmarked(r.sectionId)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = "نشان کردن") } }) else null,
                            modifier = Modifier.clickable { onResultClick(r) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showAdvanced) {
        AlertDialog(
            onDismissRequest = { showAdvanced = false },
            title = { Text("تنظیمات جستجوی پیشرفته") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text("جستجو در:", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(inSectionTitle, { inSectionTitle = it }); Text("عنوان بخش") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(inText, { inText = it }); Text("متن شعر") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(inRole, { inRole = it }); Text("نام نقش") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(inTazieh, { inTazieh = it }); Text("نام تعزیه") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(inField, { inField = it }); Text("نام زمینه") }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(inFootnote, { inFootnote = it }); Text("واژه و پاورقی") }
                    Spacer(Modifier.height(8.dp))
                    Text("نوع تطبیق", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(matchMode == SearchMatchMode.ALL_WORDS, { matchMode = SearchMatchMode.ALL_WORDS }, label = { Text("همه کلمات") }) }
                        item { FilterChip(matchMode == SearchMatchMode.EXACT_PHRASE, { matchMode = SearchMatchMode.EXACT_PHRASE }, label = { Text("عبارت دقیق") }) }
                        item { FilterChip(matchMode == SearchMatchMode.ANY_WORD, { matchMode = SearchMatchMode.ANY_WORD }, label = { Text("هر کلمه") }) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("مرتب‌سازی", style = MaterialTheme.typography.titleSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(sortMode == SearchSortMode.RELEVANCE, { sortMode = SearchSortMode.RELEVANCE }, label = { Text("مرتبط‌ترین") }) }
                        item { FilterChip(sortMode == SearchSortMode.HIERARCHY, { sortMode = SearchSortMode.HIERARCHY }, label = { Text("ساختار") }) }
                        item { FilterChip(sortMode == SearchSortMode.TITLE, { sortMode = SearchSortMode.TITLE }, label = { Text("عنوان") }) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAdvanced = false }) { Text("اعمال") } },
            dismissButton = { TextButton(onClick = { showAdvanced = false }) { Text("بستن") } }
        )
    }
}
