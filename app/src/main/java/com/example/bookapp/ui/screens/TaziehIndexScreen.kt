package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TaziehIndexItem(
    val roleId: Long,
    val roleTitle: String,
    val firstVerse: String
)

/** عدد ابتدای عنوان نقش را استخراج می‌کند (فارسی یا لاتین)، مثلاً از "۱- امام" یا "2 زینب" */
private fun leadingNumber(title: String): Int? {
    val normalized = title.trim()
        .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
        .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
    val match = Regex("^\\s*(\\d+)").find(normalized) ?: return null
    return match.groupValues[1].toIntOrNull()
}

/**
 * ترتیب نمایش فهرست: اگر عنوان نقش با یک عدد شروع شده باشد (مثلاً «۱ امام»،
 * «۲ زینب»)، بر اساس همان عدد مرتب می‌شود؛ در غیر این صورت همان ترتیبی که
 * از متن اصلی/نمایش‌نامه آمده (ترتیب ورودی لیست) حفظ می‌شود.
 */
fun sortTaziehIndexItems(items: List<TaziehIndexItem>): List<TaziehIndexItem> {
    val indexed = items.withIndex().toList()
    return indexed.sortedWith(
        compareBy(
            { (_, item) -> leadingNumber(item.roleTitle) ?: Int.MAX_VALUE },
            { (originalIndex, _) -> originalIndex }
        )
    ).map { it.value }
}

/**
 * فهرست کل یک تعزیه: عنوان هر نقش به همراه بیت اول شعرش، برای اینکه کارگردان
 * یا کاربر بتواند سریع ببیند این تعزیه شامل چه نقش‌هایی است و با یک لمس
 * مستقیم به همان نقطه از متن برود. قابل ویرایش است: می‌توان نام نقش را
 * تغییر داد یا با دکمه‌های بالا/پایین ترتیبش را در فهرست جابه‌جا کرد.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaziehIndexScreen(
    taziehTitle: String,
    author: String? = null,
    authorEmail: String? = null,
    items: List<TaziehIndexItem>,
    onItemClick: (TaziehIndexItem) -> Unit,
    onExportPdf: () -> Unit,
    onOpenGallery: () -> Unit = {},
    showGallery: Boolean = true,
    onRename: (TaziehIndexItem, String) -> Unit,
    onMove: (index: Int, direction: Int) -> Unit,
    onBack: () -> Unit,
    readOnly: Boolean = false,
    showPdf: Boolean = true
) {
    val sorted = remember(items) { sortTaziehIndexItems(items) }
    var editingItem by remember { mutableStateOf<TaziehIndexItem?>(null) }
    var editingText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("فهرست: $taziehTitle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (showGallery) {
                        IconButton(onClick = onOpenGallery) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = "گالری تصاویر این تعزیه")
                        }
                    }
                    if (showPdf) {
                        IconButton(onClick = onExportPdf) {
                            Icon(Icons.Filled.Share, contentDescription = "خروجی PDF کل تعزیه")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showGallery) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenGallery,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("گالری تصاویر این مجلس", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (readOnly) "مشاهده تصاویر" else "افزودن و مدیریت تصاویر همین تعزیه",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text("ورود", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            if (!author.isNullOrBlank() || !authorEmail.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("مشخصات نویسنده/گردآورنده", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            if (!author.isNullOrBlank()) Text(author)
                            if (!authorEmail.isNullOrBlank()) Text(authorEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            items(sorted, key = { it.roleId }) { item ->
                val index = sorted.indexOf(item)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    onClick = { onItemClick(item) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.roleTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (item.firstVerse.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    item.firstVerse,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        if (!readOnly) {
                            IconButton(onClick = {
                                editingItem = item
                                editingText = item.roleTitle
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "ویرایش نام نقش")
                            }
                            Column {
                                IconButton(
                                    onClick = { onMove(index, -1) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "جابه‌جایی به بالا")
                                }
                                IconButton(
                                    onClick = { onMove(index, 1) },
                                    enabled = index < sorted.size - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "جابه‌جایی به پایین")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val current = editingItem
    if (current != null) {
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("ویرایش نام نقش") },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editingText.isNotBlank()) onRename(current, editingText.trim())
                    editingItem = null
                }) { Text("ذخیره") }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text("انصراف") }
            }
        )
    }
}
