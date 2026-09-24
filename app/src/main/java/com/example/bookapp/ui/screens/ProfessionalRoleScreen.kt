package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ProfessionalRoleItem(
    val id: Long,
    val title: String,
    val sectionCount: Int,
    val firstVerse: String,
    val isMine: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalRoleScreen(
    taziehTitle: String,
    items: List<ProfessionalRoleItem>,
    onOpen: (ProfessionalRoleItem) -> Unit,
    onSetMine: (ProfessionalRoleItem) -> Unit,
    onCompare: (ProfessionalRoleItem) -> Unit,
    onOpenGallery: () -> Unit = {},
    showGallery: Boolean = true,
    onBack: () -> Unit,
    readOnly: Boolean = false,
    showCompare: Boolean = true,
    canSelectMine: Boolean = true
) {
    var query by remember { mutableStateOf("") }
    val filtered = items.filter { normalizePersianSearch(it.title).contains(normalizePersianSearch(query)) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("نقش‌ها: $taziehTitle") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } },
            actions = {
                if (showGallery) {
                    IconButton(onClick = onOpenGallery) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "گالری تصاویر این تعزیه")
                    }
                }
            }
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(12.dp), singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("جستجوی نقش") })
            Text("نقش من با علامت ✓ مشخص است. با انتخاب آن، دسترسی سریع به مطالعه و تمرین خواهید داشت.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            if (showGallery) {
                Card(
                    onClick = onOpenGallery,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("گالری تصاویر این تعزیه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(if (readOnly) "مشاهده تصاویر اختصاصی این تعزیه" else "افزودن و مدیریت تصاویر اختصاصی این تعزیه", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("ورود", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { role ->
                    Card(onClick = { onOpen(role) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(role.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (role.isMine) Icon(Icons.Filled.CheckCircle, "نقش من", tint = MaterialTheme.colorScheme.primary)
                            }
                            Text("${role.sectionCount} بخش", style = MaterialTheme.typography.bodySmall)
                            if (role.firstVerse.isNotBlank()) Text(role.firstVerse, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (canSelectMine) OutlinedButton(onClick = { onSetMine(role) }) { Icon(Icons.Filled.School, null); Spacer(Modifier.width(4.dp)); Text(if (role.isMine) "نقش من" else "انتخاب نقش من") }
                                if (showCompare) OutlinedButton(onClick = { onCompare(role) }) { Text("مقایسه") }
                            }
                        }
                    }
                }
            }
        }
    }
}
