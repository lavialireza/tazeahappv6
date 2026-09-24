package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class MyRoleItem(
    val taziehId: Long,
    val taziehTitle: String,
    val roleId: Long,
    val roleTitle: String
)

/**
 * فهرست نقش‌هایی که کاربر به‌عنوان «نقش من» در تعزیه‌های مختلف مشخص کرده،
 * با دسترسی سریع به مطالعه، حالت تمرین (حفظ‌کردن) و خروجی چاپی مخصوص همان نقش.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRoleScreen(
    items: List<MyRoleItem>,
    onRead: (MyRoleItem) -> Unit,
    onRehearse: (MyRoleItem) -> Unit,
    onExportPdf: (MyRoleItem) -> Unit,
    onRemove: (MyRoleItem) -> Unit,
    onBack: () -> Unit,
    readOnly: Boolean = false,
    showPdf: Boolean = true,
    canRead: Boolean = true,
    canRehearse: Boolean = true,
    canRemove: Boolean = true
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("نقش من") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "هنوز نقشی مشخص نکرده‌اید.\nاز فهرست نقش‌های هر تعزیه، گزینه «انتخاب نقش من» را بزنید و نقش خودتان را مشخص کنید.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { item ->
                    MyRoleCard(
                        item = item,
                        onRead = { onRead(item) },
                        onRehearse = { onRehearse(item) },
                        onExportPdf = { onExportPdf(item) },
                        showPdf = showPdf,
                        canRead = canRead,
                        canRehearse = canRehearse,
                        canRemove = canRemove,
                        onRemove = { onRemove(item) },
                        readOnly = readOnly
                    )
                }
            }
        }
    }
}

@Composable
private fun MyRoleCard(
    item: MyRoleItem,
    onRead: () -> Unit,
    onRehearse: () -> Unit,
    onExportPdf: () -> Unit,
    showPdf: Boolean = true,
    canRead: Boolean = true,
    canRehearse: Boolean = true,
    canRemove: Boolean = true,
    onRemove: () -> Unit,
    readOnly: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.roleTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        item.taziehTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canRemove) IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Close, contentDescription = "حذف از نقش من")
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canRead) OutlinedButton(onClick = onRead, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("مطالعه")
                }
                if (canRehearse) OutlinedButton(onClick = onRehearse, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.School, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("تمرین")
                }
                if (showPdf) {
                    OutlinedButton(onClick = onExportPdf, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("PDF")
                    }
                }
            }
        }
    }
}
