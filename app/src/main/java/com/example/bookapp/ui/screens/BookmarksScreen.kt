package com.example.bookapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.SearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    items: List<SearchResult>,
    onItemClick: (SearchResult) -> Unit,
    onBack: () -> Unit,
    onRemove: (SearchResult) -> Unit = {},
    canDelete: Boolean = true
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("علاقه‌مندی‌ها") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("هنوز چیزی نشان نکرده‌اید")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(items) { r ->
                    ListItem(
                        headlineContent = { Text(r.sectionTitle) },
                        supportingContent = { Text("${r.fieldTitle} ← ${r.taziehTitle} ← ${r.roleTitle}") },
                        trailingContent = { if (canDelete) IconButton(onClick = { onRemove(r) }) { Icon(Icons.Filled.Delete, "حذف از علاقه‌مندی‌ها") } },
                        modifier = Modifier.clickable { onItemClick(r) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
