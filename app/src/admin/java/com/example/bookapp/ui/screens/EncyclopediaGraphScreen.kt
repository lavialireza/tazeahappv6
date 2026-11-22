package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.AppDatabase
import com.example.bookapp.data.EncyclopediaGraphBuilder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncyclopediaGraphScreen(db: AppDatabase, onBack: () -> Unit) {
    var graph by remember { mutableStateOf<com.example.bookapp.data.KnowledgeGraph?>(null) }
    LaunchedEffect(Unit) { graph = EncyclopediaGraphBuilder.build(db.encyclopediaDao().allVerified(), db.encyclopediaDao().allLinks()) }
    Scaffold(topBar = { TopAppBar(title = { Text("نقشه ارتباطات دانشنامه") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { padding ->
        val value = graph
        if (value == null) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("${value.nodes.size} گره • ${value.edges.size} پیوند", style = MaterialTheme.typography.titleMedium) }
            item { Text("در این نسخه، پیوندها بین مدخل‌های تأییدشده و زمینه‌های آرشیوی (تعزیه/نسخه/نقش/بخش) نمایش داده می‌شوند.", style = MaterialTheme.typography.bodySmall) }
            items(value.edges) { edge ->
                val label = value.nodes.firstOrNull { it.id == edge.fromId }?.label ?: edge.fromId
                ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text(label, style = MaterialTheme.typography.titleSmall)
                    Text("${edge.relation} → ${edge.contextUid}", style = MaterialTheme.typography.bodyMedium)
                } }
            }
        }
    }
}
