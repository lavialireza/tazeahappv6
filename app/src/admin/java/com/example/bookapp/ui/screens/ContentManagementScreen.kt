package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Professional content-management dashboard. It does not modify user notes/bookmarks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentManagementScreen(
    fieldsCount: Int,
    taziehsCount: Int,
    rolesCount: Int,
    sectionsCount: Int,
    imagesCount: Int,
    dialoguesCount: Int,
    healthWarnings: List<String>,
    processedFilesCount: Int,
    onRefresh: () -> Unit,
    onSyncLocal: () -> Unit,
    onSyncRemote: () -> Unit,
    onOpenEditor: () -> Unit,
    onImportJson: () -> Unit,
    onExportJson: () -> Unit,
    onImportWord: () -> Unit,
    onDetailedHealthCheck: () -> Unit,
    onExportHealthReport: () -> Unit,
    busy: Boolean,
    message: String?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مدیریت محتوا") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !busy) {
                        Icon(Icons.Filled.Refresh, contentDescription = "بازخوانی")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("مرکز مدیریت مجموعه", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("محتوای تعزیه‌ها را به‌صورت منظم بررسی و به‌روزرسانی کنید؛ اطلاعات شخصی شما در این بخش تغییر نمی‌کند.")
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("زمینه", fieldsCount.toString(), Modifier.weight(1f))
                    StatCard("تعزیه", taziehsCount.toString(), Modifier.weight(1f))
                    StatCard("نقش", rolesCount.toString(), Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("بخش", sectionsCount.toString(), Modifier.weight(1f))
                    StatCard("تصویر", imagesCount.toString(), Modifier.weight(1f))
                    StatCard("گفتگو", dialoguesCount.toString(), Modifier.weight(1f))
                }
            }

            item {
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Storage, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("فایل‌های محتوایی", fontWeight = FontWeight.Bold)
                                Text("$processedFilesCount فایل تاکنون پردازش شده است", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onSyncLocal, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("بررسی و به‌روزرسانی محتوای داخلی")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onSyncRemote, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.CloudSync, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("همگام‌سازی محتوای آنلاین")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onOpenEditor, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Storage, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("ویرایش و مدیریت ساختار محتوا")
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onImportJson, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text("ورود JSON سازگار با برنامه جانبی")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onImportWord, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text("ورود از Word (.docx)")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onExportJson, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text("خروجی JSON سازگار با برنامه جانبی")
                        }
                    }
                }
            }

            item {
                OutlinedCard {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FactCheck, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text("سلامت محتوا", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onDetailedHealthCheck, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.FactCheck, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("بررسی عمیق سلامت محتوا")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onExportHealthReport, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Storage, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("ارسال گزارش سلامت محتوا")
                        }
                        Spacer(Modifier.height(10.dp))
                        if (healthWarnings.isEmpty()) {
                            Text("✓ ساختار فعلی محتوا سالم است.", color = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("${healthWarnings.size} مورد نیازمند بررسی است", color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(6.dp))
                            healthWarnings.take(8).forEach { warning ->
                                Text("• $warning", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (message != null) {
                item {
                    Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
            if (busy) {
                item {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
