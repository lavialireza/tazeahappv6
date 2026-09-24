package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TaziehCatalogItem(
    val id: Long,
    val fieldId: Long,
    val fieldTitle: String,
    val title: String,
    val author: String?,
    val roleCount: Int,
    val hasAudio: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaziehCatalogScreen(
    items: List<TaziehCatalogItem>,
    onOpen: (TaziehCatalogItem) -> Unit,
    onOpenGallery: (TaziehCatalogItem) -> Unit = {},
    showGallery: Boolean = true,
    onBack: () -> Unit,
    initialFieldId: Long? = null
) {
    var query by remember { mutableStateOf("") }
    var selectedField by remember { mutableStateOf<Long?>(initialFieldId) }
    var audioOnly by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }

    val fields = remember(items) { items.distinctBy { it.fieldId }.sortedBy { it.fieldTitle } }
    val filtered = remember(items, query, selectedField, audioOnly, sortMode) {
        val q = normalizePersianSearch(query)
        val base = items.filter { item ->
            (selectedField == null || item.fieldId == selectedField) &&
                (!audioOnly || item.hasAudio) &&
                (q.isBlank() || normalizePersianSearch(item.title).contains(q) ||
                    normalizePersianSearch(item.author.orEmpty()).contains(q) ||
                    normalizePersianSearch(item.fieldTitle).contains(q))
        }
        when (sortMode) {
            1 -> base.sortedBy { normalizePersianSearch(it.title) }
            2 -> base.sortedByDescending { it.roleCount }
            else -> base.sortedWith(compareBy<TaziehCatalogItem> { it.fieldTitle }.thenBy { it.title })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedField == null) "همه تعزیه‌ها (${filtered.size})" else "تعزیه‌های این زمینه (${filtered.size})") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Filled.FilterList, "فیلتر") }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("همه تعزیه‌ها") }, onClick = { selectedField = null; audioOnly = false; menuExpanded = false })
                        DropdownMenuItem(text = { Text(if (audioOnly) "نمایش همه صوت‌ها" else "فقط تعزیه‌های دارای صوت") }, onClick = { audioOnly = !audioOnly; menuExpanded = false })
                        DropdownMenuItem(text = { Text("مرتب‌سازی عنوان") }, onClick = { sortMode = 1; menuExpanded = false })
                        DropdownMenuItem(text = { Text("بیشترین نقش") }, onClick = { sortMode = 2; menuExpanded = false })
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("جستجوی نام تعزیه، نویسنده یا زمینه") }
            )
            if (fields.isNotEmpty()) {
                Text("دسته‌بندی / زمینه", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selectedField == null, { selectedField = null }, label = { Text("همه") }) }
                    items(fields) { f -> FilterChip(selectedField == f.fieldId, { selectedField = f.fieldId }, label = { Text(f.fieldTitle) }) }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("تعزیه‌ای با این فیلترها پیدا نشد") }
            } else {
                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { item ->
                        Card(onClick = { onOpen(item) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(item.fieldTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (showGallery) {
                                        IconButton(onClick = { onOpenGallery(item) }) {
                                            Icon(Icons.Filled.PhotoLibrary, contentDescription = "گالری تصاویر ${item.title}")
                                        }
                                    }
                                }
                                if (!item.author.isNullOrBlank()) Text("نویسنده: ${item.author}", style = MaterialTheme.typography.bodySmall)
                                Text("${item.roleCount} نقش${if (item.hasAudio) "  •  صوت دارد" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (showGallery) {
                                    Spacer(Modifier.height(6.dp))
                                    TextButton(onClick = { onOpenGallery(item) }) {
                                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("گالری تصاویر همین تعزیه")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
